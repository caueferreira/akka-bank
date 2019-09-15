package responses

data class BalanceResponse(val requestId: String, val balance: Long, val accountId: String, val status: StatusResponse)
