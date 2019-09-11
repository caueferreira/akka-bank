package actors

import source.EventStore
import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import commands.AccountCommand
import commands.Command
import akka.actor.SupervisorStrategy
import akka.japi.pf.DeciderBuilder
import akka.actor.OneForOneStrategy
import errors.AccountWithoutBalanceForDebit
import java.lang.Exception
import java.time.Duration
import java.util.*

class AccountSupervisor(private val eventStore: EventStore) : AbstractActor() {

    private val accountIdToActor: MutableMap<String, ActorRef> = HashMap()

    companion object {
        fun props(eventStore: EventStore): Props {
            return Props.create(AccountSupervisor::class.java) { AccountSupervisor(eventStore) }
        }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(Command::class.java, ::handleCommand)
                .build()
    }

    private fun handleCommand(command: Command) {
        when (command) {
            is AccountCommand.Transfer -> {
                println("${command.requestId} ~ ${command.accountId} requested a transfer of ${command.amount} to ${command.receiverId}")
                val transferSaga = TransferSaga.props(accountIdToActor[command.accountId]!!,
                        accountIdToActor[command.receiverId]!!,
                        command)

                context.actorOf(transferSaga, "transfer-saga:" + UUID.randomUUID())
                        .forward(command, context)
            }
            else -> accountIdToActor[command.accountId]?.forward(command, context)
        }
    }

    override fun preStart() {
        eventStore.commands.keys.forEach {
            accountIdToActor[it] = context.actorOf(Account.props(it, eventStore), it)
        }
    }

}