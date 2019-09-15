package source

import commands.AccountCommand
import org.junit.Test
import java.util.UUID.randomUUID
import kotlin.test.assertEquals

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
        val commands = LinkedHashMap<String, ArrayList<AccountCommand>>()
        commands["etreardu"] = arrayListOf(
                AccountCommand.Credit(10000, randomUUID().toString(), "etreardu"),
                AccountCommand.Debit(220, randomUUID().toString(), "etreardu"),
                AccountCommand.Credit(4803, randomUUID().toString(), "etreardu"))

        commands["sonore"] = arrayListOf(
                AccountCommand.Credit(4000, randomUUID().toString(), "sonore"),
                AccountCommand.Credit(2110, randomUUID().toString(), "sonore"),
                AccountCommand.Debit(222, randomUUID().toString(), "sonore"))

        commands["alotow"] = arrayListOf(
                AccountCommand.Credit(8730, randomUUID().toString(), "alotow"),
                AccountCommand.Debit(953, randomUUID().toString(), "alotow"))
        eventStore = EventStore(commands)

        val events = eventStore.commands

        assertEquals(commands, events)
    }

    @Test
    fun `should return all events from account`() {
        val commands = LinkedHashMap<String, ArrayList<AccountCommand>>()
        commands["etreardu"] = arrayListOf(
                AccountCommand.Credit(10000, randomUUID().toString(), "etreardu"),
                AccountCommand.Debit(220, randomUUID().toString(), "etreardu"),
                AccountCommand.Credit(4803, randomUUID().toString(), "etreardu"))

        commands["sonore"] = arrayListOf(
                AccountCommand.Credit(4000, randomUUID().toString(), "sonore"),
                AccountCommand.Credit(2110, randomUUID().toString(), "sonore"),
                AccountCommand.Debit(222, randomUUID().toString(), "sonore"))

        commands["alotow"] = arrayListOf(
                AccountCommand.Credit(8730, randomUUID().toString(), "alotow"),
                AccountCommand.Debit(953, randomUUID().toString(), "alotow"))
        eventStore = EventStore(commands)

        assertEquals(commands["etreardu"], eventStore.commands("etreardu"))
        assertEquals(commands["sonore"], eventStore.commands("sonore"))
        assertEquals(commands["alotow"], eventStore.commands("alotow"))
    }

    @Test
    fun `should return added events from account`() {
        val commands = LinkedHashMap<String, ArrayList<AccountCommand>>()
        eventStore = EventStore(commands)

        eventStore.add(AccountCommand.Credit(10000, randomUUID().toString(), "etreardu"))

        assertEquals(commands["etreardu"], eventStore.commands("etreardu"))
    }
}