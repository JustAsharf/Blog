package com.dailyassistant.util

/** Text handling for Arabic input: digits, hamza forms, tatweel and diacritics. */
object ArabicText {

    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"
    private const val EASTERN_INDIC = "۰۱۲۳۴۵۶۷۸۹"

    /** Turns ٠١٢ and ۰۱۲ into 012 so every number path sees plain digits. */
    fun latinDigits(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            val arabic = ARABIC_INDIC.indexOf(ch)
            val eastern = EASTERN_INDIC.indexOf(ch)
            when {
                arabic >= 0 -> append(arabic)
                eastern >= 0 -> append(eastern)
                else -> append(ch)
            }
        }
    }

    /**
     * Folds the spellings people mix freely — أ/إ/آ، ى/ي، ة/ه — so that a keyword
     * written any common way still matches. Never shown to the user, only matched against.
     */
    fun normalise(input: String): String =
        latinDigits(input)
            .replace(Regex("[ً-ْـ]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("\\s+"), " ")
            .trim()
}
