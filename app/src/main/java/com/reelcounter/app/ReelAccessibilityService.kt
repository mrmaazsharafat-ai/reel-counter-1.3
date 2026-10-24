package com.reelcounter.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

/**
 * Reels apps main scroll pakadta hai, count barhata hai aur
 * screen ke upar-beech main chota sa counter dikhata hai.
 */
class ReelAccessibilityService : AccessibilityService() {

    companion object {
        // package -> dikhane wala naam
        private val TARGETS = mapOf(
            "com.instagram.android" to "Instagram",
            "com.facebook.katana" to "Facebook",
            "com.google.android.youtube" to "YouTube Shorts",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.ss.android.ugc.trill" to "TikTok"
        )

        // In apps ka poora feed hi reels hota hai, screen check ki zaroorat nahi
        private val ALWAYS_REELS = setOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
        )

        private const val MIN_GAP_MS = 800L      // 2 scroll events ke beech kam az kam gap
        private const val SCREEN_CACHE_MS = 1500L
        private const val MAX_NODES = 600
    }

    private lateinit var store: ReelStore
    private lateinit var wm: WindowManager
    private var pill: TextView? = null

    private var lastCountAt = 0L
    private var screenCheckAt = 0L
    private var screenIsReels = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = ReelStore(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buildPill()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when {
                    pkg in TARGETS -> showPill()
                    pkg.startsWith("com.android.systemui") -> Unit // notification shade, ignore
                    else -> hidePill()
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val appName = TARGETS[pkg] ?: return
                val now = System.currentTimeMillis()
                if (now - lastCountAt < MIN_GAP_MS) return
                if (!isReelsScreen(pkg)) return

                lastCountAt = now
                store.increment(appName)
                refreshPill()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pill?.let { runCatching { wm.removeView(it) } }
        pill = null
        super.onDestroy()
    }

    // ---------- reels screen detect ----------

    private fun isReelsScreen(pkg: String): Boolean {
        if (pkg in ALWAYS_REELS) return true
        val now = System.currentTimeMillis()
        if (now - screenCheckAt < SCREEN_CACHE_MS) return screenIsReels
        screenCheckAt = now
        screenIsReels = scanForReels(rootInActiveWindow)
        return screenIsReels
    }

    /** View ids main "reel", "clips" ya "shorts" dhoondta hai. */
    @Suppress("DEPRECATION")
    private fun scanForReels(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        var found = false
        while (queue.isNotEmpty() && visited < MAX_NODES && !found) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null && (id.contains("reel", true) ||
                        id.contains("clips", true) ||
                        id.contains("shorts", true))
            ) {
                found = true
            } else {
                for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
            }
            if (Build.VERSION.SDK_INT < 33) node.recycle()
        }
        if (Build.VERSION.SDK_INT < 33) queue.forEach { it.recycle() }
        return found
    }

    // ---------- floating counter ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildPill() {
        val tv = TextView(this).apply {
            text = "🎬 ${store.today()}"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(0xCC000000.toInt())
            }
            visibility = View.GONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(36) // upar se doori, zaroorat ho to kam/zyada karo
        }
        wm.addView(tv, lp)
        pill = tv
    }

    private fun refreshPill() {
        pill?.text = "🎬 ${store.today()}"
    }

    private fun showPill() {
        refreshPill()
        pill?.visibility = View.VISIBLE
    }

    private fun hidePill() {
        pill?.visibility = View.GONE
    }
}
