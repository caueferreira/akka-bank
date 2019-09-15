package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import commands.AccountCommand
import commands.Command
import java.util.UUID.randomUUID
import kotlin.collections.HashMap
import source.EventStore

class AccountSupervisor(private val eventStore: EventStore) : AbstractActor() {

    private val accountIdToActor: MutableMap<String, ActorRef> = HashMap()

    companion object {
        fun props(eventStore: EventStore): Props {
            return Props.create(AccountSupervisor::class.java) { AccountSupervisor(eventStore) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(AccountCommand.Transfer::class.java, ::handleTransfer)
                .match(Command::class.java, ::handleCommand)
                .build()
    }

    private fun handleTransfer(transfer: AccountCommand.Transfer) {
        val transferSaga = TransferSaga.props(
                accountIdToActor[transfer.accountId]!!,
                accountIdToActor[transfer.receiverId]!!)

        context.actorOf(transferSaga, "transfer-saga:${transfer.accountId}:${randomUUID()}")
                .forward(transfer, context)
    }

    private fun handleCommand(command: Command) {
        accountIdToActor[command.accountId]?.forward(command, context)
    }

    override fun preStart() {
        eventStore.commands.keys.forEach {
            accountIdToActor[it] = context.actorOf(Account.props(it, eventStore), it)
        }
    }
}
