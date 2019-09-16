import commands.Operation

data class TransferRequest(val amount: Long, val receiverId: String, val requestId: String, val accountId: String)

fun TransferRequest.operation() = Operation.Transfer(amount, receiverId, requestId, accountId)