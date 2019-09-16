import actors.AccountSupervisor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import akka.pattern.Patterns.ask
import akka.stream.ActorMaterializer
import commands.Operation
import java.time.Duration
import java.util.concurrent.CompletionStage
import responses.TransferResponse
import source.EventStore

class BankServer : AllDirectives() {

    init {
        val system = ActorSystem.create("kakka-system")
        val eventStore = EventStore()

        try {
            val supervisor = system.actorOf(AccountSupervisor.props(eventStore), "account-supervisor")
            startHttpServer(system, supervisor)
        } finally {
            system.terminate()
        }
    }

    private fun startHttpServer(system: ActorSystem, supervisor: ActorRef) {
        val http = Http.get(system)
        val materializer = ActorMaterializer.create(system)

        val app = BankServer()

        val routeFlow = app.createRoute(supervisor).flow(system, materializer)
        val binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 9097), materializer)

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept { system.terminate() }
    }

    private fun createRoute(supervisor: ActorRef): Route {
        val timeout = Duration.ofSeconds(5L)
        return concat(
                path("transfer"
                ) {
                    post {
                        entity(Jackson.unmarshaller(Operation.Transfer::class.java)) { transfer ->
                            println("received $transfer")
                            val bids: CompletionStage<TransferResponse> = ask(supervisor, transfer, timeout)
                                    .thenApply { it as TransferResponse }
                            completeOKWithFuture(bids, Jackson.marshaller())
                        }
                    }
                })
    }
}
