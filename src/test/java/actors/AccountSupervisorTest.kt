package actors

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import commands.Operation
import java.util.UUID.randomUUID
import kotlin.collections.LinkedHashMap
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
    fun `supervisor should forward debit`() {
        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf())
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        val debit = Operation.Debit(100, randomUUID().toString(), account1)
        supervisor.tell(debit, probe.ref)

        val expected = DebitResponse(debit.amount, StatusResponse.SUCCESS, debit.requestId, debit.accountId)
        val response = probe.expectMsgClass(DebitResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward credit`() {
        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf())
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        val credit = Operation.Credit(100, randomUUID().toString(), account1)
        supervisor.tell(credit, probe.ref)

        val expected = CreditResponse(credit.amount, StatusResponse.SUCCESS, credit.requestId, credit.accountId)
        val response = probe.expectMsgClass(CreditResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward transfer`() {
        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf())
                put(account2, arrayListOf())
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        val transfer = Operation.Transfer(100, account2, randomUUID().toString(), account1)
        supervisor.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.SUCCESS, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward error transfer`() {
        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf())
                put(account2, arrayListOf())
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        val transfer = Operation.Transfer(100000, account2, randomUUID().toString(), account1)
        supervisor.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.ERROR, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward transfer even when credit already executed`() {
        val transfer = Operation.Transfer(100, account2, randomUUID().toString(), account1)
        val credit = Operation.Credit(100, account2, transfer.receiverId)

        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf())
                put(account2, arrayListOf(credit))
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        supervisor.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.SUCCESS, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    @Test
    fun `supervisor should forward transfer even when debit already executed`() {
        val transfer = Operation.Transfer(100, account2, randomUUID().toString(), account1)
        val debit = Operation.Debit(100, account1, transfer.receiverId)

        val events = object : LinkedHashMap<String, ArrayList<Operation>>() {
            init {
                put(account1, arrayListOf(debit))
                put(account2, arrayListOf())
            }
        }

        val supervisor = AccountSupervisorBuilder()
                .withEvents(events)
                .build()

        supervisor.tell(transfer, probe.ref)

        val expected = TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.SUCCESS, transfer.requestId, transfer.accountId)
        val response = probe.expectMsgClass(TransferResponse::class.java)

        assertEquals(expected, response)
    }

    private inner class AccountSupervisorBuilder {
        private val supervisor = "account-supervisor"

        fun withEvents(events: LinkedHashMap<String, ArrayList<Operation>>): AccountSupervisorBuilder {
            given(eventStore.commands).willReturn(events)
            return this
        }

        fun build(): ActorRef = system.actorOf(AccountSupervisor.props(eventStore), supervisor)
    }
}
