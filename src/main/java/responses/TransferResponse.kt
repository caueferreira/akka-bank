package responses

data class TransferResponse(
    val amount: Long,
    val receiverId: String,
    override val status: StatusResponse,
    override val requestId: String,
    override val accountId: String
) : Response(requestId, accountId, status)
