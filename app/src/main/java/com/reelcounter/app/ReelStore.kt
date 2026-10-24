package com.reelcounter.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Saari counting phone ke andar SharedPreferences main save hoti hai. */
class ReelStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("reel_stats", Context.MODE_PRIVATE)

    /** offset 0 = aaj, 1 = kal, ... */
    fun dayKey(offset: Int = 0): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    /** Ek reel count karo. Aaj ka naya total wapas deta hai. */
    @Synchronized
    fun increment(appName: String): Int {
        val d = dayKey()
        val total = prefs.getInt("t_$d", 0) + 1
        val perApp = prefs.getInt("a_${d}_$appName", 0) + 1
        prefs.edit()
            .putInt("t_$d", total)
            .putInt("a_${d}_$appName", perApp)
            .apply()
        return total
    }

    fun today(): Int = day(0)

    fun day(offset: Int): Int = prefs.getInt("t_${dayKey(offset)}", 0)

    /** Aaj ki reels app ke hisaab se, zyada se kam. */
    fun perAppToday(): List<Pair<String, Int>> {
        val prefix = "a_${dayKey()}_"
        return prefs.all
            .filterKeys { it.startsWith(prefix) }
            .map { it.key.removePrefix(prefix) to (it.value as Int) }
            .sortedByDescending { it.second }
    }

    /** Date -> count, sab dinon ka. */
    fun allDays(): Map<String, Int> = prefs.all
        .filterKeys { it.startsWith("t_") }
        .mapKeys { it.key.removePrefix("t_") }
        .mapValues { it.value as Int }

    fun allTime(): Int = allDays().values.sum()

    fun bestDay(): Pair<String, Int>? = allDays().maxByOrNull { it.value }?.toPair()

    /** Kitne dinon se lagatar reels dekh rahe ho (aaj ya kal se shuru). */
    fun streak(): Int {
        var offset = if (day(0) > 0) 0 else 1
        var s = 0
        while (day(offset) > 0) {
            s++
            offset++
        }
        return s
    }
}
