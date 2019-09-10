package responses

data class DebitResponse(val requestId: String, val balance: Long, val accountId: String) {
    init {
        println("$requestId ~ $accountId has a balance of $balance")
    }
}