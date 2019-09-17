package requests

import commands.Operation

data class CreditRequest(val amount: Long, val requestId: String, val accountId: String)

fun CreditRequest.operation() = Operation.Credit(amount, requestId, accountId)
