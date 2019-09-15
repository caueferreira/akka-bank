package actors

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.Patterns.ask
import commands.AccountCommand
import errors.AccountWithoutBalanceForDebit
import responses.StatusResponse
import responses.TransferResponse

class TransferSaga(private val from: ActorRef, private val to: ActorRef, private val transfer: AccountCommand.Transfer) : AbstractActor() {

    companion object {
        fun props(from: ActorRef, to: ActorRef, transfer: AccountCommand.Transfer): Props {
            return Props.create(TransferSaga::class.java) { TransferSaga(from, to, transfer) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(AccountCommand.Transfer::class.java, ::transfer)
                .build()
    }

    private fun transfer(command: AccountCommand.Transfer) {
        val debit = ask(from,
                AccountCommand.Debit(
                        command.amount,
                        command.requestId,
                        command.accountId
                ), 200)

        val credit = ask(to,
                AccountCommand.Credit(
                        command.amount,
                        command.requestId,
                        command.receiverId
                ), 200)

        val currentSender = sender
        credit.zip(debit).onComplete({
            if (it.get()._2 is AccountWithoutBalanceForDebit) {
                compensation()
                currentSender.tell(TransferResponse(
                        transfer.requestId,
                        transfer.amount,
                        transfer.accountId,
                        transfer.receiverId,
                        StatusResponse.ERROR),
                        self)
            } else {
                currentSender.tell(TransferResponse(
                        transfer.requestId,
                        transfer.amount,
                        transfer.accountId,
                        transfer.receiverId,
                        StatusResponse.SUCCESS),
                        self)
            }
        }, context.system.dispatcher)
    }

    private fun compensation() {
        println("${transfer.requestId} ~ triggered compensation for ${transfer.accountId} with amount ${transfer.amount}")
        to.forward(AccountCommand.Debit(
                transfer.amount, transfer.requestId, transfer.receiverId
        ), context)
    }
}
