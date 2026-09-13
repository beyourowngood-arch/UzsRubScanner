package uz.rub.scanner

import android.content.Context
import android.content.SharedPreferences

class RatePreferences(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))

    fun load(): ExchangeRates = ExchangeRates(
        marketUzsPerRub = preferences.readRate(KEY_MARKET, ExchangeRates.DEFAULT_MARKET_UZS_PER_RUB),
        officialUzsPerUsd = preferences.readRate(KEY_UZS_USD, ExchangeRates.DEFAULT_UZS_PER_USD),
        rubPerUsd = preferences.readRate(KEY_RUB_USD, ExchangeRates.DEFAULT_RUB_PER_USD),
    )

    fun save(rates: ExchangeRates) {
        preferences.edit()
            .putString(KEY_MARKET, rates.marketUzsPerRub.toString())
            .putString(KEY_UZS_USD, rates.officialUzsPerUsd.toString())
            .putString(KEY_RUB_USD, rates.rubPerUsd.toString())
            .apply()
    }

    private fun SharedPreferences.readRate(key: String, default: Double): Double = when (val value = all[key]) {
        is String -> value.toDoubleOrNull()
        is Number -> value.toDouble()
        else -> null
    }?.takeIf { it.isFinite() && it > 0 } ?: default

    private companion object {
        const val FILE_NAME = "exchange_rates"
        const val KEY_MARKET = "market_uzs_per_rub"
        const val KEY_UZS_USD = "official_uzs_per_usd"
        const val KEY_RUB_USD = "rub_per_usd"
    }
}
