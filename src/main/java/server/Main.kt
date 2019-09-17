package server

import akka.actor.ActorSystem
import commands.Operation
import java.util.UUID.randomUUID
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import source.EventStore

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val system = ActorSystem.create("kakka-system")
        val events = LinkedHashMap<String, ArrayList<Operation>>()
        events["account1"] = arrayListOf<Operation>(
                Operation.Credit(10000, randomUUID().toString(), "account1"))
        events["account2"] = arrayListOf<Operation>(
                Operation.Credit(4000, randomUUID().toString(), "account2"))
        events["account3"] = arrayListOf<Operation>(
                Operation.Debit(2883, "same-id-from-request-debit", "account3"),
                Operation.Credit(40000, "same-id-from-request-credit", "account3"))

        val eventStore = EventStore(events)
        BankServer(system, eventStore)
    }
}
