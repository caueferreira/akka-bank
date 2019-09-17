package requests

import com.fasterxml.jackson.annotation.JsonProperty
import commands.Operation

data class CreditRequest(@JsonProperty("amount") val amount: Long, @JsonProperty("requestId") val requestId: String, @JsonProperty("accountId") val accountId: String)

fun CreditRequest.operation() = Operation.Credit(amount, requestId, accountId)
