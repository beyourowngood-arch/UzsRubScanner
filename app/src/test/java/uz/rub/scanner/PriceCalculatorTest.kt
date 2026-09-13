package uz.rub.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceCalculatorTest {
    @Test
    fun `extractAmount selects longest formatted number`() {
        assertEquals(
            1_250_000.0,
            requireNotNull(PriceCalculator.extractAmount("Цена 1 250 000 сум, скидка 10%")),
            0.001,
        )
    }

    @Test
    fun `extractAmount returns null without digits`() {
        assertNull(PriceCalculator.extractAmount("цена не указана"))
    }

    @Test
    fun `parsePrice keeps decimal part and removes grouping separators`() {
        assertEquals(1_234.56, requireNotNull(PriceCalculator.parsePrice("1 234,56")), 0.001)
        assertEquals(1_234.56, requireNotNull(PriceCalculator.parsePrice("1.234,56")), 0.001)
        assertEquals(1_234.56, requireNotNull(PriceCalculator.parsePrice("1,234.56")), 0.001)
        assertEquals(1_234.0, requireNotNull(PriceCalculator.parsePrice("1.234")), 0.001)
    }

    @Test
    fun `convert uses configurable direct and cross rates`() {
        val result = PriceCalculator.convert(
            uzs = 23_500.0,
            rates = ExchangeRates(marketUzsPerRub = 100.0, officialUzsPerUsd = 11_750.0, rubPerUsd = 90.0),
        )

        assertEquals(235.0, result.directRub, 0.001)
        assertEquals(180.0, result.crossRateRub, 0.001)
    }
}
