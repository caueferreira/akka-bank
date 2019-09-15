package commands

sealed class Operation(requestId: String, accountId: String) : Command(requestId, accountId) {

    data class Read(override val requestId: String, override val accountId: String) : Operation(requestId, accountId)
    data class Credit(val amount: Long, override val requestId: String, override val accountId: String) : Operation(requestId, accountId)
    data class Debit(val amount: Long, override val requestId: String, override val accountId: String) : Operation(requestId, accountId)
    data class Transfer(val amount: Long, val receiverId: String, override val requestId: String, override val accountId: String) : Operation(requestId, accountId)
}

fun Operation.Transfer.debit() = Operation.Debit(amount, requestId, accountId)
fun Operation.Transfer.credit() = Operation.Credit(amount, requestId, receiverId)
fun Operation.Transfer.compensation() = Operation.Debit(amount, requestId, receiverId)