package requests

import commands.Operation
import java.util.UUID.randomUUID
import kotlin.test.assertEquals
import org.junit.Test

class DebitRequestTest {

    private val account = "account1"

    @Test
    fun `should build command`() {
        val requestId = randomUUID().toString()

        val expected = Operation.Debit(1000, requestId, account)
        val request = DebitRequest(1000, requestId, account)

        assertEquals(expected, request.operation())
    }
}
