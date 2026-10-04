package club.matix.mathclub.data

import org.json.JSONObject

/** Port of the web app's POINTS section (ledger, requests, rewards). */
class PointsData(
    val points: JSONObject, val roles: JSONObject, val profiles: JSONObject,
    val members: JSONObject, val notes: JSONObject, val reqs: JSONObject
) {
    fun role(u: String) = roles.optString(u, "member").ifEmpty { "member" }
    fun score(u: String): Int = when (val v = points.opt(u)) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: 0
        is JSONObject -> v.optInt("total")
        else -> 0
    }
    fun nameOf(u: String) = profiles.optJSONObject(u)?.optString("displayName").orEmpty().ifEmpty { u }
    fun people(): List<String> {
        val seen = linkedSetOf<String>()
        listOf(members, points, roles, profiles).forEach { o -> o.keys().forEach { seen.add(it) } }
        return seen.sortedWith(compareBy<String>({ -score(it) }, { it }))
    }
    fun pending(): List<Pair<String, JSONObject>> =
        reqs.keys().asSequence().mapNotNull { k -> reqs.optJSONObject(k)?.takeIf { it.optString("status") == "pending" }?.let { k to it } }
            .sortedByDescending { it.second.optLong("timestamp") }.toList()
}

class Reward(val icon: String, val title: String, val cost: Int)
class Earn(val icon: String, val title: String, val pts: String, val desc: String)

val EarnList = listOf(
    Earn("✅", "Finish a lesson", "+10", "Work through a lesson on the Lessons page."),
    Earn("✏️", "Do the exercises", "+5 to +15", "Depends how hard the set is."),
    Earn("📝", "Create a lesson", "+20", "Write a lesson that gets published to the club."),
    Earn("💡", "Teach us something new", "+5", "Share a new thing you learned with everybody.")
)
val RewardList = listOf(
    Reward("🏅", "A badge on your profile", 50),
    Reward("🎨", "Your own colour theme", 75),
    Reward("🛡️", "Admin for a week", 100),
    Reward("👑", "Manager for a week", 200)
)

object Points {
    private fun obj(path: String) = Firebase.get(path) as? JSONObject ?: JSONObject()

    fun load() = PointsData(
        Firebase.get("/points") as? JSONObject ?: JSONObject(), obj("/roles"), obj("/profiles"),
        obj("/members"), obj("/member_notes"), obj("/point_requests")
    )

    private fun notify(user: String, title: String, body: String, from: String) {
        Firebase.post(
            "/notifications/${Firebase.enc(user)}",
            JSONObject().put("type", "points").put("title", title).put("body", body).put("at", System.currentTimeMillis()).put("from", from).put("read", false)
        )
    }

    /** amount == null means "reset to 0". Returns a message for the UI. */
    fun change(d: PointsData, me: String, isOwner: Boolean, target: String, amount: Int?): String {
        val canDirect = isOwner || d.role(me) == "manager"
        val canAsk = d.role(me) == "admin"
        if (canAsk && !canDirect) {
            val ok = Firebase.post(
                "/point_requests",
                JSONObject().put("requestedBy", me).put("targetUser", target).put("amount", amount ?: "reset").put("status", "pending").put("timestamp", System.currentTimeMillis())
            )
            return if (ok) "Sent to the owner for approval" else "Could not send the request."
        }
        if (!canDirect) return "Only the owner and the managers can change points."
        val next = if (amount == null) 0 else maxOf(0, d.score(target) + amount)
        if (!Firebase.put("/points/${Firebase.enc(target)}", next)) return "Could not change the points."
        d.points.put(target, next)
        val word = if (amount == null) "reset to 0" else (if (amount > 0) "+" else "") + amount
        notify(target, "⭐ Your points changed", "You are now on $next points ($word) — by ${d.nameOf(me)}.", me)
        return "${d.nameOf(target)} is now on $next points"
    }

    fun askReward(me: String, r: Reward): Boolean = Firebase.post(
        "/point_requests",
        JSONObject().put("requestedBy", me).put("targetUser", me).put("reward", r.title).put("amount", -r.cost).put("status", "pending").put("timestamp", System.currentTimeMillis())
    )

    fun decide(d: PointsData, me: String, id: String, req: JSONObject, ok: Boolean): Boolean {
        val requester = req.optString("requestedBy")
        if (!Firebase.patch("/point_requests/$id", JSONObject().put("status", if (ok) "approved" else "denied").put("decidedBy", me).put("decidedAt", System.currentTimeMillis()))) return false
        if (!ok) {
            notify(requester, "❌ Request denied", "The owner denied your points request.", me)
            return true
        }
        val u = req.optString("targetUser")
        val amt = req.opt("amount")
        val next = if (amt == "reset") 0 else maxOf(0, d.score(u) + ((amt as? Number)?.toInt() ?: 0))
        if (!Firebase.put("/points/${Firebase.enc(u)}", next)) return false
        val reward = req.optString("reward")
        if (reward.isNotEmpty()) {
            if (Regex("badge", RegexOption.IGNORE_CASE).containsMatchIn(reward)) Firebase.put("/profiles/${Firebase.enc(u)}/hasBadge", true)
            notify(u, "🎁 Reward unlocked", "You spent points on “$reward” — enjoy!", me)
        } else notify(u, "⭐ Your points changed", "You are now on $next points.", me)
        notify(requester, "✅ Request approved", "The owner approved your points request.", me)
        return true
    }

    fun saveNote(user: String, text: String) = Firebase.put("/member_notes/${Firebase.enc(user)}", text)
}
