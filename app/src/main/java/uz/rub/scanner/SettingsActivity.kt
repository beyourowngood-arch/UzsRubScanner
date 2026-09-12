package uz.rub.scanner

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var rate1: EditText
    private lateinit var rate2: EditText
    private lateinit var rate3: EditText
    private lateinit var repository: RatesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        repository = RatesRepository(
            SharedPreferencesRatesStorage(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)),
        )
        rate1 = findViewById(R.id.rate1)
        rate2 = findViewById(R.id.rate2)
        rate3 = findViewById(R.id.rate3)

        val rates = repository.load()
        rate1.setText(editableValue(rates.rate1))
        rate2.setText(editableValue(rates.rate2))
        rate3.setText(editableValue(rates.rate3))
        findViewById<android.view.View>(R.id.saveButton).setOnClickListener { save() }
    }

    private fun save() {
        val rates = Rates(
            rate1.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN,
            rate2.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN,
            rate3.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN,
        )
        if (!rates.rate1.isFinite() || rates.rate1 <= 0 ||
            !rates.rate2.isFinite() || rates.rate2 <= 0 ||
            !rates.rate3.isFinite() || rates.rate3 <= 0
        ) {
            Toast.makeText(this, R.string.invalid_rates, Toast.LENGTH_SHORT).show()
            return
        }
        repository.save(rates)
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun editableValue(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    companion object {
        const val PREFERENCES_NAME = "rates"
    }
}
