package uz.rub.scanner

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
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var cropImage: CropImageView
    private lateinit var hint: TextView
    private lateinit var recognizeButton: MaterialButton
    private lateinit var results: View
    private lateinit var uzsResult: TextView
    private lateinit var rubOneResult: TextView
    private lateinit var rubTwoResult: TextView
    private var pendingCameraUri: Uri? = null

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
        recognizeButton = findViewById(R.id.recognizeButton)
        results = findViewById(R.id.results)
        uzsResult = findViewById(R.id.uzsResult)
        rubOneResult = findViewById(R.id.rubOneResult)
        rubTwoResult = findViewById(R.id.rubTwoResult)

        findViewById<View>(R.id.galleryButton).setOnClickListener { pickPhoto.launch("image/*") }
        findViewById<View>(R.id.cameraButton).setOnClickListener { openCamera() }
        recognizeButton.setOnClickListener { recognizeSelection() }
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
                recognizeButton.isEnabled = true
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
        recognizeButton.isEnabled = false
        recognizeButton.setText(R.string.recognizing)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(selected, 0))
            .addOnSuccessListener { text ->
                val amount = PriceParser.parseRecognizedText(text.text)
                if (amount == null) {
                    Toast.makeText(this, R.string.no_digits, Toast.LENGTH_LONG).show()
                } else {
                    showResult(amount)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.no_digits, Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                selected.recycle()
                recognizer.close()
                recognizeButton.isEnabled = true
                recognizeButton.setText(R.string.recognize)
            }
    }

    private fun showResult(uzs: Double) {
        val locale = Locale("ru", "RU")
        val uzsFormat = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        val rubFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        uzsResult.text = getString(R.string.result_uzs, uzsFormat.format(uzs))
        rubOneResult.text = getString(R.string.result_rate_one, rubFormat.format(uzs / 110.0))
        rubTwoResult.text = getString(R.string.result_rate_two, rubFormat.format(uzs / 11750.0 * 86.0))
        results.visibility = View.VISIBLE
    }
}
