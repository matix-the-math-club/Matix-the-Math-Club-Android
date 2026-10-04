package club.matix.mathclub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/**
 * Direct-message data adapter for the same Firebase paths used by the web app.
 * Message IDs are client-generated because the tiny REST wrapper intentionally
 * exposes no Firebase push-key API; timestamps, not keys, define message order.
 */
data class ChatPerson(
    val username: String,
    val displayName: String,
    val role: String,
    val emoji: String,
    val color: String,
    val hasBadge: Boolean
)

data class DirectMessage(
    val id: String,
    val from: String,
    val to: String,
    val type: String,
    val text: String,
    val at: Long
)

data class ConversationPreview(
    val person: ChatPerson,
    val lastMessage: DirectMessage?,
    val unread: Int
)

data class DirectChatSnapshot(
    val messages: List<DirectMessage>,
    val otherReadAt: Long,
    val otherTypingAt: Long,
    val otherOnline: Boolean
)

object DirectChatRepository {
    private val corePeople = linkedMapOf(
        "ghadi" to "Ghadi", "dahlia" to "Dahlia", "yara" to "Yara",
        "jad" to "Jad", "marwan" to "Marwan", "mak" to "Mohammad-ali", "hicham" to "Hicham"
    )

    /** Deliberately reads public profile/role indexes, not /members (which contains passwords). */
    fun people(me: String): List<ChatPerson> {
        val profiles = Firebase.get("/profiles") as? JSONObject ?: JSONObject()
        val roles = Firebase.get("/roles") as? JSONObject ?: JSONObject()
        val usernames = linkedSetOf<String>().apply {
            addAll(profiles.keys().asSequence().toList())
            addAll(roles.keys().asSequence().toList())
            addAll(corePeople.keys)
        }
        return usernames.asSequence()
            .map { Auth.normalize(it) }
            .filter { it.isNotEmpty() && it != Auth.normalize(me) }
            .distinct()
            .map { username ->
                val profile = profiles.optJSONObject(username) ?: JSONObject()
                val role = roles.optString(username).ifEmpty {
                    if (username in corePeople) "member" else "user"
                }
                ChatPerson(
                    username = username,
                    displayName = profile.optString("displayName").ifBlank { corePeople[username] ?: username },
                    role = role,
                    emoji = profile.optString("emoji"),
                    color = profile.optString("color"),
                    hasBadge = profile.optBoolean("hasBadge")
                )
            }
            .sortedWith(compareBy<ChatPerson> { it.displayName.lowercase() }.thenBy { it.username })
            .toList()
    }

    fun conversationId(a: String, b: String): String =
        listOf(Auth.normalize(a), Auth.normalize(b)).sorted().joinToString("__")

    private fun messageRows(value: Any?): List<DirectMessage> {
        val rows = value as? JSONObject ?: return emptyList()
        return rows.keys().asSequence().mapNotNull { id ->
            val row = rows.optJSONObject(id) ?: return@mapNotNull null
            if (row.optBoolean("deleted")) return@mapNotNull null
            DirectMessage(
                id = id,
                from = Auth.normalize(row.optString("from")),
                to = Auth.normalize(row.optString("to")),
                type = row.optString("type").ifBlank { "text" },
                text = row.optString("text"),
                at = row.optLong("at")
            )
        }.sortedWith(compareBy<DirectMessage> { it.at }.thenBy { it.id }).toList()
    }

    private fun lastReadAt(me: String, peer: String): Long =
        (Firebase.get("/chatMeta/${conversationId(me, peer)}/read/${Firebase.enc(Auth.normalize(me))}") as? Number)
            ?.toLong() ?: 0L

    private fun preview(me: String, peer: ChatPerson): ConversationPreview {
        val messages = messageRows(Firebase.get("/chats/${conversationId(me, peer.username)}/messages"))
        val readAt = lastReadAt(me, peer.username)
        val unread = messages.count { it.from == peer.username && it.at > readAt }
        return ConversationPreview(peer, messages.lastOrNull(), unread)
    }

    /** Load discussion previews concurrently without blocking the UI thread. */
    suspend fun discussions(me: String): List<ConversationPreview> {
        val directory = withContext(Dispatchers.IO) { people(me) }
        return coroutineScope {
            directory.map { peer -> async(Dispatchers.IO) { preview(me, peer) } }
                .awaitAll()
                .sortedWith(
                    compareByDescending<ConversationPreview> { it.lastMessage?.at ?: 0L }
                        .thenBy { it.person.displayName.lowercase() }
                )
        }
    }

    fun snapshot(me: String, peer: String): DirectChatSnapshot {
        val id = conversationId(me, peer)
        val messages = messageRows(Firebase.get("/chats/$id/messages"))
        val presence = Firebase.get("/presence/${Firebase.enc(peer)}") as? JSONObject
        val presenceAt = presence?.optLong("at") ?: 0L
        val typing = (Firebase.get("/chats/$id/typing/${Firebase.enc(peer)}") as? Number)?.toLong() ?: 0L
        return DirectChatSnapshot(
            messages = messages,
            otherReadAt = lastReadAt(peer, me),
            otherTypingAt = typing,
            otherOnline = presence?.optBoolean("online") == true &&
                presenceAt > 0 && System.currentTimeMillis() - presenceAt < 70_000L
        )
    }

    fun markRead(me: String, peer: String): Boolean = Firebase.put(
        "/chatMeta/${conversationId(me, peer)}/read/${Firebase.enc(Auth.normalize(me))}",
        System.currentTimeMillis()
    )

    fun setTyping(me: String, peer: String, typing: Boolean): Boolean = Firebase.put(
        "/chats/${conversationId(me, peer)}/typing/${Firebase.enc(Auth.normalize(me))}",
        if (typing) System.currentTimeMillis() else 0L
    )

    fun refreshPresence(me: String): Boolean = Firebase.put(
        "/presence/${Firebase.enc(Auth.normalize(me))}",
        JSONObject().put("online", true).put("at", System.currentTimeMillis())
    )

    fun send(me: String, peer: ChatPerson, type: String, rawText: String): Boolean {
        val text = rawText.trim().take(2_000)
        if (text.isEmpty()) return false
        val now = System.currentTimeMillis()
        val msg = JSONObject()
            .put("from", Auth.normalize(me))
            .put("to", peer.username)
            .put("type", type)
            .put("text", text)
            .put("at", now)
        val id = UUID.randomUUID().toString().replace("-", "")
        val path = "/chats/${conversationId(me, peer.username)}/messages/${Firebase.enc(id)}"
        if (!Firebase.put(path, msg)) return false

        val body = when (type) {
            "sticker" -> "$text  (sticker)"
            "math" -> "📐 sent an equation"
            else -> if (text.length > 90) text.take(90) + "…" else text
        }
        Firebase.post(
            "/notifications/${Firebase.enc(peer.username)}",
            JSONObject()
                .put("type", "chat")
                .put("title", "💬 New message from $me")
                .put("body", body)
                .put("at", now)
                .put("from", Auth.normalize(me))
                .put("url", "/chat")
        )
        setTyping(me, peer.username, false)
        return true
    }

    fun deleteOwnMessage(me: String, peer: String, message: DirectMessage): Boolean {
        if (message.from != Auth.normalize(me)) return false
        return Firebase.patch(
            "/chats/${conversationId(me, peer)}/messages/${Firebase.enc(message.id)}",
            JSONObject().put("deleted", true).put("text", "")
        )
    }
}
