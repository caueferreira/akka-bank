package actors

import akka.actor.*
import akka.japi.pf.DeciderBuilder
import commands.AccountCommand
import commands.Command
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
        from.forward(AccountCommand.Debit(
                command.amount, command.requestId, command.accountId
        ), context)
        to.forward(AccountCommand.Credit(
                command.amount, command.requestId, command.receiverId
        ), context)
    }

    private fun handleError(exception: Exception) {
        println("handleError $exception")
    }

    private val strategy = OneForOneStrategy(
            10,
            Duration.ofMinutes(1),
            DeciderBuilder
                    .match(AccountWithoutBalanceForDebit::class.java) {
                        println("${transfer.requestId} ~ triggered compensation for ${transfer.accountId} with amount ${transfer.amount}")
                        to.forward(AccountCommand.Debit(
                                transfer.amount, transfer.requestId, transfer.accountId
                        ), context)
                        SupervisorStrategy.resume()
                    }
                    .build())

    override fun supervisorStrategy(): SupervisorStrategy {
        return strategy
    }
}