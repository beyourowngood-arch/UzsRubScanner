package uz.rub.scanner

data class ExchangeRates(
    val marketUzsPerRub: Double = 110.0,
    val officialUzsPerUsd: Double = 11_750.0,
    val rubPerUsd: Double = 86.0,
) {
    fun isValid(): Boolean = marketUzsPerRub > 0 && officialUzsPerUsd > 0 && rubPerUsd > 0
}

data class PriceResult(
    val directRub: Double,
    val crossRateRub: Double,
)

object PriceCalculator {
    fun extractAmount(text: String): Double? = Regex("[0-9][0-9\\s.,'’]*")
        .findAll(text)
        .map { it.value.filter(Char::isDigit) }
        .filter { it.isNotEmpty() }
        .maxByOrNull { it.length }
        ?.toDoubleOrNull()

    fun convert(uzs: Double, rates: ExchangeRates): PriceResult {
        require(rates.isValid())
        return PriceResult(
            directRub = uzs / rates.marketUzsPerRub,
            crossRateRub = uzs / rates.officialUzsPerUsd * rates.rubPerUsd,
        )
    }
}
