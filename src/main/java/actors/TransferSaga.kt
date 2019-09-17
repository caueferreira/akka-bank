package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.Patterns.ask
import commands.Operation
import commands.compensation
import commands.credit
import commands.debit
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
        println("TransferSaga: Starting transfer from ${transfer.accountId} to ${transfer.receiverId} with amount of ${transfer.amount}")
        val debit = ask(from, transfer.debit(), 200)
        val credit = ask(to, transfer.credit(), 200)

        handleFuture(sender, transfer, credit.zip(debit))
    }

    private fun handleFuture(sender: ActorRef, transfer: Operation.Transfer, zip: Future<Tuple2<Any, Any>>) = zip.onComplete({
        val debit = (it.get()._2 as DebitResponse)
        if (StatusResponse.ERROR == debit.status) {
            println("TransferSaga: Transfer ${transfer.requestId} received an error")
            compensation(transfer)
        }

        println("TransferSaga: Transfer ${transfer.requestId} was successfully executed")
        sender.tell(buildTransfer(transfer, debit.status), self)
    }, context.system.dispatcher)

    private fun buildTransfer(transfer: Operation.Transfer, status: StatusResponse) = TransferResponse(
            transfer.amount,
            transfer.receiverId,
            status,
            transfer.requestId,
            transfer.accountId)

    private fun compensation(transfer: Operation.Transfer) {
        println("TransferSaga: Triggered compensation for ${transfer.receiverId} due lack of funds of ${transfer.accountId}")
        to.forward(transfer.compensation(), context)
    }
}
