package club.matix.mathclub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.Firebase
import club.matix.mathclub.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/* ------------------------------------------------------------------ */
/* Messages (notifications feed) - port of renderMessages/buildMsg     */
/* ------------------------------------------------------------------ */

private class TypeMeta(val icon: String, val color: Color, val label: String)

private val TypeMetas = linkedMapOf(
    "follower" to TypeMeta("➕", Color(0xFF38BDF8), "New follower"),
    "joined" to TypeMeta("🎉", Color(0xFF34D399), "New member joined"),
    "role" to TypeMeta("🎖", Color(0xFFFBBF24), "Role change"),
    "changelog" to TypeMeta("📜", Color(0xFF60A5FA), "Changelog update"),
    "comment" to TypeMeta("💬", Color(0xFFA78BFA), "Profile comment"),
    "points" to TypeMeta("⭐", Color(0xFFF59E0B), "Points"),
    "meme" to TypeMeta("😂", Color(0xFFFACC15), "Joke / meme"),
    "lesson" to TypeMeta("📚", Color(0xFF22D3EE), "New lesson"),
    "deleted" to TypeMeta("🗑", Color(0xFFF87171), "User removed"),
    "points_request" to TypeMeta("📊", Color(0xFFF59E0B), "Point requests to approve"),
    "approval_request" to TypeMeta("📝", Color(0xFFFACC15), "Requests to approve"),
    "chat" to TypeMeta("💬", Color(0xFF00A884), "Chat message")
)

private class Chip(val id: String, val label: String, val types: Set<String>?)

private val Chips = listOf(
    Chip("all", "All", null),
    Chip("follower", "Follows", setOf("follower")),
    Chip("joined", "Members", setOf("joined")),
    Chip("comment", "Comments", setOf("comment")),
    Chip("role", "Roles", setOf("role", "approval_request")),
    Chip("points", "Points", setOf("points", "points_request")),
    Chip("meme", "Memes", setOf("meme")),
    Chip("lesson", "Lessons", setOf("lesson"))
)

private class Event(val type: String, val user: String?, val at: Long, val title: String, val body: String)

fun timeAgo(ms: Long): String {
    if (ms <= 0) return ""
    val s = (System.currentTimeMillis() - ms) / 1000
    if (s < 60) return "just now"
    val m = s / 60
    if (m < 60) return "${m}m ago"
    val h = m / 60
    if (h < 24) return "${h}h ago"
    val d = h / 24
    return if (d < 30) "${d}d ago" else java.text.DateFormat.getDateInstance().format(java.util.Date(ms))
}

private fun JSONObject.objs(key: String): JSONObject = optJSONObject(key) ?: JSONObject()

private fun loadEvents(me: String): List<Event> {
    fun obj(path: String) = Firebase.get(path) as? JSONObject ?: JSONObject()
    val enc = Firebase.enc(me)
    val follows = obj("/follows/$enc")
    val members = obj("/members")
    val comments = obj("/profile_comments/$enc")
    val stored = obj("/notifications/$enc")
    val changelog = obj("/changelog")
    val memes = obj("/funny_memes")
    val lessons = obj("/lessons")
    val dels = obj("/deletions")
    val out = mutableListOf<Event>()
    fun norm(s: String?) = (s ?: "").lowercase().replace(Regex("\\s+"), "")

    follows.keys().forEach { k ->
        val u = norm(k); if (u.isEmpty() || u == me) return@forEach
        out += Event("follower", u, follows.optJSONObject(k)?.optLong("at") ?: 0L, "", "")
    }
    members.keys().forEach { k ->
        val u = norm(k); if (u.isEmpty() || u == me) return@forEach
        val jt = members.optJSONObject(k)?.optLong("joinedAt") ?: 0L
        if (jt > 0) out += Event("joined", u, jt, "", "")
    }
    comments.keys().forEach { k ->
        val c = comments.optJSONObject(k) ?: return@forEach
        val u = norm(c.optString("by")); if (u == me) return@forEach
        out += Event("comment", u, c.optLong("at"), "", c.optString("text"))
    }
    stored.keys().forEach { k ->
        val s = stored.optJSONObject(k) ?: return@forEach
        out += Event(s.optString("type", "role"), s.optString("user").takeIf { it.isNotEmpty() }, s.optLong("at"),
            s.optString("title", "Notification"), s.optString("body"))
    }
    changelog.keys().forEach { k ->
        val c = changelog.optJSONObject(k) ?: return@forEach
        out += Event("changelog", null, c.optLong("at"), "📜 " + c.optString("title", "Changelog update"), c.optString("description"))
    }
    memes.keys().forEach { k ->
        val m = memes.optJSONObject(k) ?: return@forEach
        if (norm(m.optString("postedBy")) == me) return@forEach
        val t = m.optString("title")
        out += Event("meme", null, m.optLong("createdAt"), "😂 New joke / meme",
            (if (t.isNotEmpty()) "“$t”" else "A new meme was posted") + " — @" + m.optString("postedBy", "?"))
    }
    lessons.keys().forEach { k ->
        val l = lessons.optJSONObject(k) ?: return@forEach
        if (norm(l.optString("createdBy")) == me) return@forEach
        out += Event("lesson", null, l.optLong("createdAt"), "📚 New lesson",
            l.optString("title", "Untitled") + " — by @" + l.optString("createdBy", "ghadi"))
    }
    dels.keys().forEach { k ->
        val d = dels.optJSONObject(k) ?: return@forEach
        val u = norm(k); if (u == me) return@forEach
        val by = d.optString("by")
        out += Event("deleted", u, d.optLong("at"), "🗑 User removed", "@$u was removed" + if (by.isNotEmpty()) " by @$by" else "")
    }
    return out.sortedByDescending { it.at }
}

@Composable
fun MessagesScreen(store: Store, me: String) {
    var events by remember { mutableStateOf<List<Event>?>(null) }
    var seen by remember { mutableStateOf(store.seenAt(me)) }
    var chip by remember { mutableStateOf("all") }
    var prefsTick by remember { mutableStateOf(0) }
    var showPrefs by remember { mutableStateOf(false) }

    LaunchedEffect(me) {
        events = withContext(Dispatchers.IO) { loadEvents(me) }
        store.setSeenAt(me, System.currentTimeMillis())
    }

    val visible = remember(events, prefsTick) { events?.filter { store.notifEnabled(me, it.type) } ?: emptyList() }
    val newCount = visible.count { it.at > seen }
    val sel = Chips.first { it.id == chip }
    val shown = visible.filter { sel.types == null || it.type in sel.types }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔔 Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            if (newCount > 0) "You have $newCount new notification${if (newCount == 1) "" else "s"}." else "All caught up — nothing new.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.padding(vertical = 8.dp).horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chips.forEach { c ->
                val cnt = visible.count { c.types == null || it.type in c.types }
                FilterChip(selected = chip == c.id, onClick = { chip = c.id }, label = { Text(c.label + if (cnt > 0) " $cnt" else "") })
            }
        }
        val l = events
        when {
            l == null -> CircularProgressIndicator()
            shown.isEmpty() -> Text("✨ Nothing here yet.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown.take(150)) { e -> EventRow(e, e.at > seen) }
            }
        }
        TextButton(onClick = { showPrefs = !showPrefs }) { Text("⚙ Alert settings") }
        if (showPrefs) Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            TypeMetas.forEach { (k, meta) ->
                var on by remember(prefsTick, k) { mutableStateOf(store.notifEnabled(me, k)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${meta.icon}  ${meta.label}", Modifier.weight(1f))
                    Switch(on, { on = it; store.setNotifEnabled(me, k, it); prefsTick++ })
                }
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun EventRow(e: Event, isNew: Boolean) {
    val meta = TypeMetas[e.type] ?: TypeMeta("🔔", Color(0xFF3B82F6), "")
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(meta.color), contentAlignment = Alignment.Center) {
                Text(if (e.user != null) initials(e.user) else meta.icon, color = Color.White, fontSize = if (e.user != null) 14.sp else 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                val line = when (e.type) {
                    "follower" -> "@${e.user} started following you."
                    "joined" -> "@${e.user} joined Matix."
                    "comment" -> "@${e.user} commented on your profile."
                    else -> e.title.ifEmpty { "Notification" }
                }
                Text(line + if (isNew) "  NEW" else "", fontWeight = FontWeight.SemiBold)
                if (e.body.isNotEmpty()) Text(e.body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeAgo(e.at), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun initials(name: String): String = name.trim().take(2).uppercase().ifEmpty { "?" }

/* ------------------------------------------------------------------ */
/* Profile editor - port of openProfileModal (text fields, emoji, color) */
/* ------------------------------------------------------------------ */

private val ProfileEmojis = listOf("😀", "😎", "🤓", "🦉", "🦊", "🐼", "🐯", "🐸", "🚀", "🌟", "🎮", "🎨", "🧠", "➕", "✖", "📐", "🍕", "⚽")
private val ProfileColors = listOf("#5B46C9", "#0284C7", "#16A34A", "#E11D48", "#7C3AED", "#DB2777", "#0D9488", "#F7A600")

@Composable
fun ProfileDialog(me: String, onClose: () -> Unit) {
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#5B46C9") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(me) {
        val pr = withContext(Dispatchers.IO) { Firebase.get("/profiles/${Firebase.enc(me)}") as? JSONObject } ?: JSONObject()
        name = pr.optString("displayName"); bio = pr.optString("bio")
        emoji = pr.optString("emoji"); color = pr.optString("color").ifEmpty { "#5B46C9" }
        loaded = true
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🎨 My profile") },
        text = {
            if (!loaded) CircularProgressIndicator() else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This is what the club sees on the Users page and next to your name.", fontSize = 13.sp)
                OutlinedTextField(name, { if (it.length <= 28) name = it }, label = { Text("Display name") }, placeholder = { Text(me) }, singleLine = true)
                OutlinedTextField(bio, { if (it.length <= 180) bio = it }, label = { Text("About you") })
                Text("Avatar emoji", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProfileEmojis.forEach { e ->
                        FilterChip(selected = emoji == e, onClick = { emoji = if (emoji == e) "" else e }, label = { Text(e) })
                    }
                }
                Text("Favourite colour", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileColors.forEach { c ->
                        Box(
                            Modifier.size(if (c.equals(color, true)) 36.dp else 28.dp).clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(c))).clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = loaded && !saving, onClick = {
                saving = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val d = JSONObject().put("displayName", name.trim().ifEmpty { me }).put("bio", bio.trim())
                            .put("emoji", if (emoji.isEmpty()) JSONObject.NULL else emoji).put("color", color)
                            .put("updatedAt", System.currentTimeMillis())
                        Firebase.patch("/profiles/${Firebase.enc(me)}", d)
                    }
                    onClose()
                }
            }) { Text(if (saving) "Saving…" else "Save my profile") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

/* ------------------------------------------------------------------ */
/* Colour theme picker - port of the Theme page                        */
/* ------------------------------------------------------------------ */

@Composable
fun ThemePicker(current: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("🎨 Colour theme", fontWeight = FontWeight.Bold)
        Text("Repaint Matix in your own colours.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ThemePresets.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (p.id == current) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .clickable { onPick(p.id) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            listOf(p.brand, p.brand2, p.accent).forEach { Box(Modifier.size(16.dp).clip(CircleShape).background(it)) }
                        }
                        Text(p.name, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
