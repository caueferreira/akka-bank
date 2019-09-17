package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import commands.Command
import commands.Operation
import java.util.UUID.randomUUID
import kotlin.collections.HashMap
import responses.StatusResponse
import responses.TransferResponse
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
                .match(Operation.Transfer::class.java, ::handleTransfer)
                .match(Command::class.java, ::handleCommand)
                .build()
    }

    private fun handleTransfer(transfer: Operation.Transfer) {
        val from = accountIdToActor[transfer.accountId]
        val to = accountIdToActor[transfer.receiverId]

        if (from != null && to != null) {
            val transferSaga = TransferSaga.props(
                    from,
                    to)

            context.actorOf(transferSaga, "transfer-saga:${transfer.accountId}:${randomUUID()}")
                    .forward(transfer, context)
        } else {
            sender.tell(TransferResponse(transfer.amount, transfer.receiverId, StatusResponse.ERROR, transfer.requestId, transfer.accountId), self)
        }
    }

    private fun handleCommand(command: Command) {
        accountIdToActor[command.accountId]?.forward(command, context)
    }

    override fun preStart() {
        println("AccountSupervisor: Recovering events of accounts ${eventStore.commands.keys}")
        eventStore.commands.keys.forEach {
            accountIdToActor[it] = context.actorOf(Account.props(it, eventStore), it)
        }
    }
}
