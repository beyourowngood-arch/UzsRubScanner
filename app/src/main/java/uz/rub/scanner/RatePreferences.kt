package uz.rub.scanner

import android.content.Context
import android.content.SharedPreferences

class RatePreferences internal constructor(private val storage: RateStorage) {
    constructor(context: Context) : this(SharedPreferencesRateStorage(
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    ))
    internal constructor(preferences: SharedPreferences) : this(SharedPreferencesRateStorage(preferences))

    fun load(): ExchangeRates = ExchangeRates(
        rateOne = storage.read(KEY_RATE_ONE, ExchangeRates.DEFAULT_RATE_ONE),
        rateTwo = storage.read(KEY_RATE_TWO, ExchangeRates.DEFAULT_RATE_TWO),
        rateThree = storage.read(KEY_RATE_THREE, ExchangeRates.DEFAULT_RATE_THREE),
    )

    fun save(rates: ExchangeRates) {
        require(rates.isValid())
        storage.write(
            mapOf(
                KEY_RATE_ONE to rates.rateOne,
                KEY_RATE_TWO to rates.rateTwo,
                KEY_RATE_THREE to rates.rateThree,
            ),
        )
    }

    private companion object {
        const val FILE_NAME = "exchange_rates"
        const val KEY_RATE_ONE = "rate_one"
        const val KEY_RATE_TWO = "rate_two"
        const val KEY_RATE_THREE = "rate_three"
    }
}

internal interface RateStorage {
    fun read(key: String, default: Double): Double
    fun write(values: Map<String, Double>)
}

private class SharedPreferencesRateStorage(private val preferences: SharedPreferences) : RateStorage {
    override fun read(key: String, default: Double): Double =
        preferences.getString(key, null)?.toDoubleOrNull() ?: default

    override fun write(values: Map<String, Double>) {
        preferences.edit().apply {
            values.forEach { (key, value) -> putString(key, value.toString()) }
        }.apply()
    }
}
