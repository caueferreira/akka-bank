package requests

import com.fasterxml.jackson.annotation.JsonProperty
import commands.Operation

data class DebitRequest(@JsonProperty("amount") val amount: Long, @JsonProperty("requestId") val requestId: String, @JsonProperty("accountId") val accountId: String)

fun DebitRequest.operation() = Operation.Debit(amount, requestId, accountId)
