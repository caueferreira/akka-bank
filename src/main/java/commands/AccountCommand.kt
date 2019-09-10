package commands

sealed class AccountCommand(requestId: String, accountId: String) : Command(requestId, accountId) {

    data class Credit(val amount: Long, override val requestId: String, override val accountId: String) : AccountCommand(requestId, accountId)
    data class Debit(val amount: Long, override val requestId: String, override val accountId: String) : AccountCommand(requestId, accountId)
    data class Transfer(val amount: Long, val receiverId: String, override val requestId: String, override val accountId: String) : AccountCommand(requestId, accountId)
}