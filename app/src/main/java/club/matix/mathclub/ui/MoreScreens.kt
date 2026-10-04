package club.matix.mathclub.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.Firebase
import club.matix.mathclub.data.ImageCodec
import club.matix.mathclub.data.Points
import club.matix.mathclub.data.PointsData
import club.matix.mathclub.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    "quiz_result" to TypeMeta("🎯", Color(0xFF10B981), "Quiz completed"),
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

private class Event(
    val type: String, val user: String?, val at: Long, val title: String, val body: String,
    val raw: JSONObject? = null
)

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
    // Do not read /members records here; they also contain credential fields.
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
    // Join timestamps are embedded in the private member records, so don't read
    // the full /members object just to populate this optional feed event.
    comments.keys().forEach { k ->
        val c = comments.optJSONObject(k) ?: return@forEach
        val u = norm(c.optString("by")); if (u == me) return@forEach
        out += Event("comment", u, c.optLong("at"), "", c.optString("text"))
    }
    stored.keys().forEach { k ->
        val s = stored.optJSONObject(k) ?: return@forEach
        out += Event(
            s.optString("type", "role"),
            (s.optString("user").ifEmpty { s.optString("targetUser") }).takeIf { it.isNotEmpty() },
            s.optLong("at"), s.optString("title", "Notification"), s.optString("body"),
            JSONObject(s.toString()).put("notifKey", k)
        )
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
fun MessagesScreen(store: Store, me: String, isOwner: Boolean) {
    var events by remember { mutableStateOf<List<Event>?>(null) }
    var seen by remember { mutableStateOf(store.seenAt(me)) }
    var chip by remember { mutableStateOf("all") }
    var prefsTick by remember { mutableStateOf(0) }
    var showPrefs by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var requests by remember { mutableStateOf<List<Pair<String, JSONObject>>>(emptyList()) }
    var canApprove by remember { mutableStateOf(isOwner) }
    var approvalError by remember { mutableStateOf<String?>(null) }
    var busyRequest by remember { mutableStateOf<String?>(null) }
    var awardEvent by remember { mutableStateOf<Event?>(null) }
    var awardAmount by remember { mutableStateOf("15") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(me, isOwner, reload) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { loadEvents(me) } to runCatching { Points.load() }
        }
        events = loaded.first.getOrDefault(emptyList())
        loaded.second.getOrNull()?.let { d ->
            canApprove = isOwner || d.role(me) == "manager"
            requests = if (canApprove) d.pending() else emptyList()
        } ?: run { requests = emptyList() }
        if (reload == 0) store.setSeenAt(me, System.currentTimeMillis())
    }
    LaunchedEffect(me, isOwner) {
        while (isActive) {
            delay(30_000)
            reload++
        }
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
        if (canApprove && requests.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🛡 Requests awaiting approval", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                requests.forEach { (id, req) ->
                    val by = req.optString("requestedBy", "?")
                    val target = req.optString("targetUser", by)
                    val reward = req.optString("reward")
                    val amount = req.opt("amount")
                    val requestLabel = if (reward.isNotBlank()) "$by requested “$reward” for $target."
                    else if (amount == "reset") "$by requested a points reset for @$target."
                    else "$by requested ${if ((amount as? Number)?.toInt()?.let { it >= 0 } == true) "+" else ""}${amount ?: "?"} points for @$target."
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(requestLabel, fontWeight = FontWeight.SemiBold)
                            Text(timeAgo(req.optLong("timestamp")), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = busyRequest == null, onClick = {
                                    busyRequest = id; approvalError = null
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val fresh = Points.load()
                                                Points.decide(fresh, me, id, req, true)
                                            }.getOrDefault(false)
                                        }
                                        busyRequest = null
                                        if (result) reload++ else approvalError = "Couldn't approve that request. Refresh and try again."
                                    }
                                }) { Text(if (busyRequest == id) "Saving…" else "Approve") }
                                OutlinedButton(enabled = busyRequest == null, onClick = {
                                    busyRequest = id; approvalError = null
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val fresh = Points.load()
                                                Points.decide(fresh, me, id, req, false)
                                            }.getOrDefault(false)
                                        }
                                        busyRequest = null
                                        if (result) reload++ else approvalError = "Couldn't deny that request. Refresh and try again."
                                    }
                                }) { Text(if (busyRequest == id) "Saving…" else "Deny") }
                            }
                        }
                    }
                }
                approvalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        val l = events
        when {
            l == null -> CircularProgressIndicator()
            shown.isEmpty() -> Text("✨ Nothing here yet.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown.take(150)) { e ->
                    EventRow(e, e.at > seen, canApprove && e.type == "quiz_result") {
                        awardEvent = e
                        awardAmount = "15"
                    }
                }
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
    awardEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { if (busyRequest == null) awardEvent = null },
            title = { Text("Award quiz points") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Give points to @${event.user ?: "member"} for completing “${event.raw?.optString("topic").orEmpty().ifBlank { "Math Learn" }}” (${event.raw?.optString("score").orEmpty().ifBlank { "quiz complete" }}).")
                    OutlinedTextField(
                        awardAmount, { awardAmount = it.filter { ch -> ch.isDigit() || ch == '-' }.take(5) },
                        label = { Text("Points to award") }, singleLine = true
                    )
                    approvalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(enabled = busyRequest == null && awardAmount.toIntOrNull() != null && event.user != null, onClick = {
                    val target = event.user.orEmpty()
                    val amount = awardAmount.toIntOrNull() ?: 0
                    busyRequest = "award"; approvalError = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val data = Points.load()
                                Points.change(data, me, isOwner, target, amount)
                            }.getOrElse { it.message ?: "Couldn't award points." }
                        }
                        busyRequest = null
                        if (result.contains("now on")) {
                            event.raw?.optString("notifKey")?.takeIf { it.isNotBlank() }?.let { key ->
                                withContext(Dispatchers.IO) { Firebase.delete("/notifications/${Firebase.enc(me)}/${Firebase.enc(key)}") }
                            }
                            awardEvent = null
                            reload++
                        } else approvalError = result
                    }
                }) { Text(if (busyRequest == "award") "Saving…" else "Award points") }
            },
            dismissButton = {
                TextButton(enabled = busyRequest == null, onClick = { awardEvent = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
internal fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun EventRow(e: Event, isNew: Boolean, showAward: Boolean = false, onAward: () -> Unit = {}) {
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
                if (showAward && e.user != null) {
                    TextButton(onClick = onAward) { Text("⭐ Award points") }
                }
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
    var photo by remember { mutableStateOf("") }
    var photoError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { ImageCodec.encodeJpegDataUri(context, uri) } }
            result.onSuccess { photo = it; photoError = null }
                .onFailure { photoError = it.message ?: "Couldn't load that picture." }
        }
    }

    LaunchedEffect(me) {
        val pr = withContext(Dispatchers.IO) { Firebase.get("/profiles/${Firebase.enc(me)}") as? JSONObject } ?: JSONObject()
        name = pr.optString("displayName"); bio = pr.optString("bio")
        emoji = pr.optString("emoji"); color = pr.optString("color").ifEmpty { "#5B46C9" }
        photo = pr.optString("profileImage").ifEmpty { pr.optString("photo") }
        loaded = true
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🎨 My profile") },
        text = {
            if (!loaded) CircularProgressIndicator() else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This is what the club sees on the Users page and next to your name.", fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(color))), contentAlignment = Alignment.Center) {
                        if (photo.isNotBlank()) DataUriImage(photo, Modifier.fillMaxSize(), "Profile photo")
                        else Text(emoji.ifBlank { name.firstOrNull()?.uppercase() ?: "?" }, fontSize = 30.sp, color = Color.White)
                    }
                    Column {
                        Text("Profile picture", fontWeight = FontWeight.Bold)
                        Row {
                            TextButton(onClick = { photoPicker.launch("image/*") }) { Text("Choose photo") }
                            if (photo.isNotBlank()) TextButton(onClick = { photo = ""; photoError = null }) { Text("Remove") }
                        }
                    }
                }
                photoError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
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
                            .put("photo", if (photo.isEmpty()) JSONObject.NULL else photo)
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
