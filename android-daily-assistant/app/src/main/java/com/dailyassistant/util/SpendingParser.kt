package com.dailyassistant.util

import com.dailyassistant.data.Doors

/** One line of spending understood from ordinary Arabic. */
data class ParsedSpend(
    val groupId: String,
    val sub: String,
    val amountMinor: Long
)

data class ParseResult(
    val entries: List<ParsedSpend>,
    /** Amounts typed without a category in front of them. */
    val orphanAmounts: Int,
    val matchedWords: Int
) {
    val totalMinor: Long get() = entries.sumOf { it.amountMinor }
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * Reads a sentence the way it is spoken at home:
 *
 *   "مصروف ألبان بـ 10,000 ومصرف أولاد بـ 15,000 ودجاج 12 الف"
 *
 * Each keyword claims the first amount that follows it, up to the next keyword, so
 * the sentence never has to be split — and a "و" inside a word never breaks it.
 */
object SpendingParser {

    private data class Keyword(val word: String, val groupId: String, val sub: String)

    private val keywords: List<Keyword> = buildList {
        Doors.all.forEach { group ->
            group.subs.forEach { sub -> add(Keyword(ArabicText.normalise(sub), group.id, sub)) }
        }
        // How people actually say it, mapped onto the formal tree.
        listOf(
            Triple("لبن", "tariya", "ألبان"), Triple("حليب", "tariya", "ألبان"),
            Triple("جبن", "tariya", "ألبان"), Triple("لبنه", "tariya", "ألبان"),
            Triple("خضره", "tariya", "خضروات"), Triple("طماطم", "tariya", "خضروات"),
            Triple("فاكهه", "tariya", "فواكه"), Triple("عصير", "tariya", "عصائر"),
            Triple("دجاجه", "luhum", "دجاج"), Triple("مفروم", "luhum", "لحم مفروم"),
            Triple("لحمه", "luhum", "لحم"),
            Triple("دواء", "ilaj", "أدوية"), Triple("علاج", "ilaj", "أدوية"),
            Triple("طبيب", "ilaj", "مراجعة طبيب"), Triple("دكتور", "ilaj", "مراجعة طبيب"),
            Triple("تحليل", "ilaj", "تحاليل"),
            Triple("مصروفي", "masarif", "مصرف شخصي"), Triple("شخصي", "masarif", "مصرف شخصي"),
            Triple("الزوجه", "masarif", "مصرف الزوجة"), Triple("زوجتي", "masarif", "مصرف الزوجة"),
            Triple("الاولاد", "masarif", "مصرف الأولاد"), Triple("اولاد", "masarif", "مصرف الأولاد"),
            Triple("العيال", "masarif", "مصرف الأولاد"),
            Triple("السياره", "masarif", "مصرف السيارة"), Triple("سياره", "masarif", "مصرف السيارة"),
            Triple("تصليح", "bina", "تصليحات"), Triple("ترميم", "bina", "بناء"),
            Triple("بنزين", Doors.GROUP_FUEL, "بنزين"), Triple("ديزل", Doors.GROUP_FUEL, "ديزل"),
            Triple("كاز", Doors.GROUP_FUEL, "كاز"), Triple("محروقات", Doors.GROUP_FUEL, "بنزين"),
            Triple("وقود", Doors.GROUP_FUEL, "بنزين"),
            Triple("طوارئ", Doors.GROUP_EMERGENCY, "طوارئ"), Triple("طاري", Doors.GROUP_EMERGENCY, "طوارئ"),
            Triple("مفاجئ", Doors.GROUP_EMERGENCY, "طوارئ")
        ).forEach { (word, group, sub) -> add(Keyword(ArabicText.normalise(word), group, sub)) }
    }.sortedByDescending { it.word.length }

    private val amountPattern = Regex("""(\d[\d.,]*)\s*(الف|ك)?""")

    fun parse(input: String): ParseResult {
        val text = ArabicText.normalise(input)
        if (text.isEmpty()) return ParseResult(emptyList(), 0, 0)

        // Keyword hits, longest-first so "لحم مفروم" wins over "لحم".
        data class Hit(val at: Int, val length: Int, val groupId: String, val sub: String)
        val hits = ArrayList<Hit>()
        keywords.forEach { kw ->
            var index = text.indexOf(kw.word)
            while (index >= 0) {
                val overlaps = hits.any { index < it.at + it.length && index + kw.word.length > it.at }
                if (!overlaps) hits += Hit(index, kw.word.length, kw.groupId, kw.sub)
                index = text.indexOf(kw.word, index + kw.word.length)
            }
        }
        hits.sortBy { it.at }

        // Amounts: "10,000" and "10.000" are both ten thousand; "12 الف" is twelve thousand.
        val amounts = amountPattern.findAll(text).mapNotNull { match ->
            val digits = match.groupValues[1].replace(",", "").replace(".", "")
            val value = digits.toLongOrNull() ?: return@mapNotNull null
            val multiplier = if (match.groupValues[2].isNotEmpty()) 1000L else 1L
            match.range.first to value * multiplier
        }.toMutableList()

        val taken = BooleanArray(amounts.size)
        val entries = ArrayList<ParsedSpend>()
        hits.forEachIndexed { i, hit ->
            val stop = if (i + 1 < hits.size) hits[i + 1].at else Int.MAX_VALUE
            val slot = amounts.indexOfFirst { (at, _) -> at >= hit.at && at < stop }
                .takeIf { it >= 0 && !taken[it] }
            if (slot != null) {
                taken[slot] = true
                entries += ParsedSpend(hit.groupId, hit.sub, amounts[slot].second * 100)
            }
        }

        return ParseResult(
            entries = entries,
            orphanAmounts = taken.count { !it },
            matchedWords = hits.size
        )
    }
}
