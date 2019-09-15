package source

import commands.Operation
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.Test

class EventStoreTest {

    private lateinit var eventStore: EventStore

    @Test
    fun `should retrieve empty events`() {
        eventStore = EventStore()
        val commands = eventStore.commands

        assertEquals(commands, LinkedHashMap())
    }

    @Test
    fun `should retrieve empty events from account`() {
        eventStore = EventStore()
        val commands = eventStore.commands("account1")

        assertEquals(commands, arrayListOf())
    }

    @Test
    fun `should return all events`() {
        val commands = LinkedHashMap<String, ArrayList<Operation>>()
        commands["etreardu"] = arrayListOf(
                Operation.Credit(10000, randomUUID().toString(), "etreardu"),
                Operation.Debit(220, randomUUID().toString(), "etreardu"),
                Operation.Credit(4803, randomUUID().toString(), "etreardu"))

        commands["sonore"] = arrayListOf(
                Operation.Credit(4000, randomUUID().toString(), "sonore"),
                Operation.Credit(2110, randomUUID().toString(), "sonore"),
                Operation.Debit(222, randomUUID().toString(), "sonore"))

        commands["alotow"] = arrayListOf(
                Operation.Credit(8730, randomUUID().toString(), "alotow"),
                Operation.Debit(953, randomUUID().toString(), "alotow"))
        eventStore = EventStore(commands)

        val events = eventStore.commands

        assertEquals(commands, events)
    }

    @Test
    fun `should return all events from account`() {
        val commands = LinkedHashMap<String, ArrayList<Operation>>()
        commands["etreardu"] = arrayListOf(
                Operation.Credit(10000, randomUUID().toString(), "etreardu"),
                Operation.Debit(220, randomUUID().toString(), "etreardu"),
                Operation.Credit(4803, randomUUID().toString(), "etreardu"))

        commands["sonore"] = arrayListOf(
                Operation.Credit(4000, randomUUID().toString(), "sonore"),
                Operation.Credit(2110, randomUUID().toString(), "sonore"),
                Operation.Debit(222, randomUUID().toString(), "sonore"))

        commands["alotow"] = arrayListOf(
                Operation.Credit(8730, randomUUID().toString(), "alotow"),
                Operation.Debit(953, randomUUID().toString(), "alotow"))
        eventStore = EventStore(commands)

        assertEquals(commands["etreardu"], eventStore.commands("etreardu"))
        assertEquals(commands["sonore"], eventStore.commands("sonore"))
        assertEquals(commands["alotow"], eventStore.commands("alotow"))
    }

    @Test
    fun `should return added events from account`() {
        val commands = LinkedHashMap<String, ArrayList<Operation>>()
        eventStore = EventStore(commands)

        eventStore.add(Operation.Credit(10000, randomUUID().toString(), "etreardu"))

        assertEquals(commands["etreardu"], eventStore.commands("etreardu"))
    }
}
