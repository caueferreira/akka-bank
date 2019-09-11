package responses

data class DebitResponse(val requestId: String, val amount: Long, val accountId: String) {
    init {
        println("$requestId ~ $accountId was debited $amount")
    }
}