package responses

data class CreditResponse(val requestId: String, val balance: Long, val accountId: String) {
    init {
        println("$requestId ~ $accountId has a balance of $balance")
    }
}