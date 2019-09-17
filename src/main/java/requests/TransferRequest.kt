package requests

import com.fasterxml.jackson.annotation.JsonProperty
import commands.Operation

data class TransferRequest(@JsonProperty("amount") val amount: Long, @JsonProperty("receiverId") val receiverId: String, @JsonProperty("requestId") val requestId: String, @JsonProperty("accountId") val accountId: String)

fun TransferRequest.operation() = Operation.Transfer(amount, receiverId, requestId, accountId)
