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
            .match(Operation.Read::class.java) { handleRead(it) }
            .match(Operation.Debit::class.java) { handleDebit(it) }
            .match(Operation.Credit::class.java) { handleCredit(it) }
            .build()

    private fun handleRead(read: Operation.Read) {
        save(read)
        sender.tell(BalanceResponse(balance, StatusResponse.SUCCESS, read.requestId, id), self)
    }

    private fun handleDebit(debit: Operation.Debit) {
        if (hasExecutedOperation(debit)) {
            sender.tell(buildDebitResponse(debit, StatusResponse.ALREADY_EXECUTED), self)
            return
        }

        if (hasBalanceForDebit(debit.amount)) {
            save(debit)
            balance -= debit.amount
            sender.tell(buildDebitResponse(debit), self)
            return
        }

        sender.tell(buildDebitResponse(debit, StatusResponse.ERROR), self)
    }

    private fun handleCredit(credit: Operation.Credit) {
        if (hasExecutedOperation(credit)) {
            sender.tell(buildCreditResponse(credit, StatusResponse.ALREADY_EXECUTED), self)
            return
        }

        save(credit)
        balance += credit.amount
        sender.tell(buildCreditResponse(credit), self)
    }

    private fun save(operation: Operation) = eventStore.add(operation)

    private fun buildCreditResponse(credit: Operation.Credit, status: StatusResponse = StatusResponse.SUCCESS) = CreditResponse(credit.amount, status, credit.requestId, credit.accountId)
    private fun buildDebitResponse(debit: Operation.Debit, status: StatusResponse = StatusResponse.SUCCESS) = DebitResponse(debit.amount, status, debit.requestId, debit.accountId)

    private fun hasExecutedOperation(operation: Operation) = eventStore.commands(id).contains(operation)
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
