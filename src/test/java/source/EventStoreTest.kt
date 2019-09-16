package source

import commands.Operation
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.Test

class EventStoreTest {

    private var account1 = "account1"
    private var account2 = "account2"
    private var account3 = "account3"

    @Test
    fun `should retrieve empty events`() {
        val eventStore = EventStoreBuilder().build()

        assertEquals(eventStore.commands, LinkedHashMap())
    }

    @Test
    fun `should retrieve empty events from account`() {
        val eventStore = EventStoreBuilder().build()

        assertEquals(eventStore.commands(account1), arrayListOf())
    }

    @Test
    fun `should return all events`() {
        val events = LinkedHashMap<String, ArrayList<Operation>>()
        events[account1] = arrayListOf(
                Operation.Credit(10000, randomUUID().toString(), account1),
                Operation.Debit(220, randomUUID().toString(), account1),
                Operation.Credit(4803, randomUUID().toString(), account1))
        events[account2] = arrayListOf(
                Operation.Credit(4000, randomUUID().toString(), account2),
                Operation.Credit(2110, randomUUID().toString(), account2),
                Operation.Debit(222, randomUUID().toString(), account2))
        events[account3] = arrayListOf(
                Operation.Credit(8730, randomUUID().toString(), account3),
                Operation.Debit(953, randomUUID().toString(), account3))

        val eventStore = EventStoreBuilder()
                .addEvents(account1, events[account1])
                .addEvents(account2, events[account2])
                .addEvents(account3, events[account3])
                .build()

        assertEquals(events, eventStore.commands)
    }

    @Test
    fun `should return all events from account`() {
        val events = LinkedHashMap<String, ArrayList<Operation>>()
        events[account1] = arrayListOf(
                Operation.Credit(10000, randomUUID().toString(), account1),
                Operation.Debit(220, randomUUID().toString(), account1),
                Operation.Credit(4803, randomUUID().toString(), account1))
        events[account2] = arrayListOf(
                Operation.Credit(4000, randomUUID().toString(), account2),
                Operation.Credit(2110, randomUUID().toString(), account2),
                Operation.Debit(222, randomUUID().toString(), account2))
        events[account3] = arrayListOf(
                Operation.Credit(8730, randomUUID().toString(), account3),
                Operation.Debit(953, randomUUID().toString(), account3))

        val eventStore = EventStoreBuilder()
                .addEvents(account1, events[account1])
                .addEvents(account2, events[account2])
                .addEvents(account3, events[account3])
                .build()

        assertEquals(events[account1], eventStore.commands(account1))
        assertEquals(events[account2], eventStore.commands(account2))
        assertEquals(events[account3], eventStore.commands(account3))
    }

    @Test
    fun `should return added events from account`() {
        val eventStore = EventStoreBuilder().build()

        val event = Operation.Credit(10000, randomUUID().toString(), account1)
        eventStore.add(event)

        assertEquals(arrayListOf<Operation>(event), eventStore.commands(account1))
    }

    private inner class EventStoreBuilder {
        private var commands = LinkedHashMap<String, ArrayList<Operation>>()

        fun addEvents(accountId: String, events: ArrayList<Operation>?): EventStoreBuilder {
            events?.let {
                commands[accountId] = events
            }
            return this
        }

        fun build() = EventStore(commands)
    }
}
