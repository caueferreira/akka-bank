import actors.AccountSupervisor
import akka.actor.ActorSystem
import commands.Operation
import java.util.UUID.randomUUID
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

        val eventStore = EventStore(events)

        try {
            val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")
            val bankServer = BankServer()

            bankServer.startHttpServer(system, supervisor)
        } finally {
            system.terminate()
        }
    }
}
