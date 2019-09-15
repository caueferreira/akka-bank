package responses

data class CreditResponse(val amount: Long,
                          override val status: StatusResponse,
                          override val requestId: String,
                          override val accountId: String) : Response(requestId, accountId, status)