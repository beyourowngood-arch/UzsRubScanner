package uz.rub.scanner

import java.text.NumberFormat
import java.util.Locale

data class ExchangeRates(
    val marketUzsPerRub: Double = DEFAULT_MARKET_UZS_PER_RUB,
    val officialUzsPerUsd: Double = DEFAULT_UZS_PER_USD,
    val rubPerUsd: Double = DEFAULT_RUB_PER_USD,
) {
    fun isValid(): Boolean = listOf(marketUzsPerRub, officialUzsPerUsd, rubPerUsd)
        .all { it.isFinite() && it > 0 }

    companion object {
        const val DEFAULT_MARKET_UZS_PER_RUB = 110.0
        const val DEFAULT_UZS_PER_USD = 11_750.0
        const val DEFAULT_RUB_PER_USD = 86.0
    }
}

data class PriceResult(
    val directRub: Double,
    val crossRateRub: Double,
)

object PriceCalculator {
    private val candidatePattern = Regex("[0-9][0-9\\s\\u00A0.,'’]*")

    fun extractAmount(text: String): Double? = candidatePattern.findAll(text)
        .mapNotNull { match ->
            parsePrice(match.value)?.let { value -> match.value.count(Char::isDigit) to value }
        }
        .maxByOrNull { (digitCount, _) -> digitCount }
        ?.second

    fun parsePrice(raw: String): Double? {
        val compact = raw.replace(Regex("[\\s\\u00A0'’]"), "")
        if (compact.isEmpty() || compact.any { !it.isDigit() && it != '.' && it != ',' }) return null

        val dot = compact.lastIndexOf('.')
        val comma = compact.lastIndexOf(',')
        val decimalIndex = when {
            dot >= 0 && comma >= 0 -> maxOf(dot, comma)
            dot >= 0 -> dot.takeIf { compact.length - it - 1 in 1..2 }
            comma >= 0 -> comma.takeIf { compact.length - it - 1 in 1..2 }
            else -> null
        }

        val normalized = buildString {
            compact.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    index == decimalIndex -> append('.')
                }
            }
        }
        return normalized.toDoubleOrNull()
    }

    fun convert(amount: Double, rates: ExchangeRates): PriceResult {
        require(rates.isValid())
        return PriceResult(
            directRub = amount / rates.marketUzsPerRub,
            crossRateRub = amount / rates.officialUzsPerUsd * rates.rubPerUsd,
        )
    }
}

object PriceFormatter {
    private val russianLocale = Locale("ru", "RU")

    fun amount(value: Double): String = number(value)

    fun rubles(value: Double): String = "${number(value)} руб."

    private fun number(value: Double): String = NumberFormat.getNumberInstance(russianLocale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        isGroupingUsed = true
    }.format(value).replace('\u00A0', ' ').replace('\u202F', ' ')
}
