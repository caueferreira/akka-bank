import commands.AccountCommand
import kotlin.collections.LinkedHashMap

class EventStore(val commands: LinkedHashMap<String, ArrayList<AccountCommand>> = LinkedHashMap()) {

    fun add(command: AccountCommand) {
        val accountCommands = commands.getOrDefault(command.accountId, arrayListOf())
        accountCommands.add(command)
        commands[command.accountId] = accountCommands
    }

    fun commands(accountId: String): ArrayList<AccountCommand> = commands.getOrDefault(accountId, arrayListOf())
}