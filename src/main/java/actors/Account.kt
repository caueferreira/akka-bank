package actors

import akka.actor.AbstractActor
import akka.actor.Props
import commands.Operation
import responses.BalanceResponse
import responses.CreditResponse
import responses.DebitResponse
import responses.StatusResponse
import source.EventStore

class Account(private val id: String, private val eventStore: EventStore, var balance: Long = 0) : AbstractActor() {

    companion object {
        fun props(accountId: String, eventStore: EventStore): Props {
            return Props.create(Account::class.java) { Account(accountId, eventStore) }
        }
    }

    override fun createReceive(): Receive = receiveBuilder()
            .match(Operation.Read::class.java) { read ->
                save(read)
                sender.tell(BalanceResponse(balance, StatusResponse.SUCCESS, read.requestId, id), self)
            }
            .match(Operation.Debit::class.java) { debit ->
                if (hasBalanceForDebit(debit.amount)) {
                    save(debit)
                    balance -= debit.amount
                    sender.tell(buildDebitResponse(debit), self)
                } else {
                    sender.tell(buildDebitResponse(debit, StatusResponse.ERROR), self)
                }
            }
            .match(Operation.Credit::class.java) { credit ->
                save(credit)
                balance += credit.amount
                sender.tell(buildCreditResponse(credit), self)
            }.build()

    private fun save(operation: Operation) = eventStore.add(operation)

    private fun buildCreditResponse(credit: Operation.Credit) = CreditResponse(credit.amount, StatusResponse.SUCCESS, credit.requestId, credit.accountId)
    private fun buildDebitResponse(debit: Operation.Debit, status: StatusResponse = StatusResponse.SUCCESS) = DebitResponse(debit.amount, status, debit.requestId, debit.accountId)

    private fun hasBalanceForDebit(amount: Long) = balance - amount > -1000

    override fun preStart() {
        eventStore.commands(id).forEach {
            when (it) {
                is Operation.Debit -> balance -= it.amount
                is Operation.Credit -> balance += it.amount
            }
        }
    }
}
