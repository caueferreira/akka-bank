package responses

data class DebitResponse(val amount: Long,
                         override val status: StatusResponse,
                         override val requestId: String,
                         override val accountId: String) : Response(requestId, accountId, status)