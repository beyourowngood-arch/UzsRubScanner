package uz.rub.scanner

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var cropImage: CropImageView
    private lateinit var hint: TextView
    private lateinit var results: View
    private lateinit var uzsResult: TextView
    private lateinit var rubOneResult: TextView
    private lateinit var rubTwoResult: TextView
    private lateinit var ratePreferences: RatePreferences
    private var rates = ExchangeRates()
    private var lastAmount: Double? = null
    private var pendingCameraUri: Uri? = null
    private var imageGeneration = 0

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::showImage)
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCameraUri?.let(::showImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cropImage = findViewById(R.id.cropImage)
        hint = findViewById(R.id.hint)
        results = findViewById(R.id.results)
        uzsResult = findViewById(R.id.uzsResult)
        rubOneResult = findViewById(R.id.rubOneResult)
        rubTwoResult = findViewById(R.id.rubTwoResult)
        ratePreferences = RatePreferences(this)
        rates = ratePreferences.load()

        cropImage.onSelectionFinished = ::recognizeSelection
        findViewById<View>(R.id.galleryButton).setOnClickListener {
            prepareForNewImage()
            pickPhoto.launch("image/*")
        }
        findViewById<View>(R.id.cameraButton).setOnClickListener {
            prepareForNewImage()
            openCamera()
        }
        findViewById<View>(R.id.settingsButton).setOnClickListener { showRateSettings() }
    }

    private fun openCamera() {
        runCatching {
            val directory = File(cacheDir, "photos").apply { mkdirs() }
            val file = File.createTempFile("price_", ".jpg", directory)
            FileProvider.getUriForFile(this, "$packageName.files", file)
        }.onSuccess { uri ->
            pendingCameraUri = uri
            takePhoto.launch(uri)
        }.onFailure {
            Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImage(uri: Uri) {
        runCatching { decodeBitmap(uri) }
            .onSuccess { bitmap ->
                cropImage.setImage(bitmap)
                hint.setText(R.string.hint_crop)
                results.visibility = View.GONE
            }
            .onFailure { Toast.makeText(this, R.string.image_error, Toast.LENGTH_SHORT).show() }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > 2400) {
                    val ratio = 2400f / longest
                    decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
                }
                decoder.isMutableRequired = false
            }
        }
        return contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream))
        }
    }

    private fun recognizeSelection() {
        val selected = cropImage.croppedBitmap()
        if (selected == null) {
            Toast.makeText(this, R.string.select_area, Toast.LENGTH_SHORT).show()
            return
        }
        val requestedGeneration = imageGeneration
        hint.setText(R.string.recognizing)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(selected, 0))
            .addOnSuccessListener { text ->
                val amount = PriceCalculator.extractAmount(text.text)
                if (amount == null) {
                    Toast.makeText(this, R.string.no_digits, Toast.LENGTH_LONG).show()
                } else {
                    showResult(amount)
                }
            }
            .addOnFailureListener {
                if (requestedGeneration == imageGeneration) {
                    Toast.makeText(this, R.string.no_digits, Toast.LENGTH_LONG).show()
                }
            }
            .addOnCompleteListener {
                selected.recycle()
                recognizer.close()
                if (requestedGeneration == imageGeneration) hint.setText(R.string.hint_crop)
            }
    }

    private fun showResult(uzs: Double) {
        lastAmount = uzs
        val locale = Locale("ru", "RU")
        val uzsFormat = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        val rubFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val converted = PriceCalculator.convert(uzs, rates)
        uzsResult.text = getString(R.string.result_uzs, uzsFormat.format(uzs))
        rubOneResult.text = getString(
            R.string.result_rate_one,
            uzsFormat.format(rates.marketUzsPerRub),
            rubFormat.format(converted.directRub),
        )
        rubTwoResult.text = getString(
            R.string.result_rate_two,
            uzsFormat.format(rates.officialUzsPerUsd),
            rubFormat.format(rates.rubPerUsd),
            rubFormat.format(converted.crossRateRub),
        )
        results.visibility = View.VISIBLE
    }

    private fun showRateSettings() {
        val content = layoutInflater.inflate(R.layout.dialog_rates, null)
        val marketInput = content.findViewById<TextInputEditText>(R.id.marketRateInput)
        val uzsUsdInput = content.findViewById<TextInputEditText>(R.id.uzsUsdRateInput)
        val rubUsdInput = content.findViewById<TextInputEditText>(R.id.rubUsdRateInput)
        marketInput.setText(rates.marketUzsPerRub.toEditableRate())
        uzsUsdInput.setText(rates.officialUzsPerUsd.toEditableRate())
        rubUsdInput.setText(rates.rubPerUsd.toEditableRate())

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setMessage(R.string.settings_description)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val updated = ExchangeRates(
                    marketUzsPerRub = marketInput.decimalValue(),
                    officialUzsPerUsd = uzsUsdInput.decimalValue(),
                    rubPerUsd = rubUsdInput.decimalValue(),
                )
                if (!updated.isValid()) {
                    Toast.makeText(this, R.string.invalid_rates, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                rates = updated
                ratePreferences.save(updated)
                lastAmount?.let(::showResult)
                Toast.makeText(this, R.string.rates_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun TextInputEditText.decimalValue(): Double = text?.toString()
        ?.trim()
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: Double.NaN

    private fun Double.toEditableRate(): String = if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}
