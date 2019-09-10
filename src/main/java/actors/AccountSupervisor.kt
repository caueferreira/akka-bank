package actors

import EventStore
import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import commands.AccountCommand
import commands.Command
import java.util.HashMap

class AccountSupervisor(private val eventStore: EventStore) : AbstractActor() {

    private val accountIdToActor: MutableMap<String, ActorRef> = HashMap()

    companion object {
        fun props(eventStore: EventStore): Props {
            return Props.create(AccountSupervisor::class.java) { AccountSupervisor(eventStore) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(Command::class.java, ::handleCommand)
                .build()
    }

    private fun handleCommand(command: Command) {
        when (command) {
            is AccountCommand.Transfer -> {
                println("${command.requestId} ~ ${command.accountId} requested a transfer of ${command.amount} to ${command.receiverId}")
                accountIdToActor[command.accountId]?.forward(AccountCommand.Debit(
                        command.amount, command.requestId, command.accountId
                ), context)
                accountIdToActor[command.receiverId]?.forward(AccountCommand.Credit(
                        command.amount, command.requestId, command.receiverId
                ), context)
            }
            else -> accountIdToActor[command.accountId]?.forward(command, context)
        }
    }

    override fun preStart() {
        eventStore.commands.keys.forEach {
            accountIdToActor[it] = context.actorOf(Account.props(it, eventStore), it)
        }
    }
}