package responses

data class TransferResponse(val requestId: String, val amount: Long, val accountId: String, val receiverId: String, val status: StatusResponse){
    init {
        println("$requestId ~ transfer between $accountId and $receiverId with amount $amount was $status")
    }
}
