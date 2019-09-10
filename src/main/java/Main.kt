import actors.AccountSupervisor
import akka.actor.ActorSystem
import akka.pattern.Patterns.ask
import commands.AccountCommand
import java.util.UUID.randomUUID

fun main(args: Array<String>) {
    val system = ActorSystem.create("kakka-system")

    val commands = LinkedHashMap<String, ArrayList<AccountCommand>>()
    commands["etreardu"] = arrayListOf(
            AccountCommand.Credit(10000, randomUUID().toString(), "etreardu"),
            AccountCommand.Debit(220, randomUUID().toString(), "etreardu"),
            AccountCommand.Credit(4803, randomUUID().toString(), "etreardu"))

    commands["sonore"] = arrayListOf(
            AccountCommand.Credit(4000, randomUUID().toString(), "sonore"),
            AccountCommand.Credit(2110, randomUUID().toString(), "sonore"),
            AccountCommand.Debit(222, randomUUID().toString(), "sonore"))

    commands["alotow"] = arrayListOf(
            AccountCommand.Credit(8730, randomUUID().toString(), "alotow"),
            AccountCommand.Debit(953, randomUUID().toString(), "alotow"))

    val eventStore = EventStore(commands = commands)
    val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")

    ask(supervisor, AccountCommand.Debit(100, randomUUID().toString(), "alotow"), 1000)
    ask(supervisor, AccountCommand.Debit(2330, randomUUID().toString(), "sonore"), 1000)
    ask(supervisor, AccountCommand.Credit(902, randomUUID().toString(), "etreardu"), 1000)
    ask(supervisor, AccountCommand.Credit(1122, randomUUID().toString(), "etreardu"), 1000)
    ask(supervisor, AccountCommand.Credit(233, randomUUID().toString(), "alotow"), 1000)
    ask(supervisor, AccountCommand.Credit(988, randomUUID().toString(), "sonore"), 1000)
    ask(supervisor, AccountCommand.Transfer(2203, "alotow", randomUUID().toString(), "sonore"), 1000)
}
