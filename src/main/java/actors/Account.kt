package actors

import akka.actor.AbstractActor
import akka.actor.Props
import commands.AccountCommand
import errors.AccountWithoutBalanceForDebit
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
            .match(AccountCommand.Read::class.java) { read ->
                save(read)
                sender.tell(BalanceResponse(
                        balance,
                        StatusResponse.SUCCESS,
                        read.requestId,
                        id
                ), self)
            }
            .match(AccountCommand.Debit::class.java) { debit ->
                if (hasBalanceForDebit(debit.amount)) {
                    save(debit)
                    balance -= debit.amount
                    sender.tell(DebitResponse(
                            debit.amount,
                            StatusResponse.SUCCESS,
                            debit.requestId,
                            debit.accountId
                    ), self)
                } else {
                    sender.tell(AccountWithoutBalanceForDebit(), self)
                    AccountWithoutBalanceForDebit()
                }
            }
            .match(AccountCommand.Credit::class.java) { credit ->
                save(credit)
                balance += credit.amount
                sender.tell(CreditResponse(
                        credit.amount,
                        StatusResponse.SUCCESS,
                        credit.requestId,
                        credit.accountId
                ), self)
            }.build()

    private fun save(read: AccountCommand) {
        eventStore.add(read)
    }

    private fun hasBalanceForDebit(amount: Long) = balance - amount > -1000

    override fun preStart() {
        eventStore.commands(id).forEach {
            when (it) {
                is AccountCommand.Debit -> balance -= it.amount
                is AccountCommand.Credit -> balance += it.amount
            }
        }
    }
}
