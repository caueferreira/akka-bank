package responses

abstract class Response(open val requestId: String, open val accountId: String, open val status: StatusResponse)