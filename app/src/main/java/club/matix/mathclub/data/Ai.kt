package club.matix.mathclub.data

import club.matix.mathclub.ai.MatixAi
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class ChatMsg(val fromUser: Boolean, val text: String)

/** Port of the web app's AI provider detection and chat call. */
object Ai {
    private const val SYSTEM =
        "You are Matix AI, the maths tutor built into the Matix the Math Club app. " +
            "Explain from zero, step by step, be friendly and concise, and check understanding with a short question."

    fun provider(key: String): String = when {
        key.startsWith("AIza") || key.startsWith("AQ.") -> "gemini"
        key.startsWith("gsk_") -> "groq"
        key.startsWith("sk-or-") -> "openrouter"
        key.startsWith("sk-") -> "openai"
        key.startsWith("ghp_") || key.startsWith("github_pat_") || key.startsWith("gho_") -> "github"
        else -> "groq"
    }

    private fun model(p: String) = when (p) {
        "gemini" -> "gemini-2.0-flash"
        "openrouter" -> "meta-llama/llama-3.3-70b-instruct"
        "openai", "github" -> "gpt-4o-mini"
        else -> "llama-3.3-70b-versatile"
    }

    /** Never throws; returns an error string prefixed with "⚠" on failure. */
    fun reply(key: String, history: List<ChatMsg>): String {
        if (key.isBlank()) return offline(history.lastOrNull()?.text ?: "")
        return complete(key, SYSTEM, history)
    }

    /** Same as [reply] but with a custom system prompt. Needs a key. */
    fun complete(key: String, system: String, history: List<ChatMsg>): String {
        if (key.isBlank()) return "⚠ No AI key set. Add one in Settings."
        return try {
            val p = provider(key)
            if (p == "gemini") gemini(key, system, history) else openAiStyle(p, key, system, history)
        } catch (e: Exception) {
            "⚠ Request failed: ${e.message ?: "network error"}"
        }
    }

    /** Offline fallback using the local MatixAi engine: math expressions and the clock. */
    fun offline(q: String): String {
        val t = q.trim()
        if (t.contains("time", true) || t.contains("clock", true)) return MatixAi.getClock()
        MatixAi.parseMath(t)?.let { return "= " + (if (it % 1.0 == 0.0 && kotlin.math.abs(it) < 1e15) it.toLong().toString() else it.toString()) }
        return "I can calculate expressions (try 2+3*4) offline. Add an AI key in Settings to chat with the full Matix AI tutor."
    }

    private fun gemini(key: String, system: String, h: List<ChatMsg>): String {
        val contents = JSONArray()
        h.forEach {
            contents.put(
                JSONObject().put("role", if (it.fromUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", it.text)))
            )
        }
        val body = JSONObject().put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model("gemini")}:generateContent?key=${Firebase.enc(key)}"
        val j = JSONObject(post(url, body.toString(), null))
        j.optJSONObject("error")?.let { return "⚠ ${it.optString("message")}" }
        return j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "⚠ Empty response"
    }

    private fun openAiStyle(p: String, key: String, system: String, h: List<ChatMsg>): String {
        val url = when (p) {
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "github" -> "https://models.inference.ai.azure.com/chat/completions"
            else -> "https://api.groq.com/openai/v1/chat/completions"
        }
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        h.forEach { msgs.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text)) }
        val body = JSONObject().put("model", model(p)).put("messages", msgs).put("temperature", 0.4).put("max_tokens", 2048)
        val j = JSONObject(post(url, body.toString(), key))
        j.optJSONObject("error")?.let { return "⚠ ${it.optString("message")}" }
        return j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "⚠ Empty response"
    }

    /** Free keyless HTML generator the web app used for "Make with AI". Returns null if busy. */
    fun generateGameHtml(prompt: String): String? {
        val sys = "You are Matix AI, a world-class creative coder. Output ONLY one complete self-contained single-file HTML5 document using inline CSS and JavaScript. No markdown, no code fences, no explanations. Make it beautiful, responsive and fully working with vanilla HTML/CSS/JS only. If it is a game, track and show the score."
        repeat(2) { attempt ->
            try {
                val body = JSONObject().put("model", "openai").put("referrer", "matixmathclub").put(
                    "messages", JSONArray().put(JSONObject().put("role", "system").put("content", sys))
                        .put(JSONObject().put("role", "user").put("content", "Make this: $prompt"))
                )
                val j = JSONObject(post("https://text.pollinations.ai/openai", body.toString(), null))
                val t = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                val h = cleanHtml(t)
                if (h.length > 120 && Regex("<html|<!DOCTYPE|<body|<canvas", RegexOption.IGNORE_CASE).containsMatchIn(h)) return stamp(h)
            } catch (e: Exception) { }
            if (attempt == 0) Thread.sleep(900)
        }
        return null
    }

    fun cleanHtml(raw: String): String {
        var t = raw.trim()
        Regex("```(?:html)?([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(t)?.let { t = it.groupValues[1].trim() }
        val i = Regex("<!DOCTYPE|<html", RegexOption.IGNORE_CASE).find(t)?.range?.first ?: 0
        return if (i > 0) t.substring(i) else t
    }

    private fun stamp(html: String): String {
        val badge = "<div style=\"position:fixed;left:0;right:0;bottom:0;text-align:center;font:700 12px system-ui,sans-serif;color:#fff;background:rgba(91,70,201,.94);padding:7px;z-index:2147483647\">✨ by Matix AI</div>"
        return if (Regex("</body>", RegexOption.IGNORE_CASE).containsMatchIn(html)) html.replace(Regex("</body>", RegexOption.IGNORE_CASE), badge + "</body>") else html + badge
    }

    fun titleOf(html: String): String =
        Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.trim() ?: ""

    private fun post(url: String, body: String, bearer: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 60_000
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer)
        c.outputStream.use { it.write(body.toByteArray()) }
        val s = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val t = s.bufferedReader().use { it.readText() }
        c.disconnect()
        return t
    }
}
