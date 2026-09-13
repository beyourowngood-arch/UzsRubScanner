package uz.rub.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceCalculatorTest {
    @Test
    fun `default rates match product defaults`() {
        assertEquals(ExchangeRates(110.0, 11_750.0, 86.0), ExchangeRates())
    }

    @Test
    fun `extractAmount selects longest formatted number`() {
        assertEquals(1_250_000.0, PriceCalculator.extractAmount("Цена 1 250 000 сум, скидка 10%")!!, 0.001)
        assertNull(PriceCalculator.extractAmount("цена не указана"))
    }

    @Test
    fun `decimal prices use the last decimal separator`() {
        val examples = mapOf(
            "3 900,00" to 3900.0,
            "3.900,00" to 3900.0,
            "3,900.00" to 3900.0,
            "3900,5" to 3900.5,
            "3 900" to 3900.0,
            "3\u00A0900'50" to 390050.0,
        )
        examples.forEach { (source, expected) ->
            assertEquals(source, expected, PriceCalculator.parsePrice(source)!!, 0.001)
        }
    }

    @Test
    fun `conversion uses all configured rates`() {
        val result = PriceCalculator.convert(23_500.0, ExchangeRates(100.0, 11_750.0, 90.0))
        assertEquals(235.0, result.throughRubles, 0.001)
        assertEquals(180.0, result.throughDollars, 0.001)
    }

    @Test
    fun `sum and ruble formatting omit zero fractions and use decimal comma`() {
        assertEquals("3 900", PriceFormatter.amount(3900.0))
        assertEquals("3 900,5", PriceFormatter.amount(3900.5))
        assertEquals("35,45 руб.", PriceFormatter.rubles(35.45))
        assertEquals("36 руб.", PriceFormatter.rubles(36.0))
    }
}
