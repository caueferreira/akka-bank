package actors

import responses.CreditResponse
import responses.DebitResponse
import source.EventStore
import akka.actor.AbstractActor
import akka.actor.Props
import commands.AccountCommand
import errors.AccountWithoutBalanceForDebit
import responses.BalanceResponse

class Account(private val id: String, private val eventStore: EventStore, var balance: Long = 0) : AbstractActor() {

    companion object {
        fun props(accountId: String, eventStore: EventStore): Props {
            return Props.create(Account::class.java) { Account(accountId, eventStore) }
        }
    }

    override fun createReceive(): Receive = receiveBuilder()
            .match(AccountCommand.Read::class.java) { read ->
                eventStore.add(read)
                sender.tell(BalanceResponse(
                        read.requestId,
                        balance,
                        id
                ), self)
            }
            .match(AccountCommand.Debit::class.java) { debit ->
                if (hasBalanceForDebit(debit.amount)) {
                    eventStore.add(debit)
                    balance -= debit.amount
                    sender.tell(DebitResponse(
                            debit.requestId,
                            debit.amount,
                            debit.accountId
                    ), self)
                } else {
                    println("${debit.requestId} ~ ${debit.accountId} has insufficient balance to debit ${debit.amount}")
                    sender.forward(AccountWithoutBalanceForDebit(), context)
                }
            }
            .match(AccountCommand.Credit::class.java) { credit ->
                eventStore.add(credit)
                balance += credit.amount
                sender.tell(CreditResponse(
                        credit.requestId,
                        credit.amount,
                        credit.accountId
                ), self)
            }.build()

    private fun hasBalanceForDebit(amount: Long) = balance - amount > -1000

    override fun preStart() {
        eventStore.commands(id).forEach {
            when (it) {
                is AccountCommand.Debit -> balance -= it.amount
                is AccountCommand.Credit -> balance += it.amount
            }
        }

        println("$id recovered balance of $balance from event store")
    }
}