package com.bakerylabel.pro.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy")

    fun displayName(pattern: String) = pattern.uppercase(Locale.US)

    fun format(epochDay: Long, pattern: String): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(pattern, Locale.US))

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Rule strings: "D:n" adds n days, "M:n" adds n months. */
    fun applyRule(mfg: Long, rule: String): Long? {
        val parts = rule.split(":")
        if (parts.size != 2) return null
        val n = parts[1].toLongOrNull() ?: return null
        val d = LocalDate.ofEpochDay(mfg)
        return when (parts[0]) {
            "D" -> d.plusDays(n)
            "W" -> d.plusWeeks(n)
            "M" -> d.plusMonths(n)
            "Y" -> d.plusYears(n)
            else -> return null
        }.toEpochDay()
    }

    fun ruleLabel(rule: String): String {
        val p = rule.split(":")
        if (p.size != 2) return rule
        val n = p[1]
        return when (p[0]) {
            "D" -> "$n day" + if (n == "1") "" else "s"
            "W" -> "$n week" + if (n == "1") "" else "s"
            "M" -> "$n month" + if (n == "1") "" else "s"
            "Y" -> "$n year" + if (n == "1") "" else "s"
            else -> rule
        }
    }

    /** Quick expiry options from the spec. */
    val quickRules = listOf("D:1", "D:2", "D:3", "D:5", "D:7", "D:10", "D:15", "D:30", "M:3", "M:6")

    /** Epoch day -> UTC millis for the Material date picker, and back. */
    fun toPickerMillis(epochDay: Long): Long = epochDay * 86_400_000L
    fun fromPickerMillis(ms: Long): Long = Math.floorDiv(ms, 86_400_000L)
}
