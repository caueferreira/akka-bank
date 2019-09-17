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
import com.google.gson.Gson
import java.time.Duration
import java.util.concurrent.CompletionStage
import requests.TransferRequest
import requests.operation
import responses.TransferResponse

class BankServer : AllDirectives() {

    fun startHttpServer(system: ActorSystem, supervisor: ActorRef) {
        val http = Http.get(system)
        val materializer = ActorMaterializer.create(system)

        val routeFlow = createRoute(supervisor).flow(system, materializer)
        val binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 9097), materializer)

        println("Server online at http://localhost:9097/\nPress RETURN to stop...")
        System.`in`.read()
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
                        entity(Jackson.unmarshaller(Any::class.java)) { transfer ->
                            val bids: CompletionStage<TransferResponse> = ask(supervisor,
                                    Gson().fromJson(Gson().toJson(transfer), TransferRequest::class.java).operation(), timeout)
                                    .thenApply { it as TransferResponse }
                            completeOKWithFuture(bids, Jackson.marshaller())
                        }
                    }
                })
    }
}
