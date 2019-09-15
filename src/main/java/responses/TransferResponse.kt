package responses

data class TransferResponse(val requestId: String, val amount: Long, val accountId: String, val receiverId: String, val status: StatusResponse)