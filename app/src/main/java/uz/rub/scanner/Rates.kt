package uz.rub.scanner

import android.content.SharedPreferences
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class Rates(val rate1: Double, val rate2: Double, val rate3: Double) {
    companion object {
        val DEFAULT = Rates(110.0, 11750.0, 86.0)
    }
}

interface RatesStorage {
    fun get(key: String): String?
    fun put(values: Map<String, String>)
}

class SharedPreferencesRatesStorage(private val preferences: SharedPreferences) : RatesStorage {
    override fun get(key: String): String? = preferences.getString(key, null)

    override fun put(values: Map<String, String>) {
        preferences.edit().apply {
            values.forEach { (key, value) -> putString(key, value) }
        }.apply()
    }
}

class RatesRepository(private val storage: RatesStorage) {
    fun load(): Rates = Rates(
        readPositive(RATE_1, Rates.DEFAULT.rate1),
        readPositive(RATE_2, Rates.DEFAULT.rate2),
        readPositive(RATE_3, Rates.DEFAULT.rate3),
    )

    fun save(rates: Rates) {
        require(rates.rate1.isFinite() && rates.rate1 > 0)
        require(rates.rate2.isFinite() && rates.rate2 > 0)
        require(rates.rate3.isFinite() && rates.rate3 > 0)
        storage.put(
            mapOf(
                RATE_1 to rates.rate1.toString(),
                RATE_2 to rates.rate2.toString(),
                RATE_3 to rates.rate3.toString(),
            ),
        )
    }

    private fun readPositive(key: String, default: Double): Double =
        storage.get(key)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: default

    private companion object {
        const val RATE_1 = "rate_1"
        const val RATE_2 = "rate_2"
        const val RATE_3 = "rate_3"
    }
}

data class ConvertedPrices(val price1: Double, val price2: Double)

object PriceCalculator {
    fun calculate(amount: Double, rates: Rates): ConvertedPrices = ConvertedPrices(
        price1 = amount / rates.rate1,
        price2 = amount / rates.rate2 * rates.rate3,
    )
}

object PriceDisplayFormatter {
    private fun number(value: Double): String = DecimalFormat(
        "#,##0.##",
        DecimalFormatSymbols(Locale("ru", "RU")),
    ).format(value)

    fun amount(amount: Double): String = "Исходная сумма: ${number(amount)} сум"
    fun price1(value: Double): String = "Цена 1: ${number(value)} руб."
    fun price2(value: Double): String = "Цена 2: ${number(value)} руб."
}
