package com.example.domain.engine

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date and time utilities strictly pinned to Asia/Kolkata (IST) timezone.
 * Compatible with Android minSdk 24+ and Robolectric JVM environments.
 */
object IstDateUtils {
    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private const val ISO_PATTERN = "yyyy-MM-dd"
    private const val DISPLAY_PATTERN = "dd MMM yyyy"
    private const val SHORT_PATTERN = "dd MMM"
    private const val MONTH_YEAR_PATTERN = "MMMM yyyy"

    private fun newCalendar(): Calendar = Calendar.getInstance(IST_TIMEZONE, Locale.US)

    private fun parseToCalendar(isoDate: String): Calendar? {
        if (isoDate.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
                timeZone = IST_TIMEZONE
                isLenient = false
            }
            val parsed = sdf.parse(isoDate.trim()) ?: return null
            newCalendar().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatCalendar(cal: Calendar): String {
        val sdf = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
        return sdf.format(cal.time)
    }

    fun todayIst(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = newCalendar().apply { timeInMillis = nowMillis }
        return formatCalendar(cal)
    }

    fun addDays(isoDate: String, days: Int): String {
        val cal = parseToCalendar(isoDate) ?: return todayIst()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return formatCalendar(cal)
    }

    fun daysBetween(fromIso: String, toIso: String): Int {
        val c1 = parseToCalendar(fromIso) ?: return 0
        val c2 = parseToCalendar(toIso) ?: return 0
        val diffMillis = c2.timeInMillis - c1.timeInMillis
        return (diffMillis / (24L * 60L * 60L * 1000L)).toInt()
    }

    fun formatDisplay(isoDate: String): String {
        val cal = parseToCalendar(isoDate) ?: return "Not scheduled"
        val sdf = SimpleDateFormat(DISPLAY_PATTERN, Locale.US).apply { timeZone = IST_TIMEZONE }
        return sdf.format(cal.time)
    }

    fun formatShort(isoDate: String): String {
        val cal = parseToCalendar(isoDate) ?: return "—"
        val sdf = SimpleDateFormat(SHORT_PATTERN, Locale.US).apply { timeZone = IST_TIMEZONE }
        return sdf.format(cal.time)
    }

    fun monthYearLabel(year: Int, monthZeroIndexed: Int): String {
        val cal = newCalendar().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat(MONTH_YEAR_PATTERN, Locale.US).apply { timeZone = IST_TIMEZONE }
        return sdf.format(cal.time)
    }

    fun currentYearMonth(todayIso: String = todayIst()): Pair<Int, Int> {
        val cal = parseToCalendar(todayIso) ?: newCalendar()
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun getMonthGridDates(year: Int, monthZeroIndexed: Int): List<String?> {
        val cal = newCalendar().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1 .. Sat=7
        val leadingBlanks = (firstDayOfWeek - Calendar.MONDAY + 7) % 7 // Monday-first grid
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = MutableList<String?>(leadingBlanks) { null }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            result.add(formatCalendar(cal))
        }
        while (result.size % 7 != 0) {
            result.add(null)
        }
        return result
    }

    fun getWeekDates(anchorIso: String = todayIst()): List<String> {
        val cal = parseToCalendar(anchorIso) ?: newCalendar()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offsetToMonday = (dow - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offsetToMonday)
        return (0 until 7).map { i ->
            val d = formatCalendar(cal)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            d
        }
    }

    fun dayOfMonth(isoDate: String): Int {
        val cal = parseToCalendar(isoDate) ?: return 1
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun dayShortName(isoDate: String): String {
        val cal = parseToCalendar(isoDate) ?: return ""
        val sdf = SimpleDateFormat("EEE", Locale.US).apply { timeZone = IST_TIMEZONE }
        return sdf.format(cal.time)
    }

    fun isSunday(isoDate: String = todayIst()): Boolean {
        val cal = parseToCalendar(isoDate) ?: return false
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    fun currentIstHourMinute(): Pair<Int, Int> {
        val cal = newCalendar()
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    fun formatDateTimeForExport(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'IST'", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
        return sdf.format(date)
    }
}
