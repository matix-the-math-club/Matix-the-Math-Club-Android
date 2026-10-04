package club.matix.mathclub.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONTokener

/** Minimal Firebase Realtime Database REST client (same endpoints the web app used). */
object Firebase {
    const val MAIN = "https://matix-1d538-default-rtdb.firebaseio.com"
    const val IDEAS = "https://matix-the-math-club-views-default-rtdb.firebaseio.com"

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Returns JSONObject, JSONArray, String, Number, Boolean or null. */
    fun get(path: String, base: String = MAIN): Any? {
        val raw = request("GET", base + path + ".json", null) ?: return null
        val v = JSONTokener(raw).nextValue()
        return if (v == JSONObject.NULL) null else v
    }

    /** Enumerates database child names without downloading member values such as plaintext passwords. */
    fun getKeys(path: String, base: String = MAIN): Set<String> {
        val raw = request("GET", "$base$path.json?shallow=true", null) ?: return emptySet()
        val value = JSONTokener(raw).nextValue()
        return (value as? JSONObject)?.keys()?.asSequence()?.toSet() ?: emptySet()
    }

    fun put(path: String, body: Any?, base: String = MAIN): Boolean =
        request("PUT", base + path + ".json", JSONObject.wrap(body)?.toString() ?: "null") != null

    fun patch(path: String, body: JSONObject, base: String = MAIN): Boolean =
        request("PATCH", base + path + ".json", body.toString()) != null

    fun post(path: String, body: JSONObject, base: String = MAIN): Boolean =
        request("POST", base + path + ".json", body.toString()) != null

    fun delete(path: String, base: String = MAIN): Boolean =
        request("DELETE", base + path + ".json", null) != null

    private fun request(method: String, url: String, body: String?): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.requestMethod = method
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val ok = c.responseCode in 200..299
        val text = (if (ok) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }
        c.disconnect()
        if (ok) text ?: "null" else null
    } catch (e: Exception) {
        null
    }
}
