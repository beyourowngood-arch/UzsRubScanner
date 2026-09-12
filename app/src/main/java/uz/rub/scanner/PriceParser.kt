package uz.rub.scanner

object PriceParser {
    private val candidatePattern = Regex("[0-9](?:[0-9\\p{Zs}\\t.,'’]*[0-9])?")
    private val groupingCharacters = setOf(' ', '\t', '\u00A0', '\u202F', '\'', '’')

    /** Extracts the most likely price from all text returned by OCR. */
    fun parseRecognizedText(text: String): Double? = candidatePattern.findAll(text)
        .map { match -> match.value to match.value.count(Char::isDigit) }
        .mapNotNull { (candidate, digitCount) -> parsePrice(candidate)?.let { it to digitCount } }
        .maxByOrNull { (_, digitCount) -> digitCount }
        ?.first

    /**
     * Parses a localized price. Spaces and apostrophes group thousands. When both `.` and `,`
     * occur, the last one is decimal; with only one kind, it is decimal only for a 1–2 digit
     * suffix. All earlier punctuation groups thousands.
     */
    fun parsePrice(raw: String): Double? {
        val compact = raw.filterNot(groupingCharacters::contains)
        if (compact.isEmpty() || compact.any { !it.isDigit() && it != '.' && it != ',' }) return null

        val lastDot = compact.lastIndexOf('.')
        val lastComma = compact.lastIndexOf(',')
        val decimalIndex = when {
            lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
            else -> maxOf(lastDot, lastComma).takeIf { index ->
                index >= 0 && compact.length - index - 1 in 1..2 &&
                    compact.substring(index + 1).all(Char::isDigit)
            }
        }

        val normalized = buildString(compact.length) {
            compact.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    index == decimalIndex -> append('.')
                }
            }
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
