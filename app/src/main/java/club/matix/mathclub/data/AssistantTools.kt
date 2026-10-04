package club.matix.mathclub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Native tool-calling assistant.  Tool arguments are always treated as untrusted,
 * owner-only tools are checked again at execution time, and public writes require
 * an explicit in-app confirmation.
 */
object AssistantTools {
    data class Approval(val title: String, val detail: String)

    private data class Tool(
        val name: String,
        val description: String,
        val fields: Map<String, String> = emptyMap(),
        val required: List<String> = emptyList()
    )

    private val tools = listOf(
        Tool("open_chat", "Open one-to-one Math Chat, optionally with a club member.", mapOf("person" to "Member username")),
        Tool("set_colors", "Change the app's colour theme.", mapOf("preset" to "Preset: matix, ocean, forest, sunset, grape, candy, mint, slate")),
        Tool("set_theme", "Switch between dark and light mode.", mapOf("mode" to "dark or light"), listOf("mode")),
        Tool("set_brightness", "Set in-app screen brightness from 60 to 120 percent.", mapOf("percent" to "Number from 60 to 120"), listOf("percent")),
        Tool("open_tab", "Open a page in Matix.", mapOf("page" to "home, games, ideas, learn, points, discussions, messages, users, bugs, private, app pages, settings, translator")),
        Tool("who_am_i", "Show the signed-in member's role and app settings."),
        Tool("search_web", "Search Wikipedia for a topic.", mapOf("query" to "Search query"), listOf("query")),
        Tool("list_games", "List published games in the Games hub."),
        Tool("get_points", "Look up member points, or all point totals.", mapOf("user" to "Optional username")),
        Tool("add_todo", "Add an item to the signed-in member's private to-do list.", mapOf("text" to "To-do text"), listOf("text")),
        Tool("list_todos", "List the signed-in member's private to-dos."),
        Tool("add_note", "Save a note in the signed-in member's private space.", mapOf("text" to "Note text"), listOf("text")),
        Tool("list_notes", "List notes in the signed-in member's private space."),
        Tool("add_game", "Publish a complete single-file HTML game to the shared Games hub.", mapOf("name" to "Game name", "html" to "Complete self-contained HTML document"), listOf("name", "html")),
        Tool("post_idea", "Post an idea to the shared Ideas board.", mapOf("text" to "Idea text"), listOf("text")),
        Tool("report_bug", "File a bug report on the shared Bugs board.", mapOf("title" to "Short title", "detail" to "Details"), listOf("title")),
        Tool("add_hint", "Add a home-page hint. Owner only.", mapOf("title" to "Hint title", "body" to "Hint text"), listOf("title", "body")),
        Tool("add_changelog", "Publish a changelog update. Owner only.", mapOf("title" to "Update title", "body" to "Update details"), listOf("title")),
        Tool("give_points", "Add or subtract points for a member. Owner only.", mapOf("user" to "Member username", "amount" to "Points to add (negative subtracts)"), listOf("user", "amount"))
    )

    private fun declarations(openAi: Boolean): JSONArray = JSONArray().also { out ->
        tools.forEach { t ->
            val props = JSONObject()
            t.fields.forEach { (name, description) ->
                props.put(name, JSONObject().put("type", if (name == "percent" || name == "amount") "number" else "string").put("description", description))
            }
            val parameters = JSONObject().put("type", "object").put("properties", props)
            if (t.required.isNotEmpty()) parameters.put("required", JSONArray(t.required))
            if (openAi) {
                out.put(JSONObject().put("type", "function").put("function",
                    JSONObject().put("name", t.name).put("description", t.description).put("parameters", parameters)))
            } else {
                val geminiProps = JSONObject()
                t.fields.forEach { (name, description) ->
                    geminiProps.put(name, JSONObject().put("type", if (name == "percent" || name == "amount") "NUMBER" else "STRING").put("description", description))
                }
                val geminiParams = JSONObject().put("type", "OBJECT").put("properties", geminiProps)
                if (t.required.isNotEmpty()) geminiParams.put("required", JSONArray(t.required))
                val decl = JSONObject().put("name", t.name).put("description", t.description)
                if (t.fields.isNotEmpty()) decl.put("parameters", geminiParams)
                out.put(decl)
            }
        }
    }

    private fun endpoint(provider: String): Pair<String, String> = when (provider) {
        "openai" -> "https://api.openai.com/v1/chat/completions" to "gpt-4o-mini"
        "openrouter" -> "https://openrouter.ai/api/v1/chat/completions" to "meta-llama/llama-3.3-70b-instruct"
        "github" -> "https://models.inference.ai.azure.com/chat/completions" to "gpt-4o-mini"
        else -> "https://api.groq.com/openai/v1/chat/completions" to "llama-3.3-70b-versatile"
    }

    suspend fun reply(
        key: String,
        user: String,
        isOwner: Boolean,
        dark: Boolean,
        theme: String,
        brightness: Int,
        history: List<ChatMsg>,
        store: Store,
        confirm: suspend (Approval) -> Boolean,
        onTheme: suspend (String, Boolean) -> Unit,
        onBrightness: suspend (Int) -> Unit,
        onNavigate: suspend (String, String?) -> Unit
    ): String {
        if (key.isBlank()) return Ai.offline(history.lastOrNull()?.text.orEmpty())
        return try {
            if (Ai.provider(key) == "gemini") {
                geminiReply(key, user, isOwner, dark, theme, brightness, history, store, confirm, onTheme, onBrightness, onNavigate)
            } else {
                openAiReply(key, user, isOwner, dark, theme, brightness, history, store, confirm, onTheme, onBrightness, onNavigate)
            }
        } catch (e: Exception) {
            "⚠ Matix AI couldn't finish that request: ${e.message ?: "network error"}"
        }
    }

    private fun system(user: String, owner: Boolean) =
        "You are Matix AI, the assistant built into the Matix the Math Club Android app. " +
            "The signed-in user is @$user${if (owner) " and is the owner" else " and is a member"}. " +
            "You can use the listed tools to operate the app. Use tools for app actions, don't claim success until a tool confirms it. " +
            "Owner-only actions are unavailable to members. Shared posts and game publishing show the user an approval prompt first. " +
            "Use search_web for current or uncertain facts. Only add a to-do or note when asked. For maths, explain clearly and check the answer. " +
            "Never expose passwords or secret keys. Be friendly and concise."

    private suspend fun openAiReply(
        key: String, user: String, owner: Boolean, dark: Boolean, theme: String, brightness: Int,
        history: List<ChatMsg>, store: Store, confirm: suspend (Approval) -> Boolean,
        onTheme: suspend (String, Boolean) -> Unit, onBrightness: suspend (Int) -> Unit,
        onNavigate: suspend (String, String?) -> Unit
    ): String {
        val provider = Ai.provider(key)
        val (url, model) = endpoint(provider)
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system(user, owner)))
        history.forEach { messages.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text)) }
        repeat(5) {
            val body = JSONObject().put("model", model).put("messages", messages)
                .put("tools", declarations(true)).put("tool_choice", "auto").put("temperature", 0.3).put("max_tokens", 8192)
            val result = withContext(Dispatchers.IO) {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000; c.readTimeout = 60_000; c.requestMethod = "POST"; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("Authorization", "Bearer $key")
                if (provider == "openrouter") {
                    c.setRequestProperty("HTTP-Referer", "https://matixthemathclub.com")
                    c.setRequestProperty("X-Title", "Matix")
                }
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val text = (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
                val code = c.responseCode
                c.disconnect()
                code to JSONObject(text)
            }
            if (result.first !in 200..299) return "⚠ AI service error (HTTP ${result.first}): ${result.second.optJSONObject("error")?.optString("message").orEmpty().take(220)}"
            val message = result.second.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: return "⚠ The AI service returned an empty response."
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return message.optString("content").ifBlank { "I'm not sure how to respond to that." }
            messages.put(message)
            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val args = runCatching { JSONObject(fn.optString("arguments", "{}")) }.getOrDefault(JSONObject())
                val outcome = runTool(fn.optString("name"), args, user, owner, dark, theme, brightness, store, confirm, onTheme, onBrightness, onNavigate)
                messages.put(JSONObject().put("role", "tool").put("tool_call_id", call.optString("id")).put("content", outcome))
            }
        }
        return "I completed several app actions. Tell me if you want another change."
    }

    private suspend fun geminiReply(
        key: String, user: String, owner: Boolean, dark: Boolean, theme: String, brightness: Int,
        history: List<ChatMsg>, store: Store, confirm: suspend (Approval) -> Boolean,
        onTheme: suspend (String, Boolean) -> Unit, onBrightness: suspend (Int) -> Unit,
        onNavigate: suspend (String, String?) -> Unit
    ): String {
        val contents = JSONArray()
        history.forEach { contents.put(JSONObject().put("role", if (it.fromUser) "user" else "model").put("parts", JSONArray().put(JSONObject().put("text", it.text)))) }
        repeat(5) {
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system(user, owner)))))
                .put("contents", contents)
                .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", declarations(false))))
                .put("generationConfig", JSONObject().put("temperature", 0.3).put("maxOutputTokens", 8192))
            val result = withContext(Dispatchers.IO) {
                val c = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${Firebase.enc(key)}").openConnection() as HttpURLConnection
                c.connectTimeout = 15_000; c.readTimeout = 60_000; c.requestMethod = "POST"; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = c.responseCode
                val text = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
                c.disconnect()
                code to JSONObject(text)
            }
            if (result.first !in 200..299) return "⚠ Gemini error (HTTP ${result.first}): ${result.second.optJSONObject("error")?.optString("message").orEmpty().take(220)}"
            val candidate = result.second.optJSONArray("candidates")?.optJSONObject(0)
                ?: return "⚠ Gemini returned an empty response."
            val modelContent = candidate.optJSONObject("content") ?: return "⚠ Gemini returned an empty response."
            val parts = modelContent.optJSONArray("parts") ?: JSONArray()
            val responses = JSONArray()
            val textOut = StringBuilder()
            var hasCalls = false
            contents.put(modelContent)
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.has("text")) textOut.append(part.optString("text"))
                val call = part.optJSONObject("functionCall") ?: continue
                hasCalls = true
                val name = call.optString("name")
                val args = call.optJSONObject("args") ?: JSONObject()
                val outcome = runTool(name, args, user, owner, dark, theme, brightness, store, confirm, onTheme, onBrightness, onNavigate)
                responses.put(JSONObject().put("functionResponse", JSONObject().put("name", name).put("response", JSONObject().put("result", outcome))))
            }
            if (!hasCalls) return textOut.toString().ifBlank { "I'm not sure how to respond to that." }
            contents.put(JSONObject().put("role", "user").put("parts", responses))
        }
        return "I completed several app actions. Tell me if you want another change."
    }

    private suspend fun runTool(
        name: String, a: JSONObject, user: String, owner: Boolean, dark: Boolean, theme: String, brightness: Int,
        store: Store, confirm: suspend (Approval) -> Boolean, onTheme: suspend (String, Boolean) -> Unit,
        onBrightness: suspend (Int) -> Unit, onNavigate: suspend (String, String?) -> Unit
    ): String {
        fun s(key: String, max: Int = 1000) = a.optString(key).trim().take(max)
        fun error(message: String) = "ERROR: $message"
        return try {
            when (name) {
                "open_chat" -> { onNavigate("mathchat", s("person", 60).ifBlank { null }); "Opened Math Chat." }
                "open_tab" -> {
                    val page = s("page", 40).lowercase()
                    val valid = setOf("home", "games", "ideas", "learn", "math learn", "points", "discussions", "discussion", "messages", "inbox", "users", "bugs", "private", "app pages", "settings", "translator", "translate", "chat", "math chat")
                    if (page !in valid) error("unknown page. Try home, games, ideas, learn, discussions, inbox, points, users, private, settings, or translator.")
                    else { onNavigate(page, null); "Opened $page." }
                }
                "set_theme" -> { val isDark = s("mode", 10).equals("dark", true); onTheme(theme, isDark); "Theme set to ${if (isDark) "dark" else "light"}." }
                "set_colors" -> {
                    val requested = s("preset", 30).lowercase()
                    val preset = when (requested) {
                        "matix" -> "matix"; "ocean" -> "ocean"; "forest" -> "forest"; "sunset" -> "sunset"
                        "grape" -> "grape"; "candy" -> "candy"; "mint" -> "mint"; "slate" -> "slate"
                        else -> return error("unknown colour preset.")
                    }
                    onTheme(preset, dark); "Colour theme set to $preset."
                }
                "set_brightness" -> {
                    val amount = a.optDouble("percent", 100.0).toInt().coerceIn(60, 120)
                    store.brightness = amount; onBrightness(amount); "Brightness is now $amount%."
                }
                "who_am_i" -> "@$user — ${if (owner) "owner" else "member"}; ${if (dark) "dark" else "light"} mode, $theme theme, $brightness% brightness."
                "search_web" -> withContext(Dispatchers.IO) { searchWikipedia(s("query", 200)) }
                "list_games" -> withContext(Dispatchers.IO) {
                    val games = Firebase.get("/hubprojects") as? JSONObject ?: JSONObject()
                    val out = JSONArray()
                    games.keys().asSequence().take(40).forEach { id ->
                        val g = games.optJSONObject(id) ?: JSONObject()
                        out.put(JSONObject().put("name", g.optString("name")).put("author", g.optString("author")).put("loves", g.optInt("loves")))
                    }
                    if (out.length() == 0) "There are no published games yet." else out.toString()
                }
                "get_points" -> withContext(Dispatchers.IO) {
                    val pts = Firebase.get("/points") as? JSONObject ?: JSONObject()
                    val target = s("user", 60).lowercase()
                    if (target.isNotBlank()) "$target has ${pts.optInt(target)} points."
                    else pts.toString().take(6000)
                }
                "add_todo", "add_note" -> withContext(Dispatchers.IO) {
                    val section = if (name == "add_todo") "todos" else "notes"
                    val text = s("text", if (section == "todos") 200 else 500)
                    if (text.isBlank()) error("text was empty.")
                    else {
                        val item = JSONObject().put("text", text).put("at", System.currentTimeMillis())
                        if (section == "todos") item.put("done", false)
                        if (Firebase.post("/private/${Firebase.enc(user)}/$section", item)) if (section == "todos") "Added to your to-do list: “$text”." else "Saved your note."
                        else error("could not save that.")
                    }
                }
                "list_todos", "list_notes" -> withContext(Dispatchers.IO) {
                    val section = if (name == "list_todos") "todos" else "notes"
                    val rows = Firebase.get("/private/${Firebase.enc(user)}/$section") as? JSONObject ?: JSONObject()
                    val values = rows.keys().asSequence().mapNotNull { key ->
                        val item = rows.optJSONObject(key) ?: return@mapNotNull null
                        val mark = if (section == "todos") (if (item.optBoolean("done")) "[done] " else "[ ] ") else "• "
                        mark + item.optString("text")
                    }.toList()
                    if (values.isEmpty()) if (section == "todos") "The to-do list is empty." else "There are no notes yet."
                    else values.joinToString("\n").take(6000)
                }
                "post_idea" -> {
                    val text = s("text", 500)
                    if (text.isBlank()) error("idea was empty.")
                    else if (!confirm(Approval("Post idea to the club?", text))) "Cancelled; nothing was posted."
                    else withContext(Dispatchers.IO) {
                        if (Firebase.post("/ideas", JSONObject().put("text", text).put("author", user).put("at", System.currentTimeMillis()).put("votes", 0), Firebase.IDEAS)) "Idea posted."
                        else error("could not post the idea.")
                    }
                }
                "report_bug" -> {
                    val title = s("title", 120)
                    if (title.isBlank()) error("bug title was empty.")
                    else if (!confirm(Approval("File this bug report?", "$title\n\n${s("detail", 3000)}"))) "Cancelled; nothing was filed."
                    else withContext(Dispatchers.IO) {
                        if (Firebase.post("/bugs", JSONObject().put("title", title).put("detail", s("detail", 3000)).put("author", user).put("at", System.currentTimeMillis()).put("status", "open"))) "Bug report filed."
                        else error("could not file the bug report.")
                    }
                }
                "add_game" -> {
                    val title = s("name", 60)
                    val html = s("html", 700_000)
                    if (title.isBlank() || html.length < 200 || !Regex("<html|<!doctype", RegexOption.IGNORE_CASE).containsMatchIn(html)) error("provide a title and complete HTML document (up to 700 KB).")
                    else if (!confirm(Approval("Publish this game?", "“$title” will be shared with everyone in the Games hub."))) "Cancelled; the game was not published."
                    else withContext(Dispatchers.IO) {
                        val record = JSONObject().put("name", title).put("author", user).put("html", html).put("thumb", "").put("loves", 0).put("createdAt", System.currentTimeMillis()).put("madeWithAI", true).put("published", true)
                        if (Firebase.post("/hubprojects", record)) "Published “$title” to Games." else error("the game could not be saved.")
                    }
                }
                "add_hint", "add_changelog", "give_points" -> {
                    if (!owner) error("this action is restricted to the owner.")
                    else when (name) {
                        "add_hint" -> {
                            val title = s("title", 100); val body = s("body", 1000)
                            if (title.isBlank() || body.isBlank()) error("hint title and text are required.")
                            else if (!confirm(Approval("Publish this home-page hint?", title))) "Cancelled; the hint was not added."
                            else withContext(Dispatchers.IO) {
                                if (Firebase.post("/hints", JSONObject().put("title", title).put("body", body).put("at", System.currentTimeMillis()).put("author", user))) "Hint published."
                                else error("could not add the hint.")
                            }
                        }
                        "add_changelog" -> {
                            val title = s("title", 100); val body = s("body", 2000)
                            if (title.isBlank()) error("a title is required.")
                            else if (!confirm(Approval("Publish this changelog update?", title))) "Cancelled; the update was not published."
                            else withContext(Dispatchers.IO) {
                                if (Firebase.post("/changelog", JSONObject().put("title", title).put("body", body).put("at", System.currentTimeMillis()).put("author", user))) "Changelog update published."
                                else error("could not publish the update.")
                            }
                        }
                        else -> {
                            val target = s("user", 60).lowercase()
                            val amount = a.optDouble("amount", Double.NaN)
                            if (target.isBlank() || !amount.isFinite()) error("a username and numeric point amount are required.")
                            else if (!confirm(Approval("Change member points?", "${if (amount >= 0) "Give" else "Take"} ${kotlin.math.abs(amount.toInt())} points ${if (amount >= 0) "to" else "from"} @$target?"))) "Cancelled; points were not changed."
                            else withContext(Dispatchers.IO) {
                                val current = (Firebase.get("/points/${Firebase.enc(target)}") as? Number)?.toInt() ?: 0
                                val next = maxOf(0, current + amount.toInt())
                                if (Firebase.put("/points/${Firebase.enc(target)}", next)) "$target is now on $next points."
                                else error("could not change the points.")
                            }
                        }
                    }
                }
                else -> error("tool not found.")
            }
        } catch (e: Exception) {
            error(e.message ?: "tool failed.")
        }
    }

    private fun searchWikipedia(query: String): String {
        if (query.isBlank()) return "ERROR: search query was empty."
        val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${URLEncoder.encode(query, "UTF-8")}&srlimit=4&srprop=snippet&format=json&origin=*"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        c.disconnect()
        val results = json.optJSONObject("query")?.optJSONArray("search") ?: JSONArray()
        if (results.length() == 0) return "No results found for “$query”."
        return (0 until results.length()).joinToString("\n\n") { i ->
            val row = results.optJSONObject(i) ?: JSONObject()
            val snippet = row.optString("snippet").replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            "${row.optString("title")}: $snippet"
        }
    }
}