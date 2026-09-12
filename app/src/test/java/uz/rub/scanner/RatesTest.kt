package uz.rub.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class RatesTest {
    @Test
    fun `defaults are used when preferences are empty`() {
        assertEquals(Rates(110.0, 11750.0, 86.0), RatesRepository(MemoryStorage()).load())
    }

    @Test
    fun `all three rates are saved and read back`() {
        val storage = MemoryStorage()
        val firstRepository = RatesRepository(storage)
        val expected = Rates(125.5, 12_000.25, 91.75)

        firstRepository.save(expected)

        assertEquals(expected, RatesRepository(storage).load())
    }

    @Test
    fun `both prices use supplied rates`() {
        val result = PriceCalculator.calculate(3900.0, Rates.DEFAULT)

        assertEquals(3900.0 / 110.0, result.price1, 0.0000001)
        assertEquals(3900.0 / 11750.0 * 86.0, result.price2, 0.0000001)
    }

    @Test
    fun `calculator preserves decimal source values`() {
        val result = PriceCalculator.calculate(3900.5, Rates(100.0, 1000.0, 10.0))

        assertEquals(39.005, result.price1, 0.0000001)
        assertEquals(39.005, result.price2, 0.0000001)
    }

    @Test
    fun `formats sum and rubles without trailing zero`() {
        assertEquals("Исходная сумма: 3\u00A0900 сум", PriceDisplayFormatter.amount(3900.0))
        assertEquals("Цена 1: 35 руб.", PriceDisplayFormatter.price1(35.0))
        assertEquals("Цена 2: 28,55 руб.", PriceDisplayFormatter.price2(28.55))
    }

    @Test
    fun `formats decimal values with reasonable precision`() {
        assertEquals("Исходная сумма: 3\u00A0900,5 сум", PriceDisplayFormatter.amount(3900.5))
        assertEquals("Цена 1: 35,45 руб.", PriceDisplayFormatter.price1(35.4545))
    }

    private class MemoryStorage : RatesStorage {
        private val values = mutableMapOf<String, String>()
        override fun get(key: String): String? = values[key]
        override fun put(values: Map<String, String>) {
            this.values.putAll(values)
        }
    }
}
