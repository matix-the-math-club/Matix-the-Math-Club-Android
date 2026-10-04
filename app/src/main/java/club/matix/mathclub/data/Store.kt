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
    var themeId: String
        get() = p.getString("theme", "matix") ?: "matix"
        set(v) = p.edit().putString("theme", v).apply()

    fun seenAt(user: String): Long = p.getLong("msg_seen_$user", 0L)
    fun setSeenAt(user: String, t: Long) = p.edit().putLong("msg_seen_$user", t).apply()

    fun notifEnabled(user: String, type: String): Boolean = p.getBoolean("notif_${user}_$type", true)
    fun setNotifEnabled(user: String, type: String, on: Boolean) = p.edit().putBoolean("notif_${user}_$type", on).apply()
}
