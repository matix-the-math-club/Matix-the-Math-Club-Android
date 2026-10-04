package club.matix.mathclub.data

import org.json.JSONObject

enum class JoinResult { OK, SHORT_NAME, SHORT_PASS, TAKEN, NETWORK }
enum class SignInResult { OK, BAD, BANNED, NETWORK }

/** Port of doSignIn / doJoin from the web app (members live under /members). */
object Auth {
    fun normalize(raw: String) = raw.lowercase().replace(Regex("\\s+"), "").trim()

    fun sessionRevision(user: String): Long {
        val member = Firebase.get("/members/${Firebase.enc(normalize(user))}") as? JSONObject ?: return 0L
        return member.optLong("sessionRev")
    }

    /** Invalidates persisted app sessions on other devices, matching the web client's sessionRev. */
    fun signOutEverywhere(user: String): Long? {
        val revision = System.currentTimeMillis()
        val ok = Firebase.patch(
            "/members/${Firebase.enc(normalize(user))}",
            JSONObject().put("sessionRev", revision)
        )
        return revision.takeIf { ok }
    }

    fun signIn(raw: String, pass: String): SignInResult {
        val u = normalize(raw)
        if (u.isEmpty() || pass.isEmpty()) return SignInResult.BAD
        val m = Firebase.get("/members/${Firebase.enc(u)}") as? JSONObject ?: return SignInResult.BAD
        if (m.optString("password") != pass) return SignInResult.BAD
        if (Firebase.get("/bans/${Firebase.enc(u)}") != null) return SignInResult.BANNED
        return SignInResult.OK
    }

    fun join(raw: String, pass: String): JoinResult {
        val u = raw.lowercase().replace(Regex("[^a-z0-9_]"), "")
        if (u.length < 2) return JoinResult.SHORT_NAME
        if (pass.length < 4) return JoinResult.SHORT_PASS
        if (Firebase.get("/members/${Firebase.enc(u)}") != null) return JoinResult.TAKEN
        val now = System.currentTimeMillis()
        val ok = Firebase.put(
            "/members/${Firebase.enc(u)}",
            JSONObject().put("password", pass).put("joinedAt", now).put("testUser", true)
        )
        if (!ok) return JoinResult.NETWORK
        Firebase.put(
            "/profiles/${Firebase.enc(u)}",
            JSONObject().put("displayName", u).put("bio", "").put("updatedAt", now)
        )
        return JoinResult.OK
    }
}
