package requests

import commands.Operation

data class ReadRequest(val requestId: String, val accountId: String)

fun ReadRequest.operation() = Operation.Read(requestId, accountId)
