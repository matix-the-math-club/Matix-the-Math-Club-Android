package club.matix.mathclub.data

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

data class MathTopic(
    val id: String, val name: String, val emoji: String, val color: Long, val keywords: List<String>
)

data class MathQuestion(
    val topicId: String,
    val prompt: String,
    val body: String,
    val answer: String,
    val choices: List<String>,
    val hint: String,
    val explanation: String,
    val level: Int,
    val numericInput: Boolean = false
) {
    fun isCorrect(input: String): Boolean {
        val a = normalizedNumber(answer)
        val b = normalizedNumber(input)
        return if (a != null && b != null) abs(a - b) < 0.000001
        else input.trim().replace("−", "-").replace(Regex("\\s+"), "").equals(
            answer.trim().replace("−", "-").replace(Regex("\\s+"), ""), true
        )
    }

    private fun normalizedNumber(raw: String): Double? {
        val s = raw.trim().replace("−", "-").replace(",", ".").replace(Regex("\\s+"), "")
        if (s.contains('/')) {
            val parts = s.split('/')
            if (parts.size == 2) {
                val n = parts[0].toDoubleOrNull() ?: return null
                val d = parts[1].toDoubleOrNull() ?: return null
                return if (d == 0.0) null else n / d
            }
        }
        return s.toDoubleOrNull()
    }
}

data class CourseNode(
    val id: String,
    val kind: String,
    val topicId: String = "",
    val level: Int = 1,
    val part: String = "",
    val topics: List<String> = emptyList(),
    val free: Boolean = false
)

data class NodeResult(val stars: Int, val best: Int)

data class PersonalMathLearn(
    val placed: Boolean = false,
    val claimLevel: Int = 1,
    val claimTopics: List<String> = emptyList(),
    val claimText: String = "",
    val levels: Map<String, Int> = emptyMap(),
    val tested: List<String> = emptyList(),
    val course: List<CourseNode> = emptyList(),
    val done: Map<String, NodeResult> = emptyMap(),
    val xp: Int = 0,
    val streak: Int = 0,
    val streakDate: String = "",
    val bestBlitz: Int = 0,
    val bestPop: Int = 0,
    val badges: Map<String, String> = emptyMap(),
    val revise: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        val nodes = JSONArray()
        course.forEach { n ->
            nodes.put(JSONObject().put("id", n.id).put("kind", n.kind).put("topic", n.topicId)
                .put("level", n.level).put("part", n.part).put("topics", JSONArray(n.topics)).put("free", n.free))
        }
        val results = JSONObject()
        done.forEach { (id, r) -> results.put(id, JSONObject().put("stars", r.stars).put("best", r.best)) }
        val lv = JSONObject()
        levels.forEach { (id, n) -> lv.put(id, n) }
        val badgeRows = JSONObject()
        badges.forEach { (id, date) -> badgeRows.put(id, date) }
        return JSONObject().put("v", 5).put("placed", placed).put("claimLevel", claimLevel)
            .put("claimTopics", JSONArray(claimTopics)).put("claimText", claimText)
            .put("levels", lv).put("tested", JSONArray(tested)).put("course", nodes).put("done", results)
            .put("xp", xp).put("streak", JSONObject().put("n", streak).put("last", streakDate))
            .put("best", JSONObject().put("blitz", bestBlitz).put("pop", bestPop))
            .put("badges", badgeRows).put("revise", JSONArray(revise))
    }
}

object AdaptiveMathLearn {
    val topics = listOf(
        MathTopic("arith", "Number Power", "🔢", 0xFFEC4899, listOf("add", "subtract", "plus", "minus", "times", "multiplication", "divide", "arithmetic", "basic", "number", "tables", "counting")),
        MathTopic("frac", "Fractions", "🍕", 0xFFF59E0B, listOf("fraction", "half", "quarter", "numerator")),
        MathTopic("dec", "Decimals & Percent", "💯", 0xFF10B981, listOf("decimal", "percent", "%", "discount")),
        MathTopic("neg", "Negative Numbers", "🌡️", 0xFF38BDF8, listOf("negative", "integer", "below zero", "minus numbers")),
        MathTopic("ops", "Order of Operations", "🧩", 0xFF8B5CF6, listOf("order of operations", "pemdas", "bodmas", "bidmas", "brackets")),
        MathTopic("alg", "Algebra", "🔍", 0xFF6366F1, listOf("algebra", "equation", "solve for", "variable", "unknown", "x and y")),
        MathTopic("geo", "Geometry", "📐", 0xFF14B8A6, listOf("geometry", "area", "perimeter", "shape", "angle", "triangle", "circle", "volume", "pythag")),
        MathTopic("pow", "Powers & Roots", "⚡", 0xFFF97316, listOf("power", "exponent", "root", "square", "cube", "indices")),
        MathTopic("stat", "Averages & Data", "📊", 0xFFEF4444, listOf("mean", "average", "median", "mode", "statistics", "data", "probability"))
    )
    private val topicById = topics.associateBy { it.id }
    val levelNames = listOf("Number Newbie", "Sum Scout", "Equation Explorer", "Fraction Fighter", "Algebra Adventurer", "Geometry Guru", "Math Wizard", "Matix Legend")
    val levelDescriptions = listOf(
        "I’m new or a bit rusty", "Add, subtract, times tables", "Fractions, percents, order of operations", "Algebra, geometry, powers"
    )
    val levelLabels = listOf("Just starting", "The basics", "Comfortable", "Pretty advanced")
    val tips = mapOf(
        "arith" to listOf("Break big sums into friendly pieces: 38 + 27 = 38 + 20 + 7.", "Multiply by 10 first, then adjust.", "Split hundreds, tens and ones, then multiply each part."),
        "frac" to listOf("A fraction is parts of a whole: the bottom counts equal parts, the top counts how many you have.", "To compare or add fractions, give them the same bottom number first.", "Multiply tops by tops and bottoms by bottoms; to divide, keep-flip-multiply."),
        "dec" to listOf("10% is the same as dividing by 10 — build other percents from it.", "Percent means out of 100: 35% = 35/100 = 0.35.", "For a discount, multiply by (100 − off)/100."),
        "neg" to listOf("Think of a thermometer: below zero is just more minus.", "Subtracting a negative is the same as adding a positive.", "Same signs when multiplying or dividing give positive; different signs give negative."),
        "ops" to listOf("Do multiplication before addition: 2 + 3 × 4 = 14.", "Brackets first, then powers, then × ÷, then + −.", "Work left to right at each level and rewrite the expression after each step."),
        "alg" to listOf("Whatever you do to one side of an equals sign, do to the other.", "Undo operations in reverse order: first + or −, then × or ÷.", "Collect x terms on one side and numbers on the other, then divide."),
        "geo" to listOf("Perimeter is the walk around; area is the carpet inside.", "Triangle area is half of base × height; circle area uses πr².", "Pythagoras: a² + b² = c² for the longest side of a right triangle."),
        "pow" to listOf("A square is a number times itself.", "Learn common squares and cubes; they appear everywhere.", "Same base: multiply powers by adding exponents; any nonzero number⁰ = 1."),
        "stat" to listOf("Mean = add everything and divide by how many: a fair share.", "Median = the middle value after sorting.", "Missing value? Mean × count gives the total — subtract what you know.")
    )

    fun topic(id: String) = topicById[id] ?: topics.first()

    fun parseClaim(text: String): Pair<Int?, List<String>> {
        val t = text.lowercase()
        val found = topics.filter { top -> top.keywords.any { it in t } }.map { it.id }
        val level = when {
            Regex("never|nothing|zero|no idea|beginner|\\bnew\\b|scared|hate|bad at").containsMatchIn(t) -> 0
            Regex("advanced|expert|calculus|college|universit|high school|genius|wizard").containsMatchIn(t) -> 3
            Regex("comfortable|decent|good at|\\bok\\b|okay|intermediate|fractions|percent").containsMatchIn(t) -> 2
            Regex("basic|little|some|add|subtract|times").containsMatchIn(t) -> 1
            else -> null
        }
        val grade = Regex("(?:grade|year|class)\\s*(\\d{1,2})").find(t)?.groupValues?.get(1)?.toIntOrNull()
        return (grade?.let { if (it <= 4) 1 else if (it <= 7) 2 else 3 } ?: level) to found
    }

    fun planTopics(level: Int, selected: List<String>): List<String> {
        val defaults = listOf(
            listOf("arith", "frac", "dec", "neg"),
            listOf("arith", "frac", "dec", "ops"),
            listOf("frac", "dec", "ops", "alg"),
            listOf("alg", "geo", "pow", "stat")
        )[level.coerceIn(0, 3)]
        return (selected.filter { topicById.containsKey(it) } + defaults).distinct().take(4)
    }

    fun buildCourse(levels: Map<String, Int>): List<CourseNode> {
        val ordered = topics.mapIndexed { index, t -> t to (levels[t.id] ?: 0) }
            .sortedWith(compareBy<Pair<MathTopic, Int>> { it.second }.thenBy { topics.indexOf(it.first) })
        val chosen = ordered.filter { it.second < 3 }.take(5).toMutableList()
        if (chosen.size < 3) ordered.filter { it.second >= 3 }.take(3 - chosen.size).forEach(chosen::add)
        chosen.sortBy { topics.indexOf(it.first) }
        val nodes = mutableListOf<CourseNode>()
        val seen = mutableListOf<String>()
        chosen.forEachIndexed { i, (topic, skill) ->
            val level = (skill + 1).coerceAtMost(3).coerceAtLeast(1)
            nodes += CourseNode("u${i}a", "lesson", topic.id, level, "a")
            nodes += CourseNode("u${i}b", "lesson", topic.id, level, "b")
            seen += topic.id
            if (i % 2 == 1) nodes += CourseNode("c$i", "blitz", topics = seen.toList())
            if (i % 2 == 1 || i == chosen.lastIndex) nodes += CourseNode("g$i", "game", topics = seen.takeLast(3))
        }
        nodes += CourseNode("boss", "boss", topics = seen.toList())
        return nodes
    }

    fun question(topicId: String, level: Int, seen: MutableSet<String> = mutableSetOf()): MathQuestion {
        var q = generate(topicId, level.coerceIn(1, 3))
        var attempts = 0
        while (q.body in seen && attempts++ < 12) {
            q = generate(topicId, level.coerceIn(1, 3))
        }
        seen += q.body
        return q
    }

    private fun fmt(x: Double): String {
        if (!x.isFinite()) return "0"
        val r = BigDecimal.valueOf(x).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        return r.toPlainString().replace("-", "−")
    }

    private fun q(
        topic: String, level: Int, prompt: String, body: String, answer: String,
        distractors: List<String>, hint: String, why: String, numeric: Boolean = false
    ): MathQuestion {
        val choices = if (numeric) emptyList() else {
            val result = mutableListOf(answer)
            distractors.forEach { if (it !in result && result.size < 4) result += it }
            var delta = 1
            while (result.size < 4) {
                val candidate = fmt((answer.replace("−", "-").toDoubleOrNull() ?: 0.0) + delta)
                if (candidate !in result) result += candidate
                delta++
            }
            result.shuffled()
        }
        return MathQuestion(topic, prompt, body, answer, choices, hint, why, level, numeric)
    }

    private fun num(
        topic: String, level: Int, prompt: String, body: String, answer: Double,
        distractors: List<Double>, hint: String, why: String
    ): MathQuestion {
        val correct = fmt(answer)
        val numeric = level == 3 && Random.nextDouble() < 0.45
        return q(topic, level, prompt, body, correct, distractors.map(::fmt), hint, why, numeric)
    }

    private fun fraction(n: Int, d: Int): String {
        fun gcd(a: Int, b: Int): Int = if (b == 0) kotlin.math.abs(a).coerceAtLeast(1) else gcd(b, a % b)
        val g = gcd(n, d)
        val nn = n / g
        val dd = d / g
        return if (dd == 1) nn.toString() else "$nn/$dd"
    }

    private fun generate(id: String, level: Int): MathQuestion {
        val r = Random
        fun n(min: Int, max: Int) = r.nextInt(min, max + 1)
        fun d(xs: List<Double>) = xs
        when (id) {
            "arith" -> {
                val k = n(0, if (level == 1) 2 else 3)
                val a: Int; val b: Int; val ans: Int; val op: String
                when (k) {
                    0 -> { a = n(if (level == 1) 3 else 26, if (level == 1) 19 else 899); b = n(if (level == 1) 3 else 18, if (level == 1) 19 else 799); ans = a + b; op = "+" }
                    1 -> { a = n(if (level == 1) 12 else 60, if (level == 1) 40 else 999); b = n(2, minOf(a - 1, if (level == 1) 30 else 799)); ans = a - b; op = "−" }
                    2 -> { a = n(if (level == 1) 2 else 6, if (level == 1) 10 else if (level == 2) 12 else 29); b = n(if (level == 1) 2 else 6, if (level == 1) 10 else if (level == 2) 12 else 19); ans = a * b; op = "×" }
                    else -> { b = n(3, 12); ans = n(4, if (level == 3) 45 else 12); a = b * ans; op = "÷" }
                }
                return num(id, level, "What is", "$a $op $b?", ans.toDouble(), d(listOf(ans + 1.0, ans - 1.0, ans + 10.0, ans - 10.0)),
                    "Break it into smaller pieces and check your work.", "$a $op $b = $ans.")
            }
            "frac" -> {
                if (level == 1) {
                    val den = n(2, 8); val a = n(1, den - 1); val b = n(1, den - 1)
                    if (r.nextBoolean()) {
                        val ans = fraction(a + b, den)
                        return q(id, level, "What is", "$a/$den + $b/$den?", ans, listOf(fraction(a + b, den * 2), fraction(a * b, den), fraction(a + b + 1, den)),
                            "The denominators match, so add the numerators.", "Add the tops and keep the same denominator: $ans.")
                    }
                    val total = den * n(2, 6)
                    return num(id, level, "What is", "1/$den of $total?", (total / den).toDouble(), d(listOf(total.toDouble(), (total / den + den).toDouble(), (total / den - 1).toDouble())),
                        "Share $total equally into $den groups.", "$total ÷ $den = ${total / den}.")
                }
                val a = n(1, 4); val b = n(2, 5); val c = n(1, 4); val d0 = n(2, 5)
                val (ansN, ansD) = when (level) {
                    2 -> Pair(a * d0 + c * b, b * d0)
                    else -> when (n(0, 2)) {
                        0 -> Pair(a * d0 + c * b, b * d0)
                        1 -> Pair(a * c, b * d0)
                        else -> Pair(a * d0, b * c)
                    }
                }
                val ans = fraction(ansN, ansD)
                return q(id, level, "What is", "$a/$b + $c/$d0?", ans,
                    listOf(fraction(a + c, b + d0), fraction(a * c, b * d0), fraction(ansN + 1, ansD)),
                    "Find a common denominator before adding.", "Use a common denominator, then simplify: $ans.")
            }
            "dec" -> {
                val k = n(0, 2)
                if (k == 0) {
                    val p = listOf(10, 20, 25, 50, 75).random(); val base = listOf(40, 60, 80, 100, 120, 200).random()
                    val ans = p * base / 100.0
                    return num(id, level, "What is", "$p% of $base?", ans, d(listOf(ans * 2, ans / 2, ans + 10, ans - 1)),
                        "Find 10% first, then build up from it.", "$p% of $base = $base × $p/100 = ${fmt(ans)}.")
                }
                if (k == 1) {
                    val a = n(11, if (level == 1) 49 else 899) / 10.0
                    val b = n(1, 60) / 10.0
                    val plus = r.nextBoolean(); val ans = if (plus) a + b else a - b
                    return num(id, level, "What is", "${fmt(a)} ${if (plus) "+" else "−"} ${fmt(b)}?", ans,
                        d(listOf(ans + 0.1, ans - 0.1, ans + 1, ans - 1)), "Line up the decimal points.", "Keeping decimal points aligned gives ${fmt(ans)}.")
                }
                val f = listOf(Triple(1, 2, "0.5"), Triple(1, 4, "0.25"), Triple(3, 4, "0.75"), Triple(1, 5, "0.2")).random()
                return q(id, level, "Which fraction equals", "${f.third}?", "${f.first}/${f.second}", listOf("1/3", "2/3", "3/10"),
                    "Read the decimal as parts of one whole.", "${f.first} ÷ ${f.second} = ${f.third}.")
            }
            "neg" -> {
                val a = n(2, 12); val b = n(2, 12)
                val parts = when (n(0, 2)) {
                    0 -> listOf("−$a + $b?", (-a + b).toString(), "Picture a thermometer: begin below zero, then move up.", "Starting at −$a and moving up $b gives ${-a + b}.")
                    1 -> listOf("$a − $b?", (a - b).toString(), "Going below zero is allowed.", "$a − $b = ${a - b}.")
                    else -> listOf("(−$a) × (−$b)?", (a * b).toString(), "Two negative signs multiply to a positive.", "Negative × negative = positive, so $a × $b = ${a * b}.")
                }
                return num(id, level, "What is", parts[0], parts[1].toDouble(), d(listOf(-a.toDouble(), a.toDouble(), (a + b).toDouble(), (a - b).toDouble())), parts[2], parts[3])
            }
            "ops" -> {
                val a = n(2, 9); val b = n(2, 8); val c = n(2, 6)
                val answer = a + b * c
                val wrong = (a + b) * c
                return num(id, level, "What is", "$a + $b × $c?", answer.toDouble(), d(listOf(wrong.toDouble(), (answer + 1).toDouble(), (answer - 1).toDouble(), (answer + c).toDouble())),
                    "Brackets, powers, multiplication/division, then addition/subtraction.", "Multiply first: $b × $c = ${b * c}; then add $a to get $answer.")
            }
            "alg" -> {
                val x = n(2, 15); val a = n(2, 6); val b = n(1, 12)
                val rhs = a * x + b
                return num(id, level, "Find x if", "$a x + $b = $rhs", x.toDouble(), d(listOf((rhs - b).toDouble(), (x + 1).toDouble(), (x - 1).toDouble(), (rhs + b).toDouble())),
                    "Undo the +$b first, then divide by $a.", "$rhs − $b = ${a * x}; ${a * x} ÷ $a = $x.")
            }
            "geo" -> {
                when (n(0, 2)) {
                    0 -> { val w = n(3, 12); val h = n(2, 10); val ans = w * h
                        return num(id, level, "A rectangle is $w by $h. What is its area?", "Area = ?", ans.toDouble(), d(listOf((2 * (w + h)).toDouble(), (w + h).toDouble(), (ans + w).toDouble())),
                            "Area = length × width.", "$w × $h = $ans square units.") }
                    1 -> { val w = n(3, 12); val h = n(2, 12); val ans = 2 * (w + h)
                        return num(id, level, "A rectangle is $w by $h. What is its perimeter?", "Perimeter = ?", ans.toDouble(), d(listOf((w * h).toDouble(), (w + h).toDouble(), (ans + 2).toDouble())),
                            "Add the two side lengths and double the total.", "2 × ($w + $h) = $ans.") }
                    else -> { val x = n(30, 80); val y = n(30, 80); val ans = 180 - x - y
                        return num(id, level, "A triangle has angles $x° and $y°. The third is…", "?", ans.toDouble(), d(listOf((360 - x - y).toDouble(), (ans + 10).toDouble(), (x + y).toDouble())),
                            "Angles in a triangle add up to 180°.", "180 − $x − $y = $ans°.") }
                }
            }
            "pow" -> {
                when (n(0, 2)) {
                    0 -> { val a = n(2, 12); val ans = a * a
                        return num(id, level, "What is", "$a²?", ans.toDouble(), d(listOf((a * 2).toDouble(), (ans + a).toDouble(), (ans - a).toDouble())),
                            "Squared means multiply the number by itself.", "$a² = $a × $a = $ans.") }
                    1 -> { val a = n(2, 5); val e = n(2, 4); val ans = Math.pow(a.toDouble(), e.toDouble())
                        return num(id, level, "What is", "$a to the power of $e?", ans, d(listOf((a * e).toDouble(), (ans + a).toDouble(), (ans - a).toDouble())),
                            "Multiply $a by itself $e times.", List(e) { a }.joinToString(" × ") + " = ${fmt(ans)}.") }
                    else -> { val a = n(2, 12); val ans = a
                        return num(id, level, "What is", "√${a * a}?", ans.toDouble(), d(listOf((a * a / 2.0), (a + 1.0), (a - 1.0))),
                            "Which number multiplied by itself makes ${a * a}?", "$a × $a = ${a * a}, so the square root is $a.") }
                }
            }
            else -> {
                val values = List(5) { n(2, 20) }
                when (n(0, 2)) {
                    0 -> { val avg = values.average()
                        return num(id, level, "What is the mean of", values.joinToString(", ") + "?", avg, d(listOf(values.sum().toDouble(), avg + 1, avg - 1, values.max().toDouble())),
                            "Add them all, then divide by how many values there are.", "${values.joinToString(" + ")} = ${values.sum()}; ${values.sum()} ÷ ${values.size} = ${fmt(avg)}.") }
                    1 -> { val sorted = values.sorted(); val med = sorted[2]
                        return num(id, level, "What is the median of", values.joinToString(", ") + "?", med.toDouble(), d(listOf(sorted[1].toDouble(), sorted[3].toDouble(), sorted.first().toDouble())),
                            "Sort the numbers and choose the one in the middle.", "In order: ${sorted.joinToString(", ")}. The middle is $med.") }
                    else -> { val range = values.max() - values.min()
                        return num(id, level, "What is the range of", values.joinToString(", ") + "?", range.toDouble(), d(listOf(values.max().toDouble(), values.min().toDouble(), (range + 1).toDouble())),
                            "Subtract the smallest number from the largest.", "${values.max()} − ${values.min()} = $range.") }
                }
            }
        }
    }

    fun read(json: JSONObject): PersonalMathLearn {
        val nodes = anyList(json.opt("course")).mapNotNull { o ->
            (o as? JSONObject)?.let { CourseNode(
                it.optString("id"), it.optString("kind"), it.optString("topic"), it.optInt("level", 1), it.optString("part"),
                anyList(it.opt("topics")).map { x -> x.toString() }, it.optBoolean("free")
            ) }
        }
        val lv = json.optJSONObject("levels") ?: JSONObject()
        val doneJson = json.optJSONObject("done") ?: JSONObject()
        val badgeJson = json.optJSONObject("badges") ?: JSONObject()
        val streak = json.optJSONObject("streak") ?: JSONObject()
        val best = json.optJSONObject("best") ?: JSONObject()
        return PersonalMathLearn(
            placed = json.optBoolean("placed"), claimLevel = json.optInt("claimLevel", 1).coerceIn(0, 3),
            claimTopics = anyList(json.opt("claimTopics")).map { it.toString() },
            claimText = json.optString("claimText"),
            levels = lv.keys().asSequence().associateWith { lv.optInt(it).coerceIn(0, 3) },
            tested = anyList(json.opt("tested")).map { it.toString() }, course = nodes,
            done = doneJson.keys().asSequence().mapNotNull { id ->
                doneJson.optJSONObject(id)?.let { id to NodeResult(it.optInt("stars", 1), it.optInt("best")) }
            }.toMap(),
            xp = json.optInt("xp"), streak = streak.optInt("n"), streakDate = streak.optString("last"),
            bestBlitz = best.optInt("blitz"), bestPop = best.optInt("pop"),
            badges = badgeJson.keys().asSequence().associateWith { badgeJson.optString(it) },
            revise = anyList(json.opt("revise")).map { it.toString() }
        )
    }

    fun nextNode(state: PersonalMathLearn) = state.course.firstOrNull { it.id !in state.done }

    fun nodeTitle(node: CourseNode): String = when (node.kind) {
        "lesson" -> "${topic(node.topicId).emoji} ${topic(node.topicId).name} · Part ${if (node.part == "a") 1 else 2}"
        "blitz" -> "⚡ 60-second Blitz"
        "game" -> "🫧 Bubble Pop revision"
        else -> "👑 Final Quest"
    }

    fun nodeSubtitle(node: CourseNode): String = when (node.kind) {
        "lesson" -> "${if (node.part == "a") "Learn the trick, then try 5 questions" else "Level-up round with speed bonuses"} · Level ${node.level}"
        "blitz" -> "Score as many points as you can — build combos!"
        "game" -> "A minigame that replays what you just learned"
        else -> "Mixed challenge — your big moment!"
    }

    fun skillLabel(level: Int) = listOf(
        "Seedling 🌱 — let’s grow it", "Sprout 🌿 — getting there", "Tree 🌳 — solid", "Star ⭐ — already strong"
    )[level.coerceIn(0, 3)]
}