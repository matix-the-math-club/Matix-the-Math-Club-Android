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
    var reduceMotion: Boolean
        get() = p.getBoolean("reduce_motion", false)
        set(v) = p.edit().putBoolean("reduce_motion", v).apply()
    var quietMode: Boolean
        get() = p.getBoolean("quiet_mode", false)
        set(v) = p.edit().putBoolean("quiet_mode", v).apply()
    var brightness: Int
        get() = p.getInt("brightness", 100).coerceIn(60, 120)
        set(v) = p.edit().putInt("brightness", v.coerceIn(60, 120)).apply()
    var language: String
        get() = p.getString("language", "en") ?: "en"
        set(v) = p.edit().putString("language", v).apply()
    var learnServerUrl: String
        get() = p.getString("learn_server_url", "") ?: ""
        set(v) = p.edit().putString("learn_server_url", v.trim().trimEnd('/')).apply()
    fun sessionRevision(user: String) = p.getLong("session_revision_$user", 0L)
    fun setSessionRevision(user: String, value: Long) =
        p.edit().putLong("session_revision_$user", value).apply()

    fun recentLearnTopics(): List<String> = runCatching {
        org.json.JSONArray(p.getString("learn_recent", "[]")).let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter(String::isNotBlank).take(12)
        }
    }.getOrDefault(emptyList())

    fun rememberLearnTopic(topic: String) {
        val items = (listOf(topic.trim()) + recentLearnTopics().filterNot { it.equals(topic.trim(), true) }).take(12)
        p.edit().putString("learn_recent", org.json.JSONArray(items).toString()).apply()
    }

    fun mathLearnProfile(username: String): org.json.JSONObject = runCatching {
        org.json.JSONObject(p.getString("ml5_${Auth.normalize(username)}", "{}") ?: "{}")
    }.getOrDefault(org.json.JSONObject())

    fun saveMathLearnProfile(username: String, state: org.json.JSONObject) {
        p.edit().putString("ml5_${Auth.normalize(username)}", state.toString()).apply()
    }

    fun clearMathLearnProfile(username: String) {
        p.edit().remove("ml5_${Auth.normalize(username)}").apply()
    }

    fun seenAt(user: String): Long = p.getLong("msg_seen_$user", 0L)
    fun setSeenAt(user: String, t: Long) = p.edit().putLong("msg_seen_$user", t).apply()

    fun notifEnabled(user: String, type: String): Boolean = p.getBoolean("notif_${user}_$type", true)
    fun setNotifEnabled(user: String, type: String, on: Boolean) = p.edit().putBoolean("notif_${user}_$type", on).apply()
}
