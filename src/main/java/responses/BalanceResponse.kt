package responses

data class BalanceResponse(val balance: Long,
                           override val status: StatusResponse,
                           override val requestId: String,
                           override val accountId: String) : Response(requestId, accountId, status)