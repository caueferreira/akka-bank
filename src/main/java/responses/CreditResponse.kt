package responses

data class CreditResponse(val requestId: String, val amount: Long, val accountId: String) {
    init {
        println("$requestId ~ $accountId was credited $amount")
    }
}