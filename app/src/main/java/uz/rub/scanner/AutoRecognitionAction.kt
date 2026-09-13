package uz.rub.scanner

class AutoRecognitionAction(private val recognize: () -> Unit) {
    fun onSelectionFinished(hasSelection: Boolean) {
        if (hasSelection) recognize()
    }
}
