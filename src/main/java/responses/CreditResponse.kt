package responses

data class CreditResponse(val requestId: String, val amount: Long, val accountId: String, val status: StatusResponse)
