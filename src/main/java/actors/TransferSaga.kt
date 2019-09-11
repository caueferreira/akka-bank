package actors

import akka.actor.*
import akka.japi.pf.DeciderBuilder
import commands.AccountCommand
import errors.AccountWithoutBalanceForDebit
import java.lang.Exception
import java.time.Duration

class TransferSaga(private val from: ActorRef, private val to: ActorRef, private val transfer: AccountCommand.Transfer) : AbstractActor() {

    companion object {
        fun props(from: ActorRef, to: ActorRef, transfer: AccountCommand.Transfer): Props {
            return Props.create(TransferSaga::class.java) { TransferSaga(from, to, transfer) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(AccountCommand.Transfer::class.java, ::transfer)
                .match(AccountWithoutBalanceForDebit::class.java, ::handleError)
                .build()
    }

    private fun transfer(command: AccountCommand.Transfer) {
        from.tell(AccountCommand.Debit(
                command.amount, command.requestId, command.accountId
        ), self)
        to.tell(AccountCommand.Credit(
                command.amount, command.requestId, command.receiverId
        ), self)
    }

    private fun handleError(exception: Exception) {
        println("${transfer.requestId} ~ triggered compensation for ${transfer.accountId} with amount ${transfer.amount}")
        to.forward(AccountCommand.Debit(
                transfer.amount, transfer.requestId, transfer.receiverId
        ), context)
    }
}