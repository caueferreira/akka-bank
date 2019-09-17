package requests

import commands.Operation
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.Test
import responses.BalanceResponse

class ReadRequestTest {

    private val account = "account1"

    @Test
    fun `should build command`() {
        val requestId = randomUUID().toString()

        val expected = Operation.Read(requestId, account)
        val request = ReadRequest(requestId, account)

        assertEquals(expected, request.operation())
    }
}
