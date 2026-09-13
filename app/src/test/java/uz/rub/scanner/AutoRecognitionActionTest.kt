package uz.rub.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoRecognitionActionTest {
    @Test
    fun `finishing a valid selection starts recognition automatically`() {
        var recognitionStarts = 0
        val action = AutoRecognitionAction { recognitionStarts++ }

        action.onSelectionFinished(hasSelection = true)

        assertEquals(1, recognitionStarts)
    }

    @Test
    fun `empty selection does not start recognition`() {
        var recognitionStarts = 0
        val action = AutoRecognitionAction { recognitionStarts++ }

        action.onSelectionFinished(hasSelection = false)

        assertEquals(0, recognitionStarts)
    }
}
