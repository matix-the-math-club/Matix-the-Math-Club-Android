package club.matix.mathclub.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import club.matix.mathclub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Owners = setOf("ghadi")

fun isOwnerUser(me: String, role: String?) = role == "owner" || me in Owners

private fun JSONObject.keyList(): List<String> = keys().asSequence().toList()

/* ------------------------------------------------------------------ */
/* Changelog                                                           */
/* ------------------------------------------------------------------ */

private class ChangeItem(val id: String, val title: String, val desc: String, val by: String, val at: Long)

@Composable
fun ChangelogScreen(me: String, isOwner: Boolean) {
    var items by remember { mutableStateOf<List<ChangeItem>?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tick) {
        items = withContext(Dispatchers.IO) {
            val o = Firebase.get("/changelog") as? JSONObject
            o?.keyList()?.mapNotNull { k ->
                o.optJSONObject(k)?.let { ChangeItem(k, it.optString("title"), it.optString("description"), it.optString("by", "ghadi"), it.optLong("at")) }
            }?.sortedByDescending { it.at } ?: emptyList()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("📝 Changelog", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Updates show up in everyone's Inbox.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isOwner) {
            OutlinedTextField(title, { if (it.length <= 80) title = it }, label = { Text("What changed?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(desc, { if (it.length <= 400) desc = it }, label = { Text("Details…") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = title.isNotBlank(), onClick = {
                val t = title.trim(); val d = desc.trim(); title = ""; desc = ""
                scope.launch {
                    withContext(Dispatchers.IO) { Firebase.post("/changelog", JSONObject().put("title", t).put("description", d).put("by", me).put("at", System.currentTimeMillis())) }
                    tick++
                }
            }) { Text("Post to changelog") }
        }
        Spacer(Modifier.height(8.dp))
        val l = items
        if (l == null) CircularProgressIndicator()
        else if (l.isEmpty()) Text("No changelog entries yet.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(l) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row { Text(c.title, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(timeAgo(c.at), fontSize = 12.sp) }
                        if (c.desc.isNotEmpty()) Text(c.desc)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("by @${c.by}", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isOwner) TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { Firebase.delete("/changelog/${c.id}") }; tick++ } }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Bugs                                                                */
/* ------------------------------------------------------------------ */

private class Bug(val id: String, val title: String, val text: String, val by: String, val at: Long, val status: String, val votes: Int, val voted: Boolean, val replies: List<Triple<String, String, Long>>)

@Composable
fun BugsScreen(me: String, isOwner: Boolean) {
    var bugs by remember { mutableStateOf<List<Bug>?>(null) }
    var filter by remember { mutableStateOf("all") }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var replyFor by remember { mutableStateOf<String?>(null) }
    var reply by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tick) {
        bugs = withContext(Dispatchers.IO) {
            val o = Firebase.get("/bugs") as? JSONObject
            o?.keyList()?.mapNotNull { k ->
                val v = o.optJSONObject(k) ?: return@mapNotNull null
                val reps = v.optJSONObject("replies")?.let { r -> r.keyList().mapNotNull { rk -> r.optJSONObject(rk)?.let { Triple(it.optString("by", "?"), it.optString("text"), it.optLong("at")) } } }?.sortedBy { it.third } ?: emptyList()
                Bug(k, v.optString("title", "Untitled"), v.optString("text"), v.optString("by", "?"), v.optLong("at"), v.optString("status", "open"),
                    v.optInt("votes"), v.optJSONObject("voters")?.has(me) == true, reps)
            } ?: emptyList()
        }
    }
    fun act(block: () -> Unit) = scope.launch { withContext(Dispatchers.IO) { block() }; tick++ }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🐞 Bugs", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Something broken? Report it here. Everyone can post and reply.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(title, { title = it }, label = { Text("Short summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(text, { text = it }, label = { Text("What happened?") }, modifier = Modifier.fillMaxWidth())
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            if (title.isBlank()) { err = "Give it a short summary first."; return@Button }
            err = ""; val t = title.trim(); val d = text.trim(); title = ""; text = ""
            act { Firebase.post("/bugs", JSONObject().put("title", t).put("text", d).put("by", me).put("at", System.currentTimeMillis()).put("status", "open").put("votes", 1).put("voters", JSONObject().put(me, true))) }
        }) { Text("Report it") }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "All", "open" to "Open", "fixed" to "Fixed").forEach { (id, l) -> FilterChip(filter == id, { filter = id }, label = { Text(l) }) }
        }
        val l = bugs?.filter { filter == "all" || it.status == filter }?.sortedWith(compareBy<Bug> { if (it.status == "open") 0 else 1 }.thenByDescending { it.votes }.thenByDescending { it.at })
        if (l == null) CircularProgressIndicator()
        else if (l.isEmpty()) Text(if (filter == "all") "🎉 No bugs reported — nice!" else "Nothing here.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(l) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TextButton(enabled = !b.voted, onClick = {
                                act {
                                    Firebase.put("/bugs/${b.id}/voters/${Firebase.enc(me)}", true)
                                    Firebase.patch("/bugs/${b.id}", JSONObject().put("votes", b.votes + 1))
                                }
                            }) { Text("▲") }
                            Text("${b.votes}", fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Row { Text(b.title, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(if (b.status == "open") "Open" else "Fixed", fontSize = 12.sp, color = if (b.status == "open") androidx.compose.ui.graphics.Color(0xFFE0762B) else androidx.compose.ui.graphics.Color(0xFF1F9D55)) }
                            if (b.text.isNotEmpty()) Text(b.text)
                            Text("by @${b.by} · ${timeAgo(b.at)}" + if (b.replies.isNotEmpty()) " · 💬 ${b.replies.size}" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            b.replies.forEach { (by, t, at) -> Text("@$by $t  ${timeAgo(at)}", fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp)) }
                            Row {
                                TextButton(onClick = { replyFor = if (replyFor == b.id) null else b.id; reply = "" }) { Text("💬 Reply") }
                                if (isOwner) TextButton(onClick = { act { Firebase.patch("/bugs/${b.id}", JSONObject().put("status", if (b.status == "open") "fixed" else "open")) } }) { Text(if (b.status == "open") "✅ Fixed" else "↩ Reopen") }
                                if (isOwner || b.by == me) TextButton(onClick = { act { Firebase.delete("/bugs/${b.id}") } }) { Text("Delete") }
                            }
                            if (replyFor == b.id) Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(reply, { reply = it }, Modifier.weight(1f), placeholder = { Text("Add what you know…") })
                                TextButton(enabled = reply.isNotBlank(), onClick = {
                                    val r = reply.trim(); reply = ""; replyFor = null
                                    act { Firebase.post("/bugs/${b.id}/replies", JSONObject().put("by", me).put("text", r).put("at", System.currentTimeMillis())) }
                                }) { Text("Post") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Users                                                               */
/* ------------------------------------------------------------------ */

private class UserInfo(val name: String, val role: String, val bio: String, val emoji: String, val games: Int, val points: Int, val followers: Int, val password: String?)

private fun loadUsers(isOwner: Boolean): List<UserInfo> {
    fun obj(p: String, base: String = Firebase.MAIN) = Firebase.get(p, base) as? JSONObject ?: JSONObject()
    val members = obj("/members"); val roles = obj("/roles"); val profiles = obj("/profiles")
    val games = obj("/hubprojects"); val points = Firebase.get("/points") as? JSONObject ?: JSONObject(); val follows = obj("/follows")
    val names = (members.keyList() + profiles.keyList()).map { Auth.normalize(it) }.filter { it.isNotEmpty() }.toSortedSet()
    val gameCount = HashMap<String, Int>()
    games.keyList().forEach { k -> games.optJSONObject(k)?.let { g -> val a = Auth.normalize(g.optString("author")); gameCount[a] = (gameCount[a] ?: 0) + 1 } }
    val followers = HashMap<String, Int>()
    follows.keyList().forEach { a -> follows.optJSONObject(a)?.keyList()?.forEach { b -> val n = Auth.normalize(b); followers[n] = (followers[n] ?: 0) + 1 } }
    return names.map { u ->
        val p = profiles.optJSONObject(u); val m = members.optJSONObject(u)
        val pt = points.opt(u).let { if (it is JSONObject) it.optInt("total") else (it as? Number)?.toInt() ?: 0 }
        val emoji = p?.optString("emoji").orEmpty().takeIf { it.length <= 6 && !it.contains("http") && !it.startsWith("data:") } ?: ""
        UserInfo(u, if (u in Owners) "owner" else roles.optString(u, "member"), p?.optString("bio").orEmpty(), emoji,
            gameCount[u] ?: 0, pt, followers[u] ?: 0, if (isOwner) m?.optString("password") else null)
    }.sortedWith(compareBy<UserInfo>({ if (it.name == "ghadi") 0 else 1 }, { it.name }))
}

@Composable
fun UsersScreen(isOwner: Boolean) {
    var users by remember { mutableStateOf<List<UserInfo>?>(null) }
    var q by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("az") }
    LaunchedEffect(Unit) { users = withContext(Dispatchers.IO) { runCatching { loadUsers(isOwner) }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("👥 Users", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Browse everyone in the Matix club" + if (isOwner) " — passwords are only visible to you, the owner." else ".", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val all = users
        if (all == null) { CircularProgressIndicator(); return@Column }
        Text("${all.size} members · ${all.count { it.role == "owner" }} owner · ${all.sumOf { it.games }} games made", fontSize = 13.sp)
        OutlinedTextField(q, { q = it }, placeholder = { Text("🔍 Search users…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("az" to "A–Z", "games" to "Most games", "points" to "Top points").forEach { (id, l) -> FilterChip(sort == id, { sort = id }, label = { Text(l) }) }
        }
        val vis = all.filter { q.isBlank() || it.name.contains(q, true) || it.bio.contains(q, true) }.let {
            when (sort) { "games" -> it.sortedByDescending { u -> u.games }; "points" -> it.sortedByDescending { u -> u.points }; else -> it }
        }
        if (vis.isEmpty()) Text("🔍 No users match “$q”.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vis) { u ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text((u.emoji.ifEmpty { initials(u.name) }) + "  @${u.name}  · ${u.role}", fontWeight = FontWeight.Bold)
                        if (u.bio.isNotEmpty()) Text(u.bio, fontSize = 13.sp)
                        Text("🎮 ${u.games}   ⭐ ${u.points}   👥 ${u.followers}", fontSize = 13.sp)
                        if (isOwner) Text(if (!u.password.isNullOrEmpty()) "🔑 ${u.password}" else "🔒 none", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Translator                                                          */
/* ------------------------------------------------------------------ */

private val TrLangs = listOf("Arabic", "English", "French", "Spanish", "German", "Turkish", "Italian", "Portuguese", "Russian", "Japanese", "Korean", "Chinese (Simplified)", "Hindi", "Dutch", "Polish", "Ukrainian")

@Composable
fun TranslatorScreen(store: Store) {
    var from by remember { mutableStateOf("Auto-detect") }
    var to by remember { mutableStateOf("English") }
    var input by remember { mutableStateOf("") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🌐 Translator", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Type anything and Matix AI will translate it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LangPicker("From", from, listOf("Auto-detect") + TrLangs, Modifier.weight(1f)) { from = it }
            TextButton(onClick = { if (from != "Auto-detect") { val t = from; from = to; to = t } }) { Text("⇄") }
            LangPicker("To", to, TrLangs, Modifier.weight(1f)) { to = it }
        }
        OutlinedTextField(input, { input = it }, label = { Text("Type or paste text here…") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && input.isNotBlank(), onClick = {
            if (store.aiKey.isBlank()) { out = "No AI key is set up yet — add one in Settings."; return@Button }
            busy = true; out = "Translating…"
            val sys = "You are a precise translator. Translate the text into $to" + (if (from == "Auto-detect") "" else " (the source text is in $from)") +
                ". Reply with ONLY the translation - no quotes, no notes, no explanations. Keep numbers, emoji and line breaks as they are."
            scope.launch {
                out = withContext(Dispatchers.IO) { Ai.complete(store.aiKey, sys, listOf(ChatMsg(true, input.trim()))) }
                busy = false
            }
        }) { Text("Translate") }
        if (out.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text(out, Modifier.padding(12.dp)) }
            TextButton(onClick = {
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("translation", out))
            }) { Text("📋 Copy") }
        }
        Text("✨ Powered by Matix AI", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LangPicker(label: String, value: String, options: List<String>, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) { Text("$label: $value", maxLines = 1, fontSize = 12.sp) }
        DropdownMenu(open, { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onPick(o); open = false }) }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Game builder: AI / code / upload-less (HTML paste)                  */
/* ------------------------------------------------------------------ */

private const val SKELETON = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<title>My Matix Game</title>\n<style>\n  body{font-family:sans-serif;text-align:center;background:#5b46c9;color:#fff;padding:24px}\n  button{font-size:1.2rem;padding:12px 18px;border:none;border-radius:12px;cursor:pointer;font-weight:700}\n</style>\n</head>\n<body>\n  <h1>My Matix Game</h1>\n  <p>Score: <b id=\"score\">0</b></p>\n  <button onclick=\"add()\">Click me! 🎉</button>\n  <script>\n    var s=0;\n    function add(){s++;document.getElementById(\"score\").textContent=s;}\n  </script>\n</body>\n</html>"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlPreview(html: String, modifier: Modifier = Modifier) {
    AndroidView(modifier, factory = { c ->
        WebView(c).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
    }, update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) })
}

@Composable
fun GameBuilderScreen(me: String, existingId: String?, onDone: () -> Unit) {
    var mode by remember { mutableStateOf("ai") }
    var prompt by remember { mutableStateOf("") }
    var html by remember { mutableStateOf(SKELETON) }
    var generated by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var madeWithAi by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(existingId) {
        if (existingId != null) {
            withContext(Dispatchers.IO) { Firebase.get("/hubprojects/$existingId") as? JSONObject }?.let {
                html = it.optString("html", SKELETON); name = it.optString("name"); mode = "code"
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text("← Back") }
            Text(if (existingId != null) "✏ Edit game" else "✨ New game", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (existingId == null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(mode == "ai", { mode = "ai" }, label = { Text("✨ Make with AI") })
            FilterChip(mode == "code", { mode = "code" }, label = { Text("</> Code") })
        }
        if (mode == "ai") {
            OutlinedTextField(prompt, { prompt = it }, label = { Text("Describe what you want to make") }, placeholder = { Text("e.g. a balloon-pop addition game") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy, onClick = {
                if (prompt.isBlank()) { err = "Describe what to make first."; return@Button }
                err = ""; busy = true
                scope.launch {
                    val r = withContext(Dispatchers.IO) { Ai.generateGameHtml(prompt.trim()) }
                    busy = false
                    if (r != null) { html = r; generated = true; madeWithAi = true; preview = true; if (name.isBlank()) name = Ai.titleOf(r).ifEmpty { "Matix Game" } }
                    else err = "Matix AI is busy right now. Try the Code tab instead."
                }
            }) { Text(if (busy) "✨ Generating…" else "✨ Generate with AI") }
        } else {
            OutlinedTextField(html, { html = it }, label = { Text("index.html") }, minLines = 10, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { preview = true }) { Text("▶ Preview") }
                OutlinedButton(onClick = { html = SKELETON; preview = true }) { Text("Reset") }
            }
        }
        if (preview && html.isNotBlank()) HtmlPreview(html, Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)))
        if (mode == "code" || generated) {
            OutlinedTextField(name, { name = it }, label = { Text("Project name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                pw, { pw = it }, label = { Text("Confirm your password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
            )
            Button(enabled = !busy, onClick = {
                when {
                    html.isBlank() -> { err = "Write some code first."; return@Button }
                    name.isBlank() -> { err = "Enter a name."; return@Button }
                    pw.isEmpty() -> { err = "Retype your password."; return@Button }
                }
                err = ""; busy = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        if (Auth.signIn(me, pw) != SignInResult.OK) false
                        else if (existingId != null) Firebase.patch("/hubprojects/$existingId", JSONObject().put("name", name.trim()).put("html", html))
                        else Firebase.post("/hubprojects", JSONObject().put("name", name.trim()).put("author", me).put("html", html).put("thumb", "").put("loves", 0)
                            .put("createdAt", System.currentTimeMillis()).put("madeWithAI", madeWithAi).put("published", true))
                    }
                    busy = false
                    if (ok) onDone() else err = "Password incorrect or you're offline."
                }
            }) { Text(if (busy) "Publishing…" else if (existingId != null) "Save changes" else "Publish") }
        }
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
    }
}

/* ------------------------------------------------------------------ */
/* Labs: home hub + "learn anything" AI lessons                        */
/* ------------------------------------------------------------------ */

@Composable
fun LabsScreen(store: Store, me: String, isOwner: Boolean, go: (String) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var lesson by remember { mutableStateOf<Pair<String, List<Exercise>>?>(null) }
    val scope = rememberCoroutineScope()

    lesson?.let { (t, ex) ->
        QuizScreen(t, ex, false, null, me, null) { lesson = null }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("MATIX LABS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Hi @$me 👋", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Free math education — learn, play, create.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🧪 Learn anything with AI", fontWeight = FontWeight.Bold)
                Text("Type a topic and Matix AI builds a short quiz.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(topic, { topic = it }, placeholder = { Text("e.g. adding fractions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && topic.isNotBlank(), onClick = {
                    if (store.aiKey.isBlank()) { err = "Add an AI key in Settings first."; return@Button }
                    err = ""; busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { runCatching { aiLesson(store.aiKey, topic.trim()) } }
                        busy = false
                        r.onSuccess { lesson = it }.onFailure { err = "Couldn't build that lesson: ${it.message}" }
                    }
                }) { Text(if (busy) "Building…" else "Start") }
                if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
        listOf("learn" to "🦉 Math Learn" , "games" to "🎮 Games Hub", "chat" to "💬 Matix AI", "ideas" to "💡 Ideas", "translator" to "🌐 Translator").forEach { (id, l) ->
            Card(Modifier.fillMaxWidth().clickable { go(id) }) { Text(l, Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun aiLesson(key: String, topic: String): Pair<String, List<Exercise>> {
    val prompt = "Make a short math quiz about: $topic. Reply with ONLY JSON of the form " +
        "{\"title\":\"...\",\"ex\":[{\"type\":\"mc\",\"prompt\":\"...\",\"q\":\"...\",\"choices\":[\"a\",\"b\",\"c\",\"d\"],\"answer\":\"a\",\"hint\":\"...\"}," +
        "{\"type\":\"input\",\"prompt\":\"...\",\"q\":\"...\",\"answer\":\"...\"},{\"type\":\"tf\",\"prompt\":\"True or false?\",\"q\":\"...\",\"choices\":[\"True\",\"False\"],\"answer\":\"True\"}]}. " +
        "Use 6 questions mixing mc, tf and input. The answer of mc/tf must equal one choice exactly."
    val txt = Ai.complete(key, "You output strict JSON only.", listOf(ChatMsg(true, prompt)))
        .replace("```json", "").replace("```", "").trim()
    if (txt.startsWith("⚠")) throw IllegalStateException(txt.removePrefix("⚠").trim())
    val a = txt.indexOf('{'); val b = txt.lastIndexOf('}')
    if (a < 0 || b < a) throw IllegalStateException("bad AI reply")
    val j = JSONObject(txt.substring(a, b + 1))
    val ex = anyList(j.opt("ex")).mapNotNull { (it as? JSONObject)?.let(Exercise::from) }.filter { it.q.isNotBlank() && it.answer.isNotBlank() }
    if (ex.isEmpty()) throw IllegalStateException("no questions")
    return j.optString("title", topic) to ex
}
