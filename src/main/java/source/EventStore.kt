package source

import commands.Operation
import kotlin.collections.LinkedHashMap

class EventStore(val commands: LinkedHashMap<String, ArrayList<Operation>> = LinkedHashMap()) {

    fun add(command: Operation) {
        val accountCommands = commands.getOrDefault(command.accountId, arrayListOf())
        accountCommands.add(command)
        commands[command.accountId] = accountCommands
    }

    fun commands(accountId: String): ArrayList<Operation> = commands.getOrDefault(accountId, arrayListOf())
}
