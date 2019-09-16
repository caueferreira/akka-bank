package commands

import org.junit.Test
import java.util.UUID.randomUUID
import kotlin.test.assertEquals

class OperationTest {

    private val amount = 1000L
    private val receiver = "receiverId"
    private val account = "requestId"
    private val requestId = randomUUID().toString()

    private val transfer = Operation.Transfer(amount, receiver, requestId, account)

    @Test
    fun `should create debit from transfer`() {
        val expected = Operation.Debit(amount, requestId, account)
        assertEquals(expected, transfer.debit())
    }

    @Test
    fun `should create credit from transfer`() {
        val expected = Operation.Credit(amount, requestId, receiver)
        assertEquals(expected, transfer.credit())
    }

    @Test
    fun `should create compensation from transfer`() {
        val expected = Operation.Debit(amount, requestId, receiver)
        assertEquals(expected, transfer.compensation())
    }
}