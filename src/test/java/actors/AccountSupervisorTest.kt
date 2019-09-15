package actors

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.AccountCommand
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
        val events = linkedMapOf<String, ArrayList<AccountCommand>>()
        events["account1"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val debit = AccountCommand.Debit(100, "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(debit, probe.ref)

        val response = probe.expectMsgClass(DebitResponse::class.java)
        val expected = DebitResponse("REQ", 100, "account1", StatusResponse.SUCCESS)
        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward credit`() {
        val events = linkedMapOf<String, ArrayList<AccountCommand>>()
        events["account1"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val credit = AccountCommand.Credit(100, "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(credit, probe.ref)

        val response = probe.expectMsgClass(CreditResponse::class.java)
        val expected = CreditResponse("REQ", 100, "account1", StatusResponse.SUCCESS)
        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward transfer`() {
        val events = linkedMapOf<String, ArrayList<AccountCommand>>()
        events["account1"] = arrayListOf()
        events["account2"] = arrayListOf()
        given(eventStore.commands).willReturn(events)

        val transfer = AccountCommand.Transfer(100, "account2", "REQ", "account1")
        val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

        val probe = TestKit(system)

        supervisor.tell(transfer, probe.ref)

        val response = probe.expectMsgClass(TransferResponse::class.java)
        val expected = TransferResponse("REQ", 100, "account1", "account2", StatusResponse.SUCCESS)
        assertEquals(expected, response)
    }
}
