package actors

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.Operation
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import responses.CreditResponse
import responses.DebitResponse
import responses.StatusResponse
import responses.TransferResponse
import source.EventStore

class AccountSupervisorTest {
    @Mock
    private lateinit var eventStore: EventStore
    private lateinit var system: ActorSystem

    @Before
    fun `before each`() {
        MockitoAnnotations.initMocks(this)

        system = ActorSystem.create()
    }

    @Test
    fun `supervisor should forward debit`() {
        val events = linkedMapOf<String, ArrayList<Operation>>()
        events["account1"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val debit = Operation.Debit(100, "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(debit, probe.ref)

        val response = probe.expectMsgClass(DebitResponse::class.java)
        val expected = DebitResponse(100, StatusResponse.SUCCESS, "REQ", "account1")
        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward credit`() {
        val events = linkedMapOf<String, ArrayList<Operation>>()
        events["account1"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val credit = Operation.Credit(100, "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(credit, probe.ref)

        val response = probe.expectMsgClass(CreditResponse::class.java)
        val expected = CreditResponse(100, StatusResponse.SUCCESS, "REQ", "account1")
        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward transfer`() {
        val events = linkedMapOf<String, ArrayList<Operation>>()
        events["account1"] = arrayListOf()
        events["account2"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val transfer = Operation.Transfer(100, "account2", "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(transfer, probe.ref)

        val response = probe.expectMsgClass(TransferResponse::class.java)
        val expected = TransferResponse(100, "account2", StatusResponse.SUCCESS, "REQ", "account1")
        assertEquals(expected, response)
    }
}
