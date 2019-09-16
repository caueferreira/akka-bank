package actors

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.Operation
import java.util.UUID.randomUUID
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import responses.BalanceResponse
import responses.CreditResponse
import responses.DebitResponse
import responses.StatusResponse
import source.EventStore

class AccountTest {

    @Mock
    private lateinit var eventStore: EventStore

    private lateinit var system: ActorSystem
    private lateinit var probe: TestKit

    private var accountId = "account1"

    @Before
    fun `before each`() {
        MockitoAnnotations.initMocks(this)

        system = ActorSystem.create()
        probe = TestKit(system)
    }

    @Test
    fun `should compose balance equals zero`() {
        val account = AccountTestBuilder()
                .build(accountId)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expected = BalanceResponse(0, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expected, response)
        kotlin.test.assertEquals(expected.balance, response.balance)
    }

    @Test
    fun `should compose balance of credits`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Credit(1000, randomUUID().toString(), accountId),
                        Operation.Credit(332, randomUUID().toString(), accountId),
                        Operation.Credit(9442, randomUUID().toString(), accountId)))
                .build(accountId)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expected = BalanceResponse(10774, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should compose balance of debits`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Debit(10, randomUUID().toString(), accountId),
                        Operation.Debit(32, randomUUID().toString(), accountId)))
                .build(accountId)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expected = BalanceResponse(-42, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should compose balance of credits and debits`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Credit(1000, randomUUID().toString(), accountId),
                        Operation.Debit(10, randomUUID().toString(), accountId),
                        Operation.Credit(2000, randomUUID().toString(), accountId),
                        Operation.Debit(32, randomUUID().toString(), accountId),
                        Operation.Debit(992, randomUUID().toString(), accountId),
                        Operation.Debit(221, randomUUID().toString(), accountId),
                        Operation.Credit(781, randomUUID().toString(), accountId)))
                .build(accountId)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expected = BalanceResponse(2526, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val response = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should raise exception`() {
        val account = AccountTestBuilder()
                .build(accountId)

        val debit = Operation.Debit(100000, randomUUID().toString(), accountId)
        account.tell(debit, probe.ref)

        val expected = DebitResponse(debit.amount, StatusResponse.ERROR, debit.requestId, debit.accountId)
        val response = probe.expectMsgClass(DebitResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should increase balance when credit event is created`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Credit(500, randomUUID().toString(), accountId)))
                .build(accountId)

        val credit = Operation.Credit(1000, randomUUID().toString(), accountId)
        account.tell(credit, probe.ref)

        val expectedCreditResponse = CreditResponse(credit.amount, StatusResponse.SUCCESS, credit.requestId, credit.accountId)
        val creditResponse = probe.expectMsgClass(CreditResponse::class.java)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expectedReadResponse = BalanceResponse(1500, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val readResponse = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expectedCreditResponse, creditResponse)
        assertEquals(expectedReadResponse, readResponse)
    }

    @Test
    fun `should reduce balance when debit event is created`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Credit(500, randomUUID().toString(), accountId)))
                .build(accountId)

        val debit = Operation.Debit(1000, randomUUID().toString(), accountId)
        account.tell(debit, probe.ref)

        val expectedDebitResponse = DebitResponse(debit.amount, StatusResponse.SUCCESS, debit.requestId, debit.accountId)
        val debitResponse = probe.expectMsgClass(DebitResponse::class.java)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expectedReadResponse = BalanceResponse(-500, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val readResponse = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expectedDebitResponse, debitResponse)
        assertEquals(expectedReadResponse, readResponse)
    }

    @Test
    fun `should increase negative balance when credit event is created`() {
        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(
                        Operation.Debit(500, randomUUID().toString(), accountId)))
                .build(accountId)

        val credit = Operation.Credit(1000, randomUUID().toString(), accountId)
        account.tell(credit, probe.ref)

        val expectedCreditResponse = CreditResponse(credit.amount, StatusResponse.SUCCESS, credit.requestId, credit.accountId)
        val creditResponse = probe.expectMsgClass(CreditResponse::class.java)

        val read = Operation.Read(randomUUID().toString(), accountId)
        account.tell(read, probe.ref)

        val expectedReadResponse = BalanceResponse(500, StatusResponse.SUCCESS, read.requestId, read.accountId)
        val readResponse = probe.expectMsgClass(BalanceResponse::class.java)

        assertEquals(expectedReadResponse, readResponse)
        assertEquals(expectedCreditResponse, creditResponse)
    }

    @Test
    fun `should throw credit already executed`() {
        val credit = Operation.Credit(1000, randomUUID().toString(), accountId)

        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(credit))
                .build(accountId)

        account.tell(credit, probe.ref)

        val expected = CreditResponse(credit.amount, StatusResponse.ALREADY_EXECUTED, credit.requestId, credit.accountId)
        val response = probe.expectMsgClass(CreditResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should throw debit already executed`() {
        val debit = Operation.Debit(1000, randomUUID().toString(), accountId)

        val account = AccountTestBuilder()
                .withEvents(accountId, arrayListOf(debit))
                .build(accountId)

        account.tell(debit, probe.ref)

        val expected = DebitResponse(debit.amount, StatusResponse.ALREADY_EXECUTED, debit.requestId, debit.accountId)
        val response = probe.expectMsgClass(DebitResponse::class.java)

        assertEquals(expected, response)
    }

    private inner class AccountTestBuilder {

        fun withEvents(accountId: String, commands: ArrayList<Operation>): AccountTestBuilder {
            given(eventStore.commands(accountId)).willReturn(commands)
            return this
        }

        fun build(accountId: String): ActorRef = system.actorOf(Account.props(accountId, eventStore), accountId)
    }
}
