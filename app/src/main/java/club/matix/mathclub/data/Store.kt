package club.matix.mathclub.data

import android.content.Context
import android.content.SharedPreferences

/** Replaces the web app's localStorage. */
class Store(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("matix", Context.MODE_PRIVATE)

    var user: String?
        get() = p.getString("user", null)
        set(v) = p.edit().putString("user", v).apply()
    var dark: Boolean
        get() = p.getBoolean("dark", false)
        set(v) = p.edit().putBoolean("dark", v).apply()
    var aiKey: String
        get() = p.getString("ai_key", "") ?: ""
        set(v) = p.edit().putString("ai_key", v.trim()).apply()
    var seenWelcome: Boolean
        get() = p.getBoolean("seen_welcome", false)
        set(v) = p.edit().putBoolean("seen_welcome", v).apply()
}
