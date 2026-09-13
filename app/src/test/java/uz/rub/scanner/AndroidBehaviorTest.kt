package uz.rub.scanner

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidBehaviorTest {
    @Test
    fun `all three rates are saved and restored`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferences = context.getSharedPreferences("rate_test", android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val subject = RatePreferences(preferences)

        assertEquals(ExchangeRates(), subject.load())
        val changed = ExchangeRates(105.5, 12_300.25, 91.75)
        subject.save(changed)

        assertEquals(changed, RatePreferences(preferences).load())
    }

    @Test
    fun `finishing a valid selection requests automatic recognition`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = CropImageView(context)
        view.layout(0, 0, 400, 300)
        view.setImage(Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888))
        var recognitionRequested = false
        view.onSelectionFinished = { recognitionRequested = true }

        view.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 60f, 0))
        view.onTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 250f, 180f, 0))

        assertTrue(recognitionRequested)
    }
}
