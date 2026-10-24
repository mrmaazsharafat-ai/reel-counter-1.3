# Reel Counter (Android)

Instagram Reels, YouTube Shorts, Facebook Reels aur TikTok main aap kitni reels scroll karte ho, ye app count karti hai.
Reels dekhte waqt screen ke **upar-beech** ek chota counter (🎬 42) nazar aata hai. App kholo to stats milte hain.

## Chalane ka tareeqa

1. **Android Studio** (Koala ya naya) install karo.
2. `File → Open` se is `ReelCounter` folder ko kholo, Gradle sync hone do.
3. Phone par USB debugging ON karke **Run ▶** dabao (ya `Build → Build APK(s)`).
4. Phone main app kholo → **Accessibility Settings kholo** → *Reel Counter* ko ON karo.
5. Instagram wagairah main reels scroll karo, counter chalna shuru ho jayega.

> Android 13+ par sideload ki hui app ke liye accessibility option grey ho sakta hai.
> Fix: `Settings → Apps → Reel Counter → ⋮ (upar-right) → Allow restricted settings`, phir dobara ON karo.

## Ye kaise kaam karta hai

- `ReelAccessibilityService` scroll events sunti hai. Har 0.8 second main 1 se zyada reel count nahi hoti.
- Instagram / YouTube / Facebook main pehle check hota hai ke screen par reels/shorts ka view hai, taake normal feed scroll count na ho. TikTok main har scroll count hota hai.
- Counter `TYPE_ACCESSIBILITY_OVERLAY` se bana hai, is liye alag "draw over apps" permission nahi chahiye.
- Data `SharedPreferences` main hai (sirf aapke phone par).

## Tuning

Ye andaaza lagane wala tareeqa hai, kyunke apps ka internal structure update ke saath badalta rehta hai.
- Count zyada/kam aaye: `ReelAccessibilityService.kt` main `MIN_GAP_MS` badlo.
- Reels screen detect na ho: `scanForReels` main keywords add karo.
- Counter ki position: `buildPill()` main `y = dp(36)`.
- Doosri app add karni ho: `TARGETS` map main package name daalo.
