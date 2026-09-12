package uz.rub.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceParserTest {
    @Test
    fun `space grouped comma decimal price`() = assertPrice(3900.00, "3 900,00")

    @Test
    fun `dot grouped comma decimal price`() = assertPrice(3900.00, "3.900,00")

    @Test
    fun `comma grouped dot decimal price`() = assertPrice(3900.00, "3,900.00")

    @Test
    fun `one digit decimal price`() = assertPrice(3900.5, "3900,5")

    @Test
    fun `integer with grouped thousands`() = assertPrice(3900.0, "3 900")

    @Test
    fun `nbsp narrow nbsp and apostrophes group thousands`() {
        assertPrice(3_900_000.0, "3\u00A0900\u202F000")
        assertPrice(3_900_000.0, "3'900’000")
    }

    @Test
    fun `single punctuation followed by three digits groups thousands`() {
        assertPrice(3900.0, "3.900")
        assertPrice(3900.0, "3,900")
    }

    @Test
    fun `recognized text keeps decimal portion`() {
        val actual = PriceParser.parseRecognizedText("Цена: 3 900,00 UZS")
        assertEquals(3900.0, requireNotNull(actual), 0.0)
    }

    private fun assertPrice(expected: Double, raw: String) {
        assertEquals(expected, requireNotNull(PriceParser.parsePrice(raw)), 0.0)
    }
}
