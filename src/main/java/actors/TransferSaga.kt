package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.Patterns.ask
import commands.*
import responses.CreditResponse
import responses.DebitResponse
import responses.StatusResponse
import responses.TransferResponse
import scala.Tuple2
import scala.concurrent.Future

class TransferSaga(private val from: ActorRef, private val to: ActorRef) : AbstractActor() {

    companion object {
        fun props(from: ActorRef, to: ActorRef): Props {
            return Props.create(TransferSaga::class.java) { TransferSaga(from, to) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(Operation.Transfer::class.java, ::transfer)
                .build()
    }

    private fun transfer(transfer: Operation.Transfer) {
        val debit = ask(from, transfer.debit(), 200)
        val credit = ask(to, transfer.credit(), 200)

        handleFuture(sender, transfer, credit.zip(debit))
    }

    private fun handleFuture(sender: ActorRef, transfer: Operation.Transfer, zip: Future<Tuple2<Any, Any>>) = zip.onComplete({
        val debit = (it.get()._2 as DebitResponse)
        if (StatusResponse.ERROR == debit.status) {
            compensation(transfer)
        }

        sender.tell(buildTransfer(transfer, debit.status), self)
    }, context.system.dispatcher)

    private fun buildTransfer(transfer: Operation.Transfer, status: StatusResponse) = TransferResponse(
            transfer.amount,
            transfer.receiverId,
            status,
            transfer.requestId,
            transfer.accountId)

    private fun compensation(transfer: Operation.Transfer) {
        to.forward(transfer.compensation(), context)
    }
}
