package requests

import commands.Operation

data class DebitRequest(val amount: Long, val requestId: String, val accountId: String)

fun DebitRequest.operation() = Operation.Debit(amount, requestId, accountId)
