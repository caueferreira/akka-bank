package actors

import responses.CreditResponse
import responses.DebitResponse
import EventStore
import akka.actor.AbstractActor
import akka.actor.Props
import commands.AccountCommand

class Account(private val id: String, private val eventStore: EventStore, var balance: Long = 0) : AbstractActor() {

    companion object {
        fun props(accountId: String, eventStore: EventStore): Props {
            return Props.create(Account::class.java) { Account(accountId, eventStore) }
        }
    }

    override fun createReceive(): Receive = receiveBuilder()
            .match(AccountCommand.Debit::class.java) { debit ->
                println("${debit.requestId} ~ debit of ${debit.amount} to ${debit.accountId}")
                eventStore.add(debit)
                balance -= debit.amount
                sender.tell(DebitResponse(
                        debit.requestId,
                        balance,
                        debit.accountId
                ), self())
            }
            .match(AccountCommand.Credit::class.java) { credit ->
                println("${credit.requestId} ~ credit of ${credit.amount} to ${credit.accountId}")
                eventStore.add(credit)
                balance += credit.amount
                sender.tell(CreditResponse(
                        credit.requestId,
                        balance,
                        credit.accountId
                ), self())
            }.build()

    override fun preStart() {
        eventStore.commands(id).forEach {
            when (it) {
                is AccountCommand.Debit -> balance -= it.amount
                is AccountCommand.Credit -> balance += it.amount
            }
        }

        println("$id has a balance of $balance")
    }
}