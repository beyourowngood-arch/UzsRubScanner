package uz.rub.scanner

import android.content.Context

class RatePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): ExchangeRates = ExchangeRates(
        marketUzsPerRub = preferences.getFloat(KEY_MARKET, 110f).toDouble(),
        officialUzsPerUsd = preferences.getFloat(KEY_UZS_USD, 11_750f).toDouble(),
        rubPerUsd = preferences.getFloat(KEY_RUB_USD, 86f).toDouble(),
    )

    fun save(rates: ExchangeRates) {
        preferences.edit()
            .putFloat(KEY_MARKET, rates.marketUzsPerRub.toFloat())
            .putFloat(KEY_UZS_USD, rates.officialUzsPerUsd.toFloat())
            .putFloat(KEY_RUB_USD, rates.rubPerUsd.toFloat())
            .apply()
    }

    private companion object {
        const val FILE_NAME = "exchange_rates"
        const val KEY_MARKET = "market_uzs_per_rub"
        const val KEY_UZS_USD = "official_uzs_per_usd"
        const val KEY_RUB_USD = "rub_per_usd"
    }
}
