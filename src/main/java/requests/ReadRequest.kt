package requests

import com.fasterxml.jackson.annotation.JsonProperty
import commands.Operation

data class ReadRequest(@JsonProperty("requestId") val requestId: String, @JsonProperty("accountId") val accountId: String)

fun ReadRequest.operation() = Operation.Read(requestId, accountId)
