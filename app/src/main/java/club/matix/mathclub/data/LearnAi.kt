package club.matix.mathclub.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AiLessonStep(val title: String, val body: String, val example: String)
data class AiWorkedExample(val problem: String, val solution: String)
data class AiGlossaryEntry(val term: String, val meaning: String)
data class AiSource(val name: String, val url: String)
data class AiLearnLesson(
    val slug: String,
    val topic: String,
    val title: String,
    val emoji: String,
    val intro: String,
    val steps: List<AiLessonStep>,
    val examples: List<AiWorkedExample>,
    val mistakes: List<String>,
    val glossary: List<AiGlossaryEntry>,
    val sources: List<AiSource>,
    val ownerLessons: List<String>,
    val agreement: Int?,
    val exercises: List<Exercise>,
    val cacheKind: String
)

object LearnAi {
    const val DEFAULT_SERVER = "http://10.0.2.2:8787"

    fun slug(topic: String): String = topic.lowercase().trim()
        .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(80).ifEmpty { "topic" }

    fun serverUrl(store: Store): String {
        if (store.learnServerUrl.isNotBlank()) return store.learnServerUrl.trimEnd('/')
        val shared = Firebase.get("/learnai/server") as? String
        return (shared?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: DEFAULT_SERVER).trimEnd('/')
    }

    fun fetch(store: Store, user: String, topic: String, refresh: Boolean = false): AiLearnLesson {
        val safeTopic = topic.trim().take(120)
        require(safeTopic.isNotEmpty()) { "Type a topic first." }
        val path = "/learnai/lessons/${Firebase.enc(slug(safeTopic))}"
        try {
            val url = serverUrl(store) + "/api/learn?topic=${URLEncoder.encode(safeTopic, "UTF-8")}" +
                "&user=${URLEncoder.encode(Auth.normalize(user).ifBlank { "guest" }, "UTF-8")}" +
                if (refresh) "&refresh=1" else ""
            val response = getJson(url)
            if (response.optBoolean("ok") && response.optJSONObject("lesson") != null) {
                val lesson = parse(response.getJSONObject("lesson"), response.optString("cache", "fresh"), safeTopic)
                Firebase.put(path, response.getJSONObject("lesson"))
                val idx = JSONObject().put("topic", lesson.topic).put("title", lesson.title).put("emoji", lesson.emoji)
                    .put("at", System.currentTimeMillis())
                val old = Firebase.get("/learnai/index/${Firebase.enc(lesson.slug)}") as? JSONObject
                idx.put("hits", (old?.optInt("hits") ?: 0) + 1)
                Firebase.put("/learnai/index/${Firebase.enc(lesson.slug)}", idx)
                return lesson
            }
            throw IllegalStateException(response.optString("error").ifBlank { "The Learn server returned no lesson." })
        } catch (e: Exception) {
            if (refresh) throw IllegalStateException(e.message ?: "The Learn server is unreachable.")
            val cached = Firebase.get(path) as? JSONObject
                ?: throw IllegalStateException("The Learn server is unreachable and this topic is not cached. Check the server URL or try a popular lesson.")
            return parse(cached, "firebase", safeTopic)
        }
    }

    fun popular(): List<JSONObject> {
        val index = Firebase.get("/learnai/index") as? JSONObject ?: return emptyList()
        return index.keys().asSequence().mapNotNull { id ->
            index.optJSONObject(id)?.let { JSONObject(it.toString()).put("slug", id) }
        }.sortedByDescending { it.optInt("hits") }.take(10).toList()
    }

    private fun parse(root: JSONObject, cacheKind: String, fallbackTopic: String): AiLearnLesson {
        fun objects(key: String) = anyList(root.opt(key)).mapNotNull { it as? JSONObject }
        fun strings(key: String) = anyList(root.opt(key)).map { it.toString() }.filter(String::isNotBlank)
        val topic = root.optString("topic").ifBlank { fallbackTopic }
        val exerciseRows = anyList(root.opt("exercises")).mapNotNull { it as? JSONObject }
        val steps = objects("steps").map { AiLessonStep(it.optString("title"), it.optString("body"), it.optString("example")) }
        val examples = objects("examples").map { AiWorkedExample(it.optString("problem"), it.optString("solution")) }
        val glossary = objects("glossary").map { AiGlossaryEntry(it.optString("term"), it.optString("meaning")) }
        val sources = objects("sources").map { AiSource(it.optString("name").ifBlank { it.optString("url") }, it.optString("url")) }
        return AiLearnLesson(
            slug = root.optString("slug").ifBlank { slug(topic) }, topic = topic,
            title = root.optString("title").ifBlank { topic }, emoji = root.optString("emoji").ifBlank { "📘" },
            intro = root.optString("intro"), steps = steps, examples = examples,
            mistakes = strings("mistakes"), glossary = glossary, sources = sources,
            ownerLessons = strings("ownerLessonsUsed"), agreement = root.optInt("agreement").takeIf { root.has("agreement") },
            exercises = exerciseRows.map(Exercise::from), cacheKind = cacheKind
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 150_000
        connection.requestMethod = "GET"
        try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Learn server returned ${connection.responseCode}.")
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
}
