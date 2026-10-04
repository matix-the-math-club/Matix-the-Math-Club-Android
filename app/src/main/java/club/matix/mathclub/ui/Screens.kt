package club.matix.mathclub.ui

import android.app.Activity
import android.annotation.SuppressLint
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import club.matix.mathclub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun MatixApp(store: Store) {
    var dark by remember { mutableStateOf(store.dark) }
    var themeId by remember { mutableStateOf(store.themeId) }
    var user by remember { mutableStateOf(store.user) }
    var language by remember { mutableStateOf(store.language) }
    var booting by remember { mutableStateOf(true) }
    var welcomed by remember { mutableStateOf(store.seenWelcome) }
    LaunchedEffect(Unit) { delay(1200); booting = false }

    MatixTheme(dark, themeId) {
        CompositionLocalProvider(LocalLayoutDirection provides if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                booting -> BootScreen()
                user == null && !welcomed -> WelcomeScreen { store.seenWelcome = true; welcomed = true }
                user == null -> AuthScreen(onBack = { welcomed = false }) { name, revision ->
                    store.user = name
                    store.setSessionRevision(name, revision)
                    user = name
                }
                else -> HomeScreen(
                    store, user!!, dark,
                    onDark = { dark = it; store.dark = it },
                    themeId = themeId,
                    onTheme = { themeId = it; store.themeId = it },
                    language = language,
                    onLanguage = { language = it; store.language = it },
                    onSignOut = { store.user = null; user = null }
                )
            }
        }
        }
    }
}

@Composable
fun BootScreen() {
    Box(Modifier.fillMaxSize().background(Brand), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MATIX", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Spacer(Modifier.height(8.dp))
            Text("Loading your math workspace…", color = Color.White.copy(alpha = .85f))
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(Modifier.width(180.dp), color = Accent, trackColor = Color.White.copy(alpha = .25f))
        }
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MATIX", fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 5.sp)
        Text("Welcome to Matix", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Matix is a free, educative math platform for the Math Club. Learn at your own pace, play member-made games, chat with AI tutors, and grow your skills — no paywall, ever.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Goal("🦉", "Learn anything", "AI lessons explain math from zero.")
        Goal("🎮", "Play & create", "Try club games or publish your own.")
        Goal("✨", "Club community", "Share ideas, earn points, and shape what Matix builds next.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue → Sign in") }
        Spacer(Modifier.height(8.dp))
        Text("100% free · forever", color = Accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Goal(icon: String, title: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column { Text(title, fontWeight = FontWeight.Bold); Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AuthScreen(onBack: () -> Unit, onDone: (String, Long) -> Unit) {
    var join by remember { mutableStateOf(false) }
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (join) "Join Matix" else "Sign in to Matix", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Your free math club workspace", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(u, { u = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            p, { p = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !busy, modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true; err = ""
                scope.launch {
                    if (join) {
                        val r = withContext(Dispatchers.IO) { Auth.join(u, p) }
                        when (r) {
                            JoinResult.OK -> {
                                val normalized = u.lowercase().replace(Regex("[^a-z0-9_]"), "")
                                val revision = withContext(Dispatchers.IO) { Auth.sessionRevision(normalized) }
                                onDone(normalized, revision)
                            }
                            JoinResult.SHORT_NAME -> err = "Pick a username with at least 2 letters or numbers."
                            JoinResult.SHORT_PASS -> err = "Password must be at least 4 characters."
                            JoinResult.TAKEN -> err = "That username is taken."
                            JoinResult.NETWORK -> err = "Couldn't reach the server. Check your internet."
                        }
                    } else {
                        when (withContext(Dispatchers.IO) { Auth.signIn(u, p) }) {
                            SignInResult.OK -> {
                                val normalized = Auth.normalize(u)
                                val revision = withContext(Dispatchers.IO) { Auth.sessionRevision(normalized) }
                                onDone(normalized, revision)
                            }
                            SignInResult.BAD -> err = "Wrong username or password."
                            SignInResult.BANNED -> err = "This account has been suspended by the owner."
                            SignInResult.NETWORK -> err = "Couldn't reach the server."
                        }
                    }
                    busy = false
                }
            }
        ) { Text(if (busy) "Please wait…" else if (join) "Create account" else "Sign in") }
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = { join = !join; err = "" }) {
            Text(if (join) "Already a member? Sign in" else "New to Matix? Create an account")
        }
        TextButton(onClick = onBack) { Text("← Back to welcome") }
    }
}

private enum class Tab(val label: String, val icon: String) {
    Labs("Home", "🏠"), Learn("Learn", "🦉"), Chat("AI", "✨"), Games("Games", "🎮"), Messages("Inbox", "🔔"),
    Discussions("Discussions", "💬"), MathChat("Math Chat", "🧮"),
    Ideas("Ideas", "💡"), Points("Points", "⭐"), Users("Users", "👥"), Bugs("Bugs", "🐞"), Changelog("Changelog", "📝"),
    Private("Private", "🔒"), AppPages("App Pages", "📄"), AppPage("Page", "📄"),
    Translator("Translator", "🌐"), Settings("Settings", "⚙")
}

private val BottomTabs = listOf(Tab.Labs, Tab.Learn, Tab.Chat, Tab.Games, Tab.Messages)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    store: Store, user: String, dark: Boolean, onDark: (Boolean) -> Unit, themeId: String, onTheme: (String) -> Unit,
    language: String, onLanguage: (String) -> Unit, onSignOut: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Labs) }
    var selectedChat by remember { mutableStateOf<String?>(null) }
    var selectedAppPage by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as? Activity
    LaunchedEffect(store.brightness, activity) {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = if (store.brightness >= 100) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else store.brightness / 100f
            window.attributes = params
        }
    }
    LaunchedEffect(user) {
        role = withContext(Dispatchers.IO) { Firebase.get("/roles/${Firebase.enc(user)}") as? String }
    }
    LaunchedEffect(user) {
        while (isActive) {
            delay(30_000)
            val serverRevision = withContext(Dispatchers.IO) { Auth.sessionRevision(user) }
            if (serverRevision > store.sessionRevision(user)) {
                store.user = null
                onSignOut()
                break
            }
        }
    }
    val owner = isOwnerUser(user, role)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matix — ${tab.label}") },
                actions = {
                    TextButton(onClick = { menu = true }) { Text("☰ More") }
                    DropdownMenu(menu, { menu = false }) {
                        Tab.values().filter { it !in BottomTabs && it != Tab.AppPage }.forEach {
                            DropdownMenuItem(
                                text = { Text("${it.icon}  ${it.label}") },
                                onClick = {
                                    if (it == Tab.MathChat) selectedChat = null
                                    tab = it
                                    menu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                BottomTabs.forEach {
                    NavigationBarItem(
                        selected = tab == it, onClick = { tab = it },
                        icon = { Text(it.icon, fontSize = 20.sp) }, label = { Text(it.label, maxLines = 1, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Labs -> LabsScreen(store, user, owner) { id ->
                    tab = when (id) { "learn" -> Tab.Learn; "games" -> Tab.Games; "chat" -> Tab.Chat; "ideas" -> Tab.Ideas; else -> Tab.Translator }
                }
                Tab.Learn -> LearnScreen(store, user, owner)
                Tab.Chat -> ChatScreen(
                    store = store,
                    user = user,
                    isOwner = owner,
                    onTheme = { id, isDark -> onTheme(id); onDark(isDark) },
                    onBrightness = { store.brightness = it },
                    onNavigate = { page, peer ->
                        when (page.lowercase()) {
                            "home" -> tab = Tab.Labs
                            "games" -> tab = Tab.Games
                            "ideas" -> tab = Tab.Ideas
                            "learn", "math learn" -> tab = Tab.Learn
                            "points" -> tab = Tab.Points
                            "discussions", "discussion" -> tab = Tab.Discussions
                            "messages", "inbox" -> tab = Tab.Messages
                            "users" -> tab = Tab.Users
                            "bugs" -> tab = Tab.Bugs
                            "private" -> tab = Tab.Private
                            "app pages" -> tab = Tab.AppPages
                            "settings" -> tab = Tab.Settings
                            "translator", "translate" -> tab = Tab.Translator
                            "mathchat" -> { selectedChat = peer; tab = Tab.MathChat }
                            "chat" -> tab = Tab.Chat
                        }
                    }
                )
                Tab.Discussions -> DiscussionsScreen(user) { peer ->
                    selectedChat = peer
                    tab = Tab.MathChat
                }
                Tab.MathChat -> MathChatScreen(user, selectedChat)
                Tab.Private -> PrivateScreen(user)
                Tab.AppPages -> CustomPagesScreen(user, owner) { pageId ->
                    selectedAppPage = pageId
                    tab = Tab.AppPage
                }
                Tab.AppPage -> CustomPageViewer(selectedAppPage.orEmpty()) {
                    selectedAppPage = null
                    tab = Tab.AppPages
                }
                Tab.Games -> GamesScreen(user, owner)
                Tab.Ideas -> IdeasScreen(user)
                Tab.Messages -> MessagesScreen(store, user, owner)
                Tab.Points -> PointsScreen(user, owner)
                Tab.Users -> UsersScreen(owner)
                Tab.Bugs -> BugsScreen(user, owner)
                Tab.Changelog -> ChangelogScreen(user, owner)
                Tab.Translator -> TranslatorScreen(store)
                Tab.Settings -> SettingsScreen(
                    store, user, dark, onDark, themeId, onTheme, language, onLanguage, onSignOut,
                    onSignOutAll = {
                        val revision = withContext(Dispatchers.IO) { Auth.signOutEverywhere(user) }
                        if (revision != null) {
                            store.setSessionRevision(user, revision)
                            onSignOut()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatScreen(
    store: Store,
    user: String,
    isOwner: Boolean,
    onTheme: (String, Boolean) -> Unit,
    onBrightness: (Int) -> Unit,
    onNavigate: (String, String?) -> Unit
) {
    val msgs = remember { mutableStateListOf(ChatMsg(false, "Hi! I'm Matix AI 🦉 Ask me any math question.")) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var approval by remember { mutableStateOf<AssistantTools.Approval?>(null) }
    var approvalDecision by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var darkNow by remember { mutableStateOf(store.dark) }
    var themeNow by remember { mutableStateOf(store.themeId) }
    var brightnessNow by remember { mutableIntStateOf(store.brightness) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size) { list.animateScrollToItem(maxOf(0, msgs.size - 1)) }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = list) {
            items(msgs) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
                    Text(
                        m.text,
                        Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (m.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        color = if (m.fromUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (busy) item { Text("Matix AI is thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask a math question…") }, maxLines = 4)
            Spacer(Modifier.width(8.dp))
            Button(enabled = !busy && input.isNotBlank(), onClick = {
                msgs.add(ChatMsg(true, input.trim())); input = ""; busy = true
                val snapshot = msgs.toList().drop(1)
                scope.launch {
                    val r = AssistantTools.reply(
                        key = store.aiKey,
                        user = user,
                        isOwner = isOwner,
                        dark = darkNow,
                        theme = themeNow,
                        brightness = brightnessNow,
                        history = snapshot,
                        store = store,
                        confirm = { request ->
                            val decision = CompletableDeferred<Boolean>()
                            approvalDecision = decision
                            approval = request
                            decision.await()
                        },
                        onTheme = { id, isDark ->
                            themeNow = id
                            darkNow = isDark
                            store.themeId = id
                            store.dark = isDark
                            onTheme(id, isDark)
                        },
                        onBrightness = { percent ->
                            brightnessNow = percent
                            store.brightness = percent
                            onBrightness(percent)
                        },
                        onNavigate = onNavigate
                    )
                    msgs.add(ChatMsg(false, r)); busy = false
                }
            }) { Text("Send") }
        }
    }
    approval?.let { request ->
        AlertDialog(
            onDismissRequest = {
                approvalDecision?.complete(false)
                approvalDecision = null
                approval = null
            },
            title = { Text(request.title) },
            text = { Text(request.detail) },
            confirmButton = {
                Button(onClick = {
                    approvalDecision?.complete(true)
                    approvalDecision = null
                    approval = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    approvalDecision?.complete(false)
                    approvalDecision = null
                    approval = null
                }) { Text("Cancel") }
            }
        )
    }
}

private class Game(val id: String, val name: String, val author: String, val ai: Boolean, val likes: Int, val html: String, val thumb: String)

@Composable
fun GamesScreen(user: String, isOwner: Boolean) {
    var building by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    if (building) { GameBuilderScreen(user, editId) { building = false; editId = null; reload++ }; return }
    var games by remember { mutableStateOf<List<Game>?>(null) }
    var playing by remember { mutableStateOf<Game?>(null) }
    LaunchedEffect(reload) {
        games = withContext(Dispatchers.IO) {
            val o = Firebase.get("/hubprojects") as? JSONObject
            o?.keys()?.asSequence()?.mapNotNull { k ->
                val v = o.optJSONObject(k) ?: return@mapNotNull null
                if (v.optBoolean("published", true).not()) return@mapNotNull null
                Game(k, v.optString("name", "Untitled"), v.optString("author", "?"), v.optBoolean("madeWithAI"),
                    v.optJSONObject("likes")?.length() ?: 0, v.optString("html"), v.optString("thumb"))
            }?.sortedByDescending { it.likes }?.toList() ?: emptyList()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎮 Games Hub", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { editId = null; building = true }) { Text("＋ New") }
        }
        Text("Play games made by the club.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val g = games
        when {
            g == null -> CircularProgressIndicator()
            g.isEmpty() -> Text("No games yet — be the first to publish one!")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(g) { game ->
                    Card(Modifier.fillMaxWidth().clickable { playing = game }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (game.thumb.isNotBlank()) {
                                DataUriImage(game.thumb, Modifier.size(92.dp, 62.dp).clip(RoundedCornerShape(10.dp)), "${game.name} thumbnail")
                                Spacer(Modifier.width(12.dp))
                            } else {
                                Box(Modifier.size(92.dp, 62.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                    Text("🎮", fontSize = 27.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(game.name + if (game.ai) "  ✨ AI" else "", fontWeight = FontWeight.Bold)
                                Text("by @${game.author}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("🤍 ${game.likes}")
                        }
                    }
                }
            }
        }
    }
    playing?.let { game ->
        val scope = rememberCoroutineScope()
        Dialog(onDismissRequest = { playing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(game.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            scope.launch(Dispatchers.IO) { Firebase.put("/hubprojects/${game.id}/likes/${Firebase.enc(user)}", true) }
                        }) { Text("🤍 Like") }
                        if (game.author == user || isOwner) {
                            TextButton(onClick = { editId = game.id; playing = null; building = true }) { Text("✏ Edit") }
                            TextButton(onClick = {
                                scope.launch { withContext(Dispatchers.IO) { Firebase.delete("/hubprojects/${game.id}") }; playing = null; reload++ }
                            }) { Text("Delete") }
                        }
                        TextButton(onClick = { playing = null }) { Text("Close") }
                    }
                    GameStage(game.html)
                }
            }
        }
    }
}

/** Member-made games are HTML documents, so they are the only thing that needs a sandboxed WebView. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GameStage(html: String) {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxSize())
}

private class Idea(val id: String, val text: String, val by: String, val votes: Int, val voted: Boolean)

@Composable
fun IdeasScreen(user: String) {
    var ideas by remember { mutableStateOf<List<Idea>?>(null) }
    var text by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tick) {
        ideas = withContext(Dispatchers.IO) {
            val o = Firebase.get("/ideas", Firebase.IDEAS) as? JSONObject
            o?.keys()?.asSequence()?.mapNotNull { k ->
                val v = o.optJSONObject(k) ?: return@mapNotNull null
                Idea(k, v.optString("text"), v.optString("by", "?"), v.optInt("votes"), v.optJSONObject("voters")?.has(user) == true)
            }?.sortedByDescending { it.votes }?.toList() ?: emptyList()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("💡 Ideas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Suggest features for the Matix app. Everyone can post and vote.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Your idea…") })
            Spacer(Modifier.width(8.dp))
            Button(enabled = text.isNotBlank(), onClick = {
                val t = text.trim(); text = ""
                scope.launch {
                    withContext(Dispatchers.IO) {
                        Firebase.post("/ideas", JSONObject().put("text", t).put("by", user).put("at", System.currentTimeMillis()).put("votes", 0), Firebase.IDEAS)
                    }
                    tick++
                }
            }) { Text("Post") }
        }
        val l = ideas
        if (l == null) CircularProgressIndicator()
        else if (l.isEmpty()) Text("No ideas yet — post the first one!")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(l) { idea ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TextButton(enabled = !idea.voted, onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        Firebase.put("/ideas/${idea.id}/voters/${Firebase.enc(user)}", true, Firebase.IDEAS)
                                        Firebase.patch("/ideas/${idea.id}", JSONObject().put("votes", idea.votes + 1), Firebase.IDEAS)
                                    }
                                    tick++
                                }
                            }) { Text("▲") }
                            Text("${idea.votes}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(idea.text)
                            Text("by @${idea.by}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    store: Store,
    user: String,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    themeId: String,
    onTheme: (String) -> Unit,
    currentLanguage: String,
    onLanguage: (String) -> Unit,
    onSignOut: () -> Unit,
    onSignOutAll: suspend () -> Unit
) {
    var showProfile by remember { mutableStateOf(false) }
    if (showProfile) ProfileDialog(user) { showProfile = false }
    var key by remember { mutableStateOf(store.aiKey) }
    var saved by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(store.reduceMotion) }
    var quietMode by remember { mutableStateOf(store.quietMode) }
    var brightness by remember { mutableIntStateOf(store.brightness) }
    var language by remember { mutableStateOf(currentLanguage) }
    var languageMenu by remember { mutableStateOf(false) }
    var confirmSignOutAll by remember { mutableStateOf(false) }
    var signingOutAll by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    LaunchedEffect(brightness, activity) {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = if (brightness >= 100) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else brightness / 100f
            window.attributes = params
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⚙ Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Signed in as @$user", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌙 Dark mode", Modifier.weight(1f))
            Switch(dark, onDark)
        }
        OutlinedButton(onClick = { showProfile = true }) { Text("🎨 My profile") }
        ThemePicker(themeId, onTheme)
        Text("Appearance & accessibility", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎞 Reduce motion", Modifier.weight(1f))
            Switch(reduceMotion, {
                reduceMotion = it
                store.reduceMotion = it
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔕 Quiet mode (fewer pop-ups)", Modifier.weight(1f))
            Switch(quietMode, {
                quietMode = it
                store.quietMode = it
            })
        }
        Text("Brightness  $brightness%", fontWeight = FontWeight.SemiBold)
        Slider(
            value = brightness.toFloat(),
            onValueChange = {
                brightness = it.toInt().coerceIn(60, 120)
                store.brightness = brightness
            },
            valueRange = 60f..120f
        )
        Text("Language", fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { languageMenu = true }) {
                Text(mapOf("en" to "English", "ar" to "العربية", "fr" to "Français", "es" to "Español")[language] ?: "English")
            }
            DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                listOf("en" to "English", "ar" to "العربية", "fr" to "Français", "es" to "Español").forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            language = code
                            store.language = code
                            onLanguage(code)
                            languageMenu = false
                        }
                    )
                }
            }
        }
        Text("AI key (Gemini, Groq, OpenAI, OpenRouter or GitHub token)", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            key, { key = it; saved = false }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = { store.aiKey = key; saved = true }) { Text(if (saved) "Saved ✓" else "Save key") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSignOut) { Text("⏻ Sign out") }
        OutlinedButton(
            enabled = !signingOutAll,
            onClick = { confirmSignOutAll = true }
        ) { Text(if (signingOutAll) "Signing out…" else "⏻ Sign out of all devices") }
    }
    if (confirmSignOutAll) AlertDialog(
        onDismissRequest = { if (!signingOutAll) confirmSignOutAll = false },
        title = { Text("Sign out everywhere?") },
        text = { Text("All devices using this account will need to sign in again.") },
        confirmButton = {
            TextButton(enabled = !signingOutAll, onClick = {
                signingOutAll = true
                scope.launch {
                    runCatching { onSignOutAll() }
                    signingOutAll = false
                    confirmSignOutAll = false
                }
            }) { Text("Sign out everywhere") }
        },
        dismissButton = { TextButton(onClick = { confirmSignOutAll = false }) { Text("Cancel") } }
    )
}
