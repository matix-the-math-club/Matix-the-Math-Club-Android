package club.matix.mathclub.data

import org.json.JSONArray
import org.json.JSONObject

const val LRN_MAX_HEARTS = 5
const val LRN_REGEN_MS = 1_200_000L
const val LRN_XP_Q = 10
const val LRN_PASS = 80

/** Arrays in the DB may be stored as JSON arrays or {0:..,1:..} objects. */
fun anyList(o: Any?): List<Any?> = when (o) {
    is JSONArray -> (0 until o.length()).map { o.opt(it) }
    is JSONObject -> o.keys().asSequence().toList().sorted().map { o.opt(it) }
    else -> emptyList()
}

fun lrnNorm(s: Any?): String =
    (s?.toString() ?: "").lowercase().replace(Regex("\\s+"), "").replace(",", "").removePrefix("+")

class Exercise(
    val type: String, val prompt: String, val q: String, val choices: List<String>,
    val answer: String, val hint: String, val steps: List<String>, val pairs: List<Pair<String, String>>
) {
    val blanks: List<String> get() = q.split("___")
    val blankCount: Int get() = maxOf(1, blanks.size - 1)

    fun isRight(given: Any?): Boolean = when (type) {
        "order" -> steps.map(::lrnNorm) == (given as? List<*>)?.map(::lrnNorm)
        "match" -> {
            val m = given as? Map<*, *>
            pairs.isNotEmpty() && pairs.all { lrnNorm(m?.get(it.first)) == lrnNorm(it.second) }
        }
        "blank" -> {
            val w = answer.split("|"); val g = (given as? List<*>) ?: emptyList<Any?>()
            w.isNotEmpty() && w.indices.all { lrnNorm(w[it]) == lrnNorm(g.getOrNull(it)) }
        }
        "multi" -> {
            val w = answer.split("|").map(::lrnNorm).sorted()
            val g = ((given as? List<*>) ?: emptyList<Any?>()).map(::lrnNorm).sorted()
            w.isNotEmpty() && w == g
        }
        else -> answer.split("|").any { lrnNorm(it) == lrnNorm(given) }
    }

    companion object {
        fun from(o: JSONObject): Exercise {
            val pairs = anyList(o.opt("pairs")).mapNotNull { p ->
                when (p) {
                    is String -> p.indexOf('=').takeIf { it > 0 }?.let { p.substring(0, it).trim() to p.substring(it + 1).trim() }
                    is JSONArray -> if (p.length() >= 2) p.optString(0) to p.optString(1) else null
                    is JSONObject -> if (p.has("a") && p.has("b")) p.optString("a") to p.optString("b") else null
                    else -> null
                }
            }
            return Exercise(
                o.optString("type", "input").ifEmpty { "input" }, o.optString("prompt", "Solve it"), o.optString("q"),
                anyList(o.opt("choices")).map { it.toString() }, o.opt("answer")?.toString() ?: "",
                o.optString("hint"), anyList(o.opt("steps")).map { it.toString() }, pairs
            )
        }
    }
}

class Lesson(val id: String, val title: String, val howto: String, val hints: List<String>, val chapterId: String?, val unitId: String?, val order: Int, val ex: List<Exercise>)
class Chapter(val id: String, val title: String, val unitId: String?, val order: Int)
class CourseUnit(val id: String, val title: String, val order: Int)

class LearnState(
    val units: List<CourseUnit>, val chapters: List<Chapter>, val lessons: List<Lesson>,
    val prog: MutableMap<String, Int>, var xp: Int, var gems: Int, var hearts: Int, var heartsAt: Long, var streak: Int
) {
    fun currentHearts(): Int {
        if (hearts >= LRN_MAX_HEARTS) return LRN_MAX_HEARTS
        val back = ((System.currentTimeMillis() - heartsAt) / LRN_REGEN_MS).toInt()
        return (hearts + back).coerceIn(0, LRN_MAX_HEARTS)
    }
    fun lessonsOf(chapterId: String) = lessons.filter { it.chapterId == chapterId }
    fun looseLessons(unitId: String) = lessons.filter { it.unitId == unitId && (it.chapterId == null || chapters.none { c -> c.id == it.chapterId }) }
    fun crowns(id: String) = prog[id] ?: 0
}

object Learn {
    private fun list(o: Any?): List<JSONObject> {
        val j = o as? JSONObject ?: return emptyList()
        return j.keys().asSequence().mapNotNull { k -> j.optJSONObject(k)?.also { it.put("id", k) } }
            .sortedBy { it.optDouble("order", 0.0) }.toList()
    }

    fun load(user: String): LearnState {
        val u = Auth.normalize(user)
        val ch = list(Firebase.get("/learn/chapters"))
        val un = list(Firebase.get("/learn/units"))
        val le = list(Firebase.get("/learn/lessons"))
        val prog = mutableMapOf<String, Int>()
        (Firebase.get("/learn/prog/${Firebase.enc(u)}") as? JSONObject)?.let { p ->
            p.keys().forEach { k -> prog[k] = p.optJSONObject(k)?.optInt("crowns") ?: 0 }
        }
        val hearts = Firebase.get("/learn/hearts/${Firebase.enc(u)}") as? JSONObject
        val streak = Firebase.get("/learn/streak/${Firebase.enc(u)}") as? JSONObject
        fun num(x: Any?): Int = (x as? Number)?.toInt() ?: 0
        return LearnState(
            un.map { CourseUnit(it.getString("id"), it.optString("title", it.optString("name", "Unit")), it.optInt("order")) },
            ch.map { Chapter(it.getString("id"), it.optString("title", it.optString("name", "Chapter")), it.optString("unitId").takeIf { id -> id.isNotEmpty() }, it.optInt("order")) },
            le.map { l ->
                Lesson(
                    l.getString("id"), l.optString("title", "Lesson"), l.optString("howto"),
                    anyList(l.opt("hints")).map { it.toString() }, l.optString("chapterId").takeIf { id -> id.isNotEmpty() },
                    l.optString("unitId").takeIf { id -> id.isNotEmpty() }, l.optInt("order"),
                    anyList(l.opt("ex")).mapNotNull { (it as? JSONObject)?.let(Exercise::from) }
                )
            },
            prog, num(Firebase.get("/learn/xp/${Firebase.enc(u)}")), num(Firebase.get("/learn/gems/${Firebase.enc(u)}")),
            hearts?.optInt("n", LRN_MAX_HEARTS) ?: LRN_MAX_HEARTS, hearts?.optLong("at") ?: System.currentTimeMillis(),
            streak?.optInt("n") ?: 0
        )
    }

    fun saveHearts(user: String, s: LearnState) {
        val n = s.currentHearts()
        s.hearts = n; s.heartsAt = System.currentTimeMillis()
        Firebase.put("/learn/hearts/${Firebase.enc(Auth.normalize(user))}", JSONObject().put("n", n).put("at", s.heartsAt))
    }

    /** Persist the result of a finished lesson; returns xp earned. */
    fun finish(user: String, s: LearnState, lessonId: String?, right: Int, wrong: Int): Int {
        val u = Firebase.enc(Auth.normalize(user))
        val asked = right + wrong
        val pct = if (asked == 0) 0 else Math.round(right * 100f / asked)
        val perfect = wrong == 0 && right > 0
        var xp = right * LRN_XP_Q + if (perfect) 20 else 0
        val gems = if (perfect) 3 else 1
        s.xp += xp; s.gems += gems
        Firebase.put("/learn/xp/$u", s.xp)
        Firebase.put("/learn/gems/$u", s.gems)
        if (lessonId != null && pct >= LRN_PASS) {
            val prev = s.crowns(lessonId)
            val cr = minOf(3, prev + if (perfect) 1 else if (prev > 0) 0 else 1)
            s.prog[lessonId] = cr
            Firebase.put("/learn/prog/$u/$lessonId", JSONObject().put("crowns", cr).put("best", pct).put("at", System.currentTimeMillis()))
        } else if (lessonId != null && pct < LRN_PASS) {
            xp = right * LRN_XP_Q
        }
        return xp
    }
}
