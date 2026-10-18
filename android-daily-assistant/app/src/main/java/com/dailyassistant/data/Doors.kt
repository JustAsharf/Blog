package com.dailyassistant.data

/**
 * الأبواب الأربعة — the four doors the whole app is organised around.
 * Door 1 is the only one with money in it; the other three are work, plans and the record.
 */
enum class Door(val number: Int, val arabicTitle: String, val arabicShort: String) {
    MONEY(1, "الباب الأول — الجانب المادي", "المادي"),
    WORKS(2, "الباب الثاني — مجموع الأعمال", "الأعمال"),
    PROJECTS(3, "الباب الثالث — المشاريع المقترحة", "المشاريع"),
    RECORD(4, "الباب الرابع — التوثيق الشامل", "التوثيق")
}

/** A spending group inside الإنفاق الشهري, with its detailed subcategories. */
data class SpendGroup(
    val id: String,
    val arabicName: String,
    val emoji: String,
    val subs: List<String>,
    /** المحروقات sits outside الإنفاق الشهري, as the plan requires. */
    val insideMonthly: Boolean = true,
    /** تكاليف المشاريع is keyed by project name rather than a fixed list. */
    val byName: Boolean = false
)

object Doors {

    const val GROUP_PROJECTS = "projects"
    const val GROUP_EMERGENCY = "tawari"
    const val GROUP_FUEL = "wuqud"

    /** الإنفاق الشهري — the monthly spending tree. */
    val monthly: List<SpendGroup> = listOf(
        SpendGroup("tariya", "الطرية", "🥬", listOf("خضروات", "فواكه", "عصائر", "ألبان")),
        SpendGroup("jaffa", "الجافة", "🍚", listOf("رز", "عدس", "معجون", "تنظيفات", "مواد أخرى")),
        SpendGroup("luhum", "اللحوم", "🍖", listOf("دجاج", "لحم", "لحم مفروم")),
        SpendGroup("khubz", "الصمون والخبز", "🥖", listOf("صمون", "خبز")),
        SpendGroup("ilaj", "العلاجات ومراجعات الأطباء", "💊", listOf("أدوية", "مراجعة طبيب", "تحاليل")),
        SpendGroup("masarif", "المصروفات المخصصة", "👛", listOf("مصرف شخصي", "مصرف الزوجة", "مصرف الأولاد", "مصرف السيارة")),
        SpendGroup("ghaz", "غاز أو نفط", "🔥", listOf("غاز", "نفط")),
        SpendGroup("bina", "التصليحات والبناء", "🧱", listOf("تصليحات", "بناء", "مواد بناء"))
    )

    /** Buckets that are counted in the grand total but not inside الإنفاق الشهري. */
    val outside: List<SpendGroup> = listOf(
        SpendGroup(GROUP_PROJECTS, "تكاليف المشاريع", "🏗️", emptyList(), insideMonthly = false, byName = true),
        SpendGroup(GROUP_EMERGENCY, "الطوارئ", "🚨", listOf("طوارئ"), insideMonthly = false),
        SpendGroup(GROUP_FUEL, "المحروقات", "⛽", listOf("بنزين", "ديزل", "كاز"), insideMonthly = false)
    )

    val all: List<SpendGroup> = monthly + outside

    fun group(id: String): SpendGroup? = all.firstOrNull { it.id == id }

    /** أنواع المشاريع المقترحة — door 3. */
    val projectKinds = listOf("تصليح", "ترميم", "مراجعات رسمية")

    /** حالات المشروع، بالترتيب. */
    val projectStatuses = listOf("مقترح", "قيد التنفيذ", "منجز")

    /** الأعمال اليومية الافتراضية — door 2. */
    val dailyWorks = listOf("أذكار شخصية", "أذكار غير شخصية")

    /** الأعمال الدورية مع فترتها بالأيام. */
    val periodicWorks = listOf(
        "زيارة المراقد المقدسة" to 30,
        "زيارة الأهل" to 7,
        "زيارة الأقارب" to 14
    )
}
