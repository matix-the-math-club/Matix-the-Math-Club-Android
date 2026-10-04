package club.matix.mathclub.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/** Arrays may arrive from Firebase as JSON arrays or index-keyed objects. */
fun anyList(o: Any?): List<Any?> = when (o) {
    is JSONArray -> (0 until o.length()).map { o.opt(it) }
    is JSONObject -> o.keys().asSequence().toList().sorted().map { o.opt(it) }
    else -> emptyList()
}

fun lrnNorm(s: Any?): String =
    (s?.toString() ?: "").lowercase().replace(Regex("\\s+"), "").replace(",", "").removePrefix("+")

/** Question format retained for the separate “Ask Matix to teach me anything” quiz. */
class Exercise(
    val type: String,
    val prompt: String,
    val q: String,
    val choices: List<String>,
    val answer: String,
    val hint: String,
    val steps: List<String>,
    val pairs: List<Pair<String, String>>
) {
    val blanks: List<String> get() = q.split("___")
    val blankCount: Int get() = maxOf(1, blanks.size - 1)

    fun isRight(given: Any?): Boolean = when (type) {
        "order" -> steps.map(::lrnNorm) == (given as? List<*>)?.map(::lrnNorm)
        "match" -> {
            val values = given as? Map<*, *>
            pairs.isNotEmpty() && pairs.all { lrnNorm(values?.get(it.first)) == lrnNorm(it.second) }
        }
        "blank" -> {
            val expected = answer.split("|")
            val actual = (given as? List<*>) ?: emptyList<Any?>()
            expected.isNotEmpty() && expected.indices.all { lrnNorm(expected[it]) == lrnNorm(actual.getOrNull(it)) }
        }
        "multi" -> {
            val expected = answer.split("|").map(::lrnNorm).sorted()
            val actual = ((given as? List<*>) ?: emptyList<Any?>()).map(::lrnNorm).sorted()
            expected.isNotEmpty() && expected == actual
        }
        else -> answer.split("|").any { equivalent(it, given?.toString().orEmpty()) }
    }

    private fun equivalent(expected: String, actual: String): Boolean {
        if (lrnNorm(expected) == lrnNorm(actual)) return true
        fun numeric(value: String): Double? {
            val fraction = Regex("^\\s*(-?\\d+)\\s*/\\s*(\\d+)\\s*$").matchEntire(value)
            if (fraction != null) {
                val denominator = fraction.groupValues[2].toDoubleOrNull() ?: return null
                if (denominator == 0.0) return null
                return (fraction.groupValues[1].toDoubleOrNull() ?: return null) / denominator
            }
            return value.trim().replace(",", "").toDoubleOrNull()
        }
        val e = numeric(expected) ?: return false
        val a = numeric(actual) ?: return false
        return abs(e - a) < 1e-9
    }

    fun toJson(): JSONObject = JSONObject()
        .put("type", type).put("prompt", prompt).put("q", q)
        .put("choices", JSONArray(choices)).put("answer", answer).put("hint", hint)
        .put("steps", JSONArray(steps))
        .put("pairs", JSONArray().apply { pairs.forEach { put(JSONArray().put(it.first).put(it.second)) } })

    companion object {
        fun from(o: JSONObject): Exercise {
            val pairs = anyList(o.opt("pairs")).mapNotNull { row ->
                when (row) {
                    is String -> row.indexOf('=').takeIf { it > 0 }?.let { row.substring(0, it).trim() to row.substring(it + 1).trim() }
                    is JSONArray -> if (row.length() >= 2) row.optString(0) to row.optString(1) else null
                    is JSONObject -> if (row.has("a") && row.has("b")) row.optString("a") to row.optString("b") else null
                    else -> null
                }
            }
            return Exercise(
                o.optString("type", "input").ifEmpty { "input" },
                o.optString("prompt", "Solve it"),
                o.optString("q"),
                anyList(o.opt("choices")).map { it.toString() },
                o.opt("answer")?.toString() ?: "",
                o.optString("hint"),
                anyList(o.opt("steps")).map { it.toString() },
                pairs
            )
        }
    }
}