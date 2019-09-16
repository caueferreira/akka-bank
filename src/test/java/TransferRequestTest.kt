import commands.Operation
import org.junit.Test
import java.util.UUID.randomUUID
import kotlin.test.assertEquals

class TransferRequestTest {

    private val requester = "account1"
    private val receiver = "account2"

    @Test
    fun `should build command`() {
        val requestId = randomUUID().toString()

        val expected = Operation.Transfer(1000, receiver, requestId, requester)
        val request = TransferRequest(1000, receiver, requestId, requester)

        assertEquals(expected, request.operation())
    }
}