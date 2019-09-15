package responses

data class DebitResponse(val requestId: String, val amount: Long, val accountId: String, val status: StatusResponse)