package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.Patterns.ask
import commands.*
import errors.AccountWithoutBalanceForDebit
import responses.StatusResponse
import responses.TransferResponse
import java.time.Duration

class TransferSaga(private val from: ActorRef, private val to: ActorRef) : AbstractActor() {

    companion object {
        fun props(from: ActorRef, to: ActorRef): Props {
            return Props.create(TransferSaga::class.java) { TransferSaga(from, to) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(AccountCommand.Transfer::class.java, ::transfer)
                .build()
    }

    private fun transfer(transfer: AccountCommand.Transfer) {
        val debit = ask(from, transfer.debit(), 200)
        val credit = ask(to, transfer.credit(), 200)

        val currentSender = sender
        credit.zip(debit).onComplete({
            if (it.get()._2 is AccountWithoutBalanceForDebit) {
                compensation(transfer)
                currentSender.tell(
                        buildTransfer(transfer, StatusResponse.ERROR),
                        self)
            } else {
                currentSender.tell(
                        buildTransfer(transfer, StatusResponse.SUCCESS),
                        self)
            }
        }, context.system.dispatcher)
    }

    private fun buildTransfer(transfer: AccountCommand.Transfer, status: StatusResponse) = TransferResponse(
            transfer.amount,
            transfer.receiverId,
            status,
            transfer.requestId,
            transfer.accountId)

    private fun compensation(transfer: AccountCommand.Transfer) {
        to.forward(transfer.compensation(), context)
    }
}
