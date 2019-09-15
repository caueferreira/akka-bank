package actors

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.AccountCommand
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import responses.StatusResponse
import responses.TransferResponse
import source.EventStore

class TransferSagaTest {

    @Mock
    private lateinit var eventStore: EventStore
    private lateinit var system: ActorSystem

    @Before
    fun `before each`() {
        MockitoAnnotations.initMocks(this)

        system = ActorSystem.create()
    }

    @Test
    fun `should complete transfer between two accounts`() {
        given(eventStore.commands("account1")).willReturn(arrayListOf<AccountCommand>())
        given(eventStore.commands("account2")).willReturn(arrayListOf<AccountCommand>())

        val account1 = system.actorOf(Account.props("account1", eventStore), "account1")
        val account2 = system.actorOf(Account.props("account2", eventStore), "account2")

        val transfer = AccountCommand.Transfer(100, "account2", "REQ", "account1")
        val transferSaga = system.actorOf(TransferSaga.props(account1, account2), "transfer")

        val probe = TestKit(system)

        transferSaga.tell(transfer, probe.ref)

        val response = probe.expectMsgClass(TransferResponse::class.java)

        Assert.assertEquals("REQ", response.requestId)
        Assert.assertEquals("account1", response.accountId)
        Assert.assertEquals("account2", response.receiverId)
        Assert.assertEquals(StatusResponse.SUCCESS, response.status)
        Assert.assertEquals(100, response.amount)
    }

    @Test
    fun `should fail transfer between two accounts`() {
        given(eventStore.commands("account1")).willReturn(arrayListOf<AccountCommand>())
        given(eventStore.commands("account2")).willReturn(arrayListOf<AccountCommand>())

        val account1 = system.actorOf(Account.props("account1", eventStore), "account1")
        val account2 = system.actorOf(Account.props("account2", eventStore), "account2")

        val transfer = AccountCommand.Transfer(10000, "account2", "REQ", "account1")
        val transferSaga = system.actorOf(TransferSaga.props(account1, account2), "transfer")

        val probe = TestKit(system)

        transferSaga.tell(transfer, probe.ref)

        val response = probe.expectMsgClass(TransferResponse::class.java)

        Assert.assertEquals("REQ", response.requestId)
        Assert.assertEquals("account1", response.accountId)
        Assert.assertEquals("account2", response.receiverId)
        Assert.assertEquals(StatusResponse.ERROR, response.status)
        Assert.assertEquals(10000, response.amount)
    }
}
