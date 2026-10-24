package com.reelcounter.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private companion object {
        const val BG = 0xFF0F0F14.toInt()
        const val CARD = 0xFF1A1A22.toInt()
        const val ACCENT = 0xFFFF3B6B.toInt()
        const val MUTED = 0xFF9A9AA8.toInt()
        const val SECONDS_PER_REEL = 30 // andaaza
    }

    private lateinit var store: ReelStore
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ReelStore(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(36), dp(20), dp(32))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName) && enabled.contains("ReelAccessibilityService")
    }

    private fun render() {
        root.removeAllViews()

        // ---- Title ----
        root.addView(text("Reel Counter", 14f, MUTED, bold = true))

        // ---- Aaj ka bara number ----
        val today = store.today()
        root.addView(text(today.toString(), 84f, Color.WHITE, bold = true))
        root.addView(text("aaj ki reels", 16f, MUTED))
        root.addView(text("≈ ${formatMinutes(today)} dekhne mein laga (andaaza)", 13f, MUTED).apply {
            setPadding(0, dp(4), 0, dp(20))
        })

        // ---- Service band hai to warning ----
        if (!isServiceEnabled()) {
            root.addView(card().apply {
                addView(text("Counter abhi band hai", 16f, Color.WHITE, bold = true))
                addView(text(
                    "Settings → Accessibility → Reel Counter ko ON karo, tab hi reels count hongi.",
                    13f, MUTED
                ).apply { setPadding(0, dp(6), 0, dp(12)) })
                addView(Button(context).apply {
                    text = "Accessibility Settings kholo"
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                })
            })
        }

        // ---- 7 din ka chart ----
        root.addView(sectionTitle("Pichle 7 din"))
        root.addView(card().apply {
            val chart = BarChartView(context)
            chart.data = (6 downTo 0).map { off -> dayLabel(off) to store.day(off) }
            addView(chart)
        })

        // ---- Stats grid ----
        root.addView(sectionTitle("Stats"))
        val all = store.allTime()
        val best = store.bestDay()
        val activeDays = store.allDays().count { it.value > 0 }
        val avg = if (activeDays == 0) 0 else all / activeDays
        val week = (0..6).sumOf { store.day(it) }

        root.addView(statRow(
            "Is hafte" to week.toString(),
            "Sab waqt" to all.toString()
        ))
        root.addView(statRow(
            "Roz ka average" to avg.toString(),
            "Lagatar din" to "${store.streak()}"
        ))
        root.addView(statRow(
            "Sab se zyada din" to (best?.second?.toString() ?: "-"),
            "Wo din" to (best?.first ?: "-")
        ))

        // ---- App ke hisaab se ----
        val perApp = store.perAppToday()
        if (perApp.isNotEmpty()) {
            root.addView(sectionTitle("Aaj kis app main"))
            root.addView(card().apply {
                perApp.forEach { (app, n) ->
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(6), 0, dp(6))
                        addView(text(app, 15f, Color.WHITE).apply {
                            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                        })
                        addView(text(n.toString(), 15f, ACCENT, bold = true))
                    })
                }
            })
        }

        root.addView(text(
            "Saara data sirf aapke phone main hai. Koi internet ya account nahi chahiye.",
            12f, MUTED
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })
    }

    // ---------- chote helpers ----------

    private fun dayLabel(offset: Int): String {
        if (offset == 0) return "Aaj"
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -offset)
        return SimpleDateFormat("EEE", Locale.ENGLISH).format(c.time)
    }

    private fun formatMinutes(reels: Int): String {
        val min = reels * SECONDS_PER_REEL / 60
        return if (min >= 60) "${min / 60} ghanta ${min % 60} min" else "$min min"
    }

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun sectionTitle(s: String) = text(s, 13f, MUTED, bold = true).apply {
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(CARD)
        }
    }

    private fun statRow(left: Pair<String, String>, right: Pair<String, String>) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
            addView(statCell(left, rightGap = true))
            addView(statCell(right, rightGap = false))
        }

    private fun statCell(p: Pair<String, String>, rightGap: Boolean): View =
        card().apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (rightGap) marginEnd = dp(10)
            }
            addView(text(p.second, 22f, Color.WHITE, bold = true))
            addView(text(p.first, 12f, MUTED))
        }
}
