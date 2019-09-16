package actors

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.Operation
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import responses.StatusResponse
import responses.TransferResponse
import source.EventStore
import java.util.UUID.randomUUID
import kotlin.collections.ArrayList
import kotlin.test.assertEquals

class TransferSagaTest {

    @Mock
    private lateinit var eventStore: EventStore

    private lateinit var system: ActorSystem
    private lateinit var probe: TestKit

    private var account1 = "account1"
    private var account2 = "account2"

    @Before
    fun `before each`() {
        MockitoAnnotations.initMocks(this)

        system = ActorSystem.create()
        probe = TestKit(system)
    }

    @Test
    fun `should complete transfer between two accounts`() {
        val transferSaga = TransferSagaBuilder()
                .withEvents(account1, arrayListOf())
                .withEvents(account2, arrayListOf())
                .build(account1, account2)

        val transfer = Operation.Transfer(100, account2, randomUUID().toString(), account1)
        transferSaga.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.SUCCESS, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `should fail transfer between two accounts`() {
        val transferSaga = TransferSagaBuilder()
                .withEvents(account1, arrayListOf())
                .withEvents(account2, arrayListOf())
                .build(account1, account2)

        val transfer = Operation.Transfer(10000, account2, randomUUID().toString(), account1)
        transferSaga.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.ERROR, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    private inner class TransferSagaBuilder {
        private val saga = "transfer-saga"

        fun withEvents(account: String, events: ArrayList<Operation>): TransferSagaBuilder {
            given(eventStore.commands(account)).willReturn(events)
            return this
        }

        fun build(requester: String, receiver: String): ActorRef {
            val from = system.actorOf(Account.props(requester, eventStore), requester)
            val to = system.actorOf(Account.props(receiver, eventStore), receiver)

            return system.actorOf(TransferSaga.props(from, to), saga)
        }
    }
}
