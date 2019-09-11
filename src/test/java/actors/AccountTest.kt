package actors

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.AccountCommand
import errors.AccountWithoutBalanceForDebit
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import responses.BalanceResponse
import responses.DebitResponse
import source.EventStore
import java.util.UUID.randomUUID

class AccountTest {

    @Mock
    private lateinit var eventStore: EventStore
    private lateinit var system: ActorSystem

    @Before
    fun `before each`() {
        MockitoAnnotations.initMocks(this)

        system = ActorSystem.create()
    }

    @Test
    fun `should compose balance equals zero`() {
        val accountId = "accountTest"
        val account = AccountTestBuilder()
                .build(accountId)

        val requestId = randomUUID().toString()
        val read = AccountCommand.Read(requestId, accountId)
        val probe = TestKit(system)

        account.tell(read, probe.ref)

        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(requestId, response.requestId)
        assertEquals(accountId, response.accountId)
        assertEquals(0, response.balance)
    }

    @Test
    fun `should compose balance of credits`() {
        val accountId = "accountTest"
        val commands = arrayListOf<AccountCommand>(
                AccountCommand.Credit(1000, randomUUID().toString(), accountId),
                AccountCommand.Credit(332, randomUUID().toString(), accountId),
                AccountCommand.Credit(9442, randomUUID().toString(), accountId))

        val account = AccountTestBuilder()
                .withEvents(accountId, commands)
                .build(accountId)

        val requestId = randomUUID().toString()
        val read = AccountCommand.Read(requestId, accountId)
        val probe = TestKit(system)

        account.tell(read, probe.ref)

        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(requestId, response.requestId)
        assertEquals(accountId, response.accountId)
        assertEquals(10774, response.balance)
    }

    @Test
    fun `should compose balance of debits`() {
        val accountId = "accountTest"
        val commands = arrayListOf<AccountCommand>(
                AccountCommand.Debit(10, randomUUID().toString(), accountId),
                AccountCommand.Debit(32, randomUUID().toString(), accountId))

        val account = AccountTestBuilder()
                .withEvents(accountId, commands)
                .build(accountId)

        val requestId = randomUUID().toString()
        val read = AccountCommand.Read(requestId, accountId)
        val probe = TestKit(system)

        account.tell(read, probe.ref)

        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(requestId, response.requestId)
        assertEquals(accountId, response.accountId)
        assertEquals(-42, response.balance)
    }

    @Test
    fun `should compose balance of credits and debits`() {
        val accountId = "accountTest"
        val commands = arrayListOf<AccountCommand>(
                AccountCommand.Credit(1000, randomUUID().toString(), accountId),
                AccountCommand.Debit(10, randomUUID().toString(), accountId),
                AccountCommand.Credit(2000, randomUUID().toString(), accountId),
                AccountCommand.Debit(32, randomUUID().toString(), accountId),
                AccountCommand.Debit(992, randomUUID().toString(), accountId),
                AccountCommand.Debit(221, randomUUID().toString(), accountId),
                AccountCommand.Credit(781, randomUUID().toString(), accountId))

        val account = AccountTestBuilder()
                .withEvents(accountId, commands)
                .build(accountId)

        val requestId = randomUUID().toString()
        val read = AccountCommand.Read(requestId, accountId)
        val probe = TestKit(system)

        account.tell(read, probe.ref)

        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(requestId, response.requestId)
        assertEquals(accountId, response.accountId)
        assertEquals(2526, response.balance)
    }

    @Test
    fun `should raise exception`() {
        val accountId = "accountTest"
        val account = AccountTestBuilder()
                .build(accountId)

        val requestId = randomUUID().toString()
        val debit = AccountCommand.Debit(100000, requestId, accountId)
        val probe = TestKit(system)

        account.tell(debit, probe.ref)

        probe.expectMsgClass(AccountWithoutBalanceForDebit::class.java)
    }

    private inner class AccountTestBuilder {

        fun withEvents(accountId: String, commands: ArrayList<AccountCommand>): AccountTestBuilder {
            given(eventStore.commands(accountId)).willReturn(commands)
            return this
        }

        fun build(accountId: String): ActorRef = system.actorOf(Account.props(accountId, eventStore), accountId)
    }
}