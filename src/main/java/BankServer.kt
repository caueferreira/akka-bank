import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.pattern.Patterns.ask
import akka.stream.ActorMaterializer
import com.google.gson.Gson
import requests.*
import java.time.Duration
import java.util.UUID.randomUUID
import java.util.concurrent.CompletionStage
import responses.BalanceResponse
import responses.CreditResponse
import responses.DebitResponse
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
                pathPrefix("balance") {
                    path<String>(segment()) { id: String ->
                        get {
                            val response = ask(supervisor, ReadRequest(randomUUID().toString(), id).operation(), timeout)
                                    .thenApply { it as BalanceResponse }
                            completeOKWithFuture(response, Jackson.marshaller())
                        }
                    }
                },
                path("transfer") {
                    post {
                        entity(Jackson.unmarshaller(Any::class.java)) { transfer ->
                            val response: CompletionStage<TransferResponse> = ask(supervisor,
                                    Gson().fromJson(Gson().toJson(transfer), TransferRequest::class.java).operation(), timeout)
                                    .thenApply { it as TransferResponse }
                            completeOKWithFuture(response, Jackson.marshaller())
                        }
                    }
                },
                path("credit") {
                    post {
                        entity(Jackson.unmarshaller(Any::class.java)) { credit ->
                            val response: CompletionStage<CreditResponse> = ask(supervisor,
                                    Gson().fromJson(Gson().toJson(credit), CreditRequest::class.java).operation(), timeout)
                                    .thenApply { it as CreditResponse }
                            completeOKWithFuture(response, Jackson.marshaller())
                        }
                    }
                },
                path("debit") {
                    post {
                        entity(Jackson.unmarshaller(Any::class.java)) { debit ->
                            val response: CompletionStage<DebitResponse> = ask(supervisor,
                                    Gson().fromJson(Gson().toJson(debit), DebitRequest::class.java).operation(), timeout)
                                    .thenApply { it as DebitResponse }
                            completeOKWithFuture(response, Jackson.marshaller())
                        }
                    }
                })
    }
}
