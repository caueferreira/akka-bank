package requests

import commands.Operation
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.Test

class CreditRequestTest {

    private val account = "account1"

    @Test
    fun `should build command`() {
        val requestId = randomUUID().toString()

        val expected = Operation.Credit(1000, requestId, account)
        val request = CreditRequest(1000, requestId, account)

        assertEquals(expected, request.operation())
    }
}
