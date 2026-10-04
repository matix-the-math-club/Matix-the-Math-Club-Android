package club.matix.mathclub.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

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
        else -> answer.split("|").any { equivalentAnswer(it, given?.toString().orEmpty()) }
    }

    private fun equivalentAnswer(expected: String, actual: String): Boolean {
        if (lrnNorm(expected) == lrnNorm(actual)) return true
        fun fraction(value: String): Double? {
            val f = Regex("^\\s*(-?\\d+)\\s*/\\s*(\\d+)\\s*$").matchEntire(value) ?: return null
            val den = f.groupValues[2].toDoubleOrNull() ?: return null
            if (den == 0.0) return null
            return (f.groupValues[1].toDoubleOrNull() ?: return null) / den
        }
        val expectedNumber = fraction(expected) ?: expected.trim().replace(",", "").toDoubleOrNull()
        val actualNumber = fraction(actual) ?: actual.trim().replace(",", "").toDoubleOrNull()
        return expectedNumber != null && actualNumber != null && abs(expectedNumber - actualNumber) < 1e-9
    }

    fun toJson(): JSONObject = JSONObject()
        .put("type", type).put("prompt", prompt).put("q", q)
        .put("choices", JSONArray(choices)).put("answer", answer).put("hint", hint)
        .put("steps", JSONArray(steps))
        .put("pairs", JSONArray().apply { pairs.forEach { put(JSONArray().put(it.first).put(it.second)) } })

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

class Lesson(
    val id: String, val title: String, val howto: String, val hints: List<String>,
    val chapterId: String?, val unitId: String?, val order: Int, val ex: List<Exercise>,
    val icon: String = "⭐", val color: String = "", val description: String = ""
)
class Chapter(
    val id: String, val title: String, val unitId: String?, val order: Int,
    val test: List<Exercise> = emptyList(), val icon: String = "📘", val color: String = ""
)
class CourseUnit(
    val id: String, val title: String, val order: Int, val description: String = "",
    val test: List<Exercise> = emptyList(), val icon: String = "📚", val color: String = "#58CC02"
)

class LearnState(
    val units: List<CourseUnit>, val chapters: List<Chapter>, val lessons: List<Lesson>,
    val prog: MutableMap<String, Int>, var xp: Int, var gems: Int, var hearts: Int, var heartsAt: Long, var streak: Int,
    var boostUntil: Long = 0L, var freeze: Boolean = false, val badges: MutableMap<String, Long> = mutableMapOf(),
    var tests: Int = 0, var chapTests: Int = 0,
    val skippedUnits: MutableSet<String> = mutableSetOf(),
    val skippedChapters: MutableSet<String> = mutableSetOf(),
    val quests: MutableMap<String, Int> = mutableMapOf(),
    var placed: Boolean = false,
    val mistakes: MutableList<Exercise> = mutableListOf(),
    var lastStreakDay: String = "",
    var questDay: String = "",
    val friendMembers: List<String> = emptyList(),
    val friendQuests: MutableMap<String, JSONObject> = mutableMapOf(),
    var friendQuestsCompleted: Int = 0
) {
    fun boosted() = boostUntil > System.currentTimeMillis()
    fun unitSkipped(id: String) = id in skippedUnits
    fun chapterSkipped(id: String) = id in skippedChapters || chapters.firstOrNull { it.id == id }?.unitId?.let(::unitSkipped) == true
    fun lessonCleared(lesson: Lesson) = crowns(lesson.id) > 0 ||
        lesson.chapterId?.let(::chapterSkipped) == true || lesson.unitId?.let(::unitSkipped) == true
    fun lessonOpen(lesson: Lesson, isOwner: Boolean): Boolean {
        if (isOwner || lessonCleared(lesson)) return true
        val unitId = lesson.unitId ?: chapters.firstOrNull { it.id == lesson.chapterId }?.unitId ?: return true
        val unitIndex = units.indexOfFirst { it.id == unitId }
        if (unitIndex < 0 || units.take(unitIndex).any { !unitDone(it) }) return false
        val sequence = lessons.filter { candidate ->
            candidate.unitId == unitId || chapters.firstOrNull { it.id == candidate.chapterId }?.unitId == unitId
        }.sortedWith(compareBy<Lesson> { chapterOrder(it.chapterId) }.thenBy { it.order })
        val index = sequence.indexOfFirst { it.id == lesson.id }
        return index <= 0 || sequence.getOrNull(index - 1)?.let(::lessonCleared) == true
    }
    private fun chapterOrder(chapterId: String?): Int =
        chapters.firstOrNull { it.id == chapterId }?.order ?: 0
    fun unitDone(u: CourseUnit): Boolean {
        if (unitSkipped(u.id)) return true
        val ls = lessons.filter { l -> l.unitId == u.id || chapters.any { c -> c.id == l.chapterId && c.unitId == u.id } }
        return ls.isNotEmpty() && ls.all(::lessonCleared)
    }
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
        val friendRows = Firebase.get("/learn/fq") as? JSONObject ?: JSONObject()
        val myFriendQuests = mutableMapOf<String, JSONObject>()
        friendRows.keys().forEach { id ->
            val row = friendRows.optJSONObject(id) ?: return@forEach
            if (Auth.normalize(row.optString("a")) == u || Auth.normalize(row.optString("b")) == u) {
                myFriendQuests[id] = JSONObject(row.toString())
            }
        }
        fun num(x: Any?): Int = (x as? Number)?.toInt() ?: 0
        return LearnState(
            un.map { CourseUnit(
                it.getString("id"), it.optString("title", it.optString("name", "Unit")), it.optInt("order"),
                it.optString("desc"), anyList(it.opt("test")).mapNotNull { x -> (x as? JSONObject)?.let(Exercise::from) },
                it.optString("icon", "📚"), it.optString("color", "#58CC02")
            ) },
            ch.map { Chapter(
                it.getString("id"), it.optString("title", it.optString("name", "Chapter")),
                it.optString("unitId").takeIf { id -> id.isNotEmpty() }, it.optInt("order"),
                anyList(it.opt("test")).mapNotNull { x -> (x as? JSONObject)?.let(Exercise::from) },
                it.optString("icon", "📘"), it.optString("color")
            ) },
            le.map { l ->
                Lesson(
                    l.getString("id"), l.optString("title", "Lesson"), l.optString("howto"),
                    anyList(l.opt("hints")).map { it.toString() }, l.optString("chapterId").takeIf { id -> id.isNotEmpty() },
                    l.optString("unitId").takeIf { id -> id.isNotEmpty() }, l.optInt("order"),
                    anyList(l.opt("ex")).mapNotNull { (it as? JSONObject)?.let(Exercise::from) },
                    l.optString("icon", "⭐"), l.optString("color"), l.optString("desc")
                )
            },
            prog, num(Firebase.get("/learn/xp/${Firebase.enc(u)}")), num(Firebase.get("/learn/gems/${Firebase.enc(u)}")),
            hearts?.optInt("n", LRN_MAX_HEARTS) ?: LRN_MAX_HEARTS, hearts?.optLong("at") ?: System.currentTimeMillis(),
            streak?.let { it.optInt("n", it.optInt("count")) } ?: 0,
            (Firebase.get("/learn/boost/${Firebase.enc(u)}") as? Number)?.toLong() ?: 0L,
            (streak?.optInt("freeze") ?: 0) > 0,
            (Firebase.get("/learn/badges/${Firebase.enc(u)}") as? JSONObject)?.let { b -> b.keys().asSequence().associateWith { b.optLong(it) }.toMutableMap() } ?: mutableMapOf(),
            (Firebase.get("/learn/tests/${Firebase.enc(u)}") as? JSONObject)?.length() ?: 0,
            (Firebase.get("/learn/ctests/${Firebase.enc(u)}") as? JSONObject)?.length() ?: 0,
            (Firebase.get("/learn/tests/${Firebase.enc(u)}") as? JSONObject)?.let { j -> j.keys().asSequence().filter { j.optBoolean(it) }.toMutableSet() } ?: mutableSetOf(),
            (Firebase.get("/learn/ctests/${Firebase.enc(u)}") as? JSONObject)?.let { j -> j.keys().asSequence().filter { j.optBoolean(it) }.toMutableSet() } ?: mutableSetOf(),
            (Firebase.get("/learn/quests/${Firebase.enc(u)}/${LocalDate.now()}") as? JSONObject)?.let { j ->
                j.keys().asSequence().mapNotNull { k -> j.optInt(k).takeIf { k != "__d" }?.let { k to it } }.toMap().toMutableMap()
            } ?: mutableMapOf(),
            Firebase.get("/learn/placed/${Firebase.enc(u)}") == true,
            anyList(Firebase.get("/learn/mistakes/${Firebase.enc(u)}")).mapNotNull { (it as? JSONObject)?.let(Exercise::from) }.toMutableList(),
            streak?.optString("last").orEmpty(),
            (Firebase.get("/learn/quests/${Firebase.enc(u)}/${LocalDate.now()}") as? JSONObject)?.optString("__d").orEmpty(),
            Firebase.getKeys("/members").map(Auth::normalize).filter { it.isNotBlank() && it != u }.sorted(),
            myFriendQuests,
            myFriendQuests.values.count { it.optBoolean("done") }
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
        if (s.boosted()) xp *= 2
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
        bumpFriendQuest(user, s, xp)
        return xp
    }

    fun recordMistake(user: String, s: LearnState, exercise: Exercise) {
        s.mistakes.removeAll { it.q == exercise.q }
        s.mistakes.add(0, exercise)
        while (s.mistakes.size > 40) s.mistakes.removeAt(s.mistakes.lastIndex)
        Firebase.put("/learn/mistakes/${Firebase.enc(Auth.normalize(user))}", JSONArray().apply {
            s.mistakes.forEach { put(it.toJson()) }
        })
    }

    fun clearMistake(user: String, s: LearnState, exercise: Exercise) {
        s.mistakes.removeAll { it.q == exercise.q }
        Firebase.put("/learn/mistakes/${Firebase.enc(Auth.normalize(user))}", JSONArray().apply {
            s.mistakes.forEach { put(it.toJson()) }
        })
    }

    fun bumpStreak(user: String, s: LearnState) {
        val today = LocalDate.now().toString()
        if (s.lastStreakDay == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        when {
            s.lastStreakDay == yesterday -> s.streak += 1
            s.freeze -> s.freeze = false
            else -> s.streak = 1
        }
        s.lastStreakDay = today
        Firebase.put("/learn/streak/${Firebase.enc(Auth.normalize(user))}",
            JSONObject().put("count", s.streak).put("last", today).put("freeze", if (s.freeze) 1 else 0))
    }

    private val quests = listOf(
        Triple("xp30", 30, 5), Triple("les2", 2, 5), Triple("cor15", 15, 5), Triple("perf", 1, 10)
    )
    private val questLabels = mapOf("xp30" to "Earn 30 XP", "les2" to "Finish 2 lessons", "cor15" to "Get 15 answers right", "perf" to "One perfect lesson")

    /** Adds progress to today's four shared daily quests and awards their gem rewards once. */
    fun bumpQuests(user: String, s: LearnState, xp: Int, lessons: Int, right: Int, perfect: Boolean): List<String> {
        val day = LocalDate.now().toString()
        val next = if (s.questDay == day) s.quests.toMutableMap() else mutableMapOf()
        val gained = mapOf("xp30" to xp, "les2" to lessons, "cor15" to right, "perf" to if (perfect) 1 else 0)
        val completed = mutableListOf<String>()
        var gems = 0
        quests.forEach { (id, goal, reward) ->
            val previous = next[id] ?: 0
            if (previous < goal) {
                val now = (previous + (gained[id] ?: 0)).coerceAtMost(goal)
                next[id] = now
                if (now >= goal && previous < goal) {
                    gems += reward
                    completed += questLabels[id] ?: id
                }
            }
        }
        s.quests.clear()
        s.quests.putAll(next)
        s.questDay = day
        if (gems > 0) {
            s.gems += gems
            Firebase.put("/learn/gems/${Firebase.enc(Auth.normalize(user))}", s.gems)
        }
        val json = JSONObject().put("__d", day)
        next.forEach { (key, value) -> json.put(key, value) }
        Firebase.put("/learn/quests/${Firebase.enc(Auth.normalize(user))}/$day", json)
        return completed
    }

    /** Persists test XP and the legacy unit/chapter skip flags; tests never consume hearts. */
    fun finishTest(user: String, s: LearnState, kind: String, targetId: String?, right: Int, wrong: Int): Int {
        val u = Firebase.enc(Auth.normalize(user))
        val attempted = right + wrong
        val percent = if (attempted == 0) 0 else Math.round(right * 100f / attempted)
        val perfect = attempted > 0 && wrong == 0
        var xp = right * LRN_XP_Q + if (perfect) 20 else 0
        if (kind == "unit-test" || kind == "chapter-test") xp = (xp * 1.5f).toInt()
        if (s.boosted()) xp *= 2
        s.xp += xp
        if (perfect) s.gems += 3 else s.gems += 1
        Firebase.put("/learn/xp/$u", s.xp)
        Firebase.put("/learn/gems/$u", s.gems)
        when {
            kind == "placement" -> {
                s.placed = true
                Firebase.put("/learn/placed/$u", true)
                val count = when {
                    percent >= 85 -> (s.units.size - 1).coerceAtLeast(0)
                    percent >= 65 -> minOf(2, s.units.size)
                    percent >= 45 -> minOf(1, s.units.size)
                    else -> 0
                }
                s.units.take(count).forEach { unit ->
                    s.skippedUnits += unit.id
                    Firebase.put("/learn/tests/$u/${Firebase.enc(unit.id)}", true)
                }
            }
            kind == "unit-test" && targetId != null && percent >= LRN_PASS -> {
                s.skippedUnits += targetId
                s.tests = s.skippedUnits.size
                Firebase.put("/learn/tests/$u/${Firebase.enc(targetId)}", true)
            }
            kind == "chapter-test" && targetId != null && percent >= LRN_PASS -> {
                val target = s.chapters.firstOrNull { it.id == targetId }
                if (target != null) {
                    s.chapters.filter { it.unitId == target.unitId && it.order <= target.order }.forEach { chapter ->
                        s.skippedChapters += chapter.id
                        Firebase.put("/learn/ctests/$u/${Firebase.enc(chapter.id)}", true)
                    }
                    s.chapTests = s.skippedChapters.size
                }
            }
        }
        bumpStreak(user, s)
        bumpQuests(user, s, xp, 0, right, perfect)
        bumpFriendQuest(user, s, xp)
        return xp
    }

    fun startFriendQuest(user: String, friend: String): Boolean {
        val a = Auth.normalize(user)
        val b = Auth.normalize(friend)
        if (a.isBlank() || b.isBlank() || a == b) return false
        val existing = Firebase.get("/learn/fq") as? JSONObject
        if (existing?.keys()?.asSequence()?.any { id ->
                val q = existing.optJSONObject(id) ?: return@any false
                !q.optBoolean("done") && (Auth.normalize(q.optString("a")) == a || Auth.normalize(q.optString("b")) == a)
            } == true) return false
        val id = listOf(a, b).sorted().joinToString("__")
        return Firebase.put(
            "/learn/fq/${Firebase.enc(id)}",
            JSONObject().put("a", a).put("b", b).put("goal", 200).put("pa", 0).put("pb", 0)
                .put("at", System.currentTimeMillis()).put("done", false)
        )
    }

    /** Adds earned XP to the user's active shared friend quest, if one exists. */
    private fun bumpFriendQuest(user: String, s: LearnState, xp: Int) {
        if (xp <= 0) return
        val me = Auth.normalize(user)
        val live = Firebase.get("/learn/fq") as? JSONObject ?: return
        val (id, row) = live.keys().asSequence().mapNotNull { id ->
            live.optJSONObject(id)?.let { id to it }
        }.firstOrNull { (_, quest) ->
            !quest.optBoolean("done") &&
                (Auth.normalize(quest.optString("a")) == me || Auth.normalize(quest.optString("b")) == me)
        } ?: return
        s.friendQuests[id] = JSONObject(row.toString())
        val key = if (Auth.normalize(row.optString("a")) == me) "pa" else "pb"
        row.put(key, row.optInt(key) + xp)
        Firebase.put("/learn/fq/${Firebase.enc(id)}/$key", row.optInt(key))
        if (row.optInt("pa") + row.optInt("pb") >= row.optInt("goal", 200) && !row.optBoolean("done")) {
            row.put("done", true)
            Firebase.put("/learn/fq/${Firebase.enc(id)}/done", true)
            s.friendQuestsCompleted += 1
            s.gems += 15
            Firebase.put("/learn/gems/${Firebase.enc(me)}", s.gems)
        }
    }

    fun saveCourseItem(collection: String, id: String?, value: JSONObject): String? {
        val key = id ?: UUID.randomUUID().toString().replace("-", "")
        return key.takeIf { Firebase.put("/learn/$collection/${Firebase.enc(it)}", value) }
    }

    fun deleteCourseItem(collection: String, id: String): Boolean =
        Firebase.delete("/learn/$collection/${Firebase.enc(id)}")

    fun markLessonFinished(user: String, s: LearnState, right: Int, wrong: Int, xpEarned: Int) {
        bumpStreak(user, s)
        val complete = wrong == 0 && right > 0
        bumpQuests(user, s, xpEarned, 1, right, complete)
    }

    class ShopItem(val id: String, val icon: String, val name: String, val cost: Int, val desc: String)

    val Shop = listOf(
        ShopItem("hearts", "❤", "Refill hearts", 50, "Straight back to five hearts so you can keep going right now."),
        ShopItem("boost", "⚡", "XP doubler", 30, "Every bit of XP you earn is doubled for the next 15 minutes."),
        ShopItem("freeze", "❄", "Streak freeze", 40, "Miss a whole day and your streak survives anyway."),
        ShopItem("box", "🎁", "Mystery box", 20, "Somewhere between 5 and 30 gems, and sometimes a half hour XP doubler instead.")
    )

    /** Returns a message to show. */
    fun buy(user: String, s: LearnState, id: String): String {
        val item = Shop.firstOrNull { it.id == id } ?: return ""
        if (s.gems < item.cost) return "Not enough gems yet."
        val u = Firebase.enc(Auth.normalize(user))
        s.gems -= item.cost
        var msg: String
        when (id) {
            "hearts" -> { s.hearts = LRN_MAX_HEARTS; s.heartsAt = System.currentTimeMillis(); Firebase.put("/learn/hearts/$u", JSONObject().put("n", s.hearts).put("at", s.heartsAt)); msg = "Hearts refilled!" }
            "boost" -> { s.boostUntil = System.currentTimeMillis() + 900_000; Firebase.put("/learn/boost/$u", s.boostUntil); msg = "Double XP for 15 minutes — go go go!" }
            "freeze" -> { s.freeze = true; Firebase.put("/learn/streak/$u", JSONObject().put("count", s.streak).put("last", "").put("freeze", 1)); msg = "Streak freeze ready." }
            else -> if (Math.random() < 0.15) {
                s.boostUntil = System.currentTimeMillis() + 1_800_000; Firebase.put("/learn/boost/$u", s.boostUntil); msg = "Jackpot — 30 minutes of double XP!"
            } else {
                val g = (Math.random() * 26).toInt() + 5; s.gems += g; msg = "You got $g 💎 back."
            }
        }
        Firebase.put("/learn/gems/$u", s.gems)
        return msg
    }

    class Badge(val icon: String, val id: String, val name: String, val desc: String, val test: (Stats) -> Boolean)
    class Stats(
        val lessons: Int, val maxCrowns: Int, val units: Int, val tests: Int, val chaps: Int,
        val xp: Int, val gems: Int, val streak: Int, val friendQuests: Int
    )

    fun stats(s: LearnState) = Stats(
        s.prog.values.count { it > 0 }, s.prog.values.maxOrNull() ?: 0, s.units.count { s.unitDone(it) },
        s.tests, s.chapTests, s.xp, s.gems, s.streak, s.friendQuestsCompleted
    )

    val Badges = listOf(
        Badge("🌱", "first", "First steps", "Finish your first lesson") { it.lessons >= 1 },
        Badge("📚", "ten", "Bookworm", "Finish 10 lessons") { it.lessons >= 10 },
        Badge("🏅", "unit", "Unit cleared", "Finish a whole unit") { it.units >= 1 },
        Badge("⚡", "chap", "Jumped ahead", "Skip to a chapter with its test") { it.chaps >= 1 },
        Badge("⭐", "xp500", "500 XP", "Earn 500 XP") { it.xp >= 500 },
        Badge("✨", "xp2k", "2000 XP", "Earn 2000 XP") { it.xp >= 2000 },
        Badge("🔥", "st3", "3 day streak", "Learn 3 days in a row") { it.streak >= 3 },
        Badge("🔥", "st7", "Week on fire", "Learn 7 days in a row") { it.streak >= 7 },
        Badge("👑", "st30", "Unstoppable", "Learn 30 days in a row") { it.streak >= 30 },
        Badge("👑", "crown", "Crown collector", "Get 3 crowns on a lesson") { it.maxCrowns >= 3 },
        Badge("🏆", "test", "Test ace", "Pass a unit final test") { it.tests >= 1 },
        Badge("💎", "rich", "Gem hoarder", "Hold 100 gems at once") { it.gems >= 100 },
        Badge("👥", "friend", "Better together", "Finish a friend quest") { it.friendQuests >= 1 }
    )

    /** Grants any newly earned badges and returns their names. */
    fun awardBadges(user: String, s: LearnState): List<Badge> {
        val st = stats(s)
        val u = Firebase.enc(Auth.normalize(user))
        val got = Badges.filter { it.id !in s.badges && runCatching { it.test(st) }.getOrDefault(false) }
        got.forEach { b -> val t = System.currentTimeMillis(); s.badges[b.id] = t; Firebase.put("/learn/badges/$u/${b.id}", t) }
        return got
    }

    fun leaderboard(): List<Pair<String, Int>> {
        val all = Firebase.get("/learn/xp") as? JSONObject ?: return emptyList()
        return all.keys().asSequence().map { k ->
            k to when (val v = all.opt(k)) { is JSONObject -> v.optInt("total"); is Number -> v.toInt(); else -> 0 }
        }.sortedByDescending { it.second }.toList()
    }
}
