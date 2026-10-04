package club.matix.mathclub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun LearnAnythingScreen(
    store: Store,
    me: String,
    isOwner: Boolean,
    onPractice: (LearnAnythingPractice) -> Unit,
    onBackToCourse: () -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var activeTopic by remember { mutableStateOf("") }
    var lesson by remember { mutableStateOf<AiLearnLesson?>(null) }
    var popular by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var recent by remember { mutableStateOf(store.recentLearnTopics()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cacheKind by remember { mutableStateOf("") }
    var showServerSettings by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(store.learnServerUrl) }
    var publishServer by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(refreshTick) { popular = withContext(Dispatchers.IO) { runCatching { LearnAi.popular() }.getOrDefault(emptyList()) } }

    fun teach(raw: String, force: Boolean = false) {
        val value = raw.trim().take(120)
        if (value.isEmpty() || loading) return
        activeTopic = value
        topic = value
        error = null
        lesson = null
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { LearnAi.fetch(store, me, value, force) } }
            loading = false
            result.onSuccess { found ->
                lesson = found
                cacheKind = found.cacheKind
                store.rememberLearnTopic(found.topic)
                recent = store.recentLearnTopics()
                refreshTick++
            }.onFailure { error = it.message ?: "The Learn server could not build that lesson." }
        }
    }

    if (lesson == null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBackToCourse) { Text("← My course") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🦉 Math Learn", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Learn any topic, from fractions to volcanoes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isOwner) TextButton(onClick = { serverUrl = store.learnServerUrl; showServerSettings = true }) { Text("⚙ Server") }
            }
            Text("The Learn server researches a topic, checks sources, creates a beginner-friendly lesson, and generates practice questions.", fontSize = 13.sp)
            OutlinedTextField(
                value = topic, onValueChange = { topic = it.take(120) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("What do you want to learn?") },
                placeholder = { Text("fractions, Minecraft, volcanoes…") }, singleLine = true,
                trailingIcon = { Text("🧠") }
            )
            Button(enabled = topic.isNotBlank() && !loading, onClick = { teach(topic) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (loading) "Building your lesson…" else "✨ Teach me")
            }
            if (loading) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Matix Learn AI is working on “$activeTopic”", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        listOf("Checking existing lessons", "Researching trusted sources", "Double-checking facts", "Writing a beginner lesson", "Making exercises").forEachIndexed { index, label ->
                            Text("${if (index == 0) "🔎" else "•"}  $label", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (recent.isNotEmpty()) {
                Text("🕒 Your recent lessons", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recent.forEach { item -> AssistChip(onClick = { teach(item) }, label = { Text("📘 $item") }) }
                }
            }
            if (popular.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥 Popular in the club", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("cached lessons", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                popular.forEach { item ->
                    val slug = item.optString("slug")
                    val label = item.optString("title").ifBlank { item.optString("topic", slug) }
                    Card(Modifier.fillMaxWidth().clickable { teach(item.optString("topic").ifBlank { label }) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.optString("emoji").ifBlank { "⚡" }, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                                Text("${item.optInt("hits")} learners", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Open →")
                        }
                    }
                }
            }
        }
    } else {
        AiLessonContent(
            lesson = lesson!!,
            cacheKind = cacheKind,
            onBackToCourse = onBackToCourse,
            isOwner = isOwner,
            onBack = { lesson = null; error = null; topic = "" },
            onRebuild = { teach(lesson!!.topic, true) },
            onPractice = {
                if (lesson!!.exercises.isEmpty()) error = "This lesson has no practice questions yet."
                else onPractice(LearnAnythingPractice(lesson!!.title, lesson!!.exercises))
            },
            error = error,
            onSource = { url -> runCatching { uriHandler.openUri(url) }.onFailure { error = "Couldn't open this source link." } }
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
    }

    if (showServerSettings) AlertDialog(
        onDismissRequest = { showServerSettings = false },
        title = { Text("⚙ Math Learn server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Point this device at the Python Learn service. The Android emulator can reach the development computer at ${LearnAi.DEFAULT_SERVER}.", fontSize = 13.sp)
                OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(publishServer, { publishServer = it })
                    Text("Publish this URL for everyone (use a hosted HTTPS server)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clean = serverUrl.trim().trimEnd('/')
                store.learnServerUrl = clean
                showServerSettings = false
                refreshTick++
                if (publishServer && clean.startsWith("https://")) scope.launch(Dispatchers.IO) {
                    Firebase.put("/learnai/server", clean)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { showServerSettings = false }) { Text("Cancel") } }
    )
}

@Composable
private fun AiLessonContent(
    lesson: AiLearnLesson,
    cacheKind: String,
    isOwner: Boolean,
    onBackToCourse: () -> Unit,
    onBack: () -> Unit,
    onRebuild: () -> Unit,
    onPractice: () -> Unit,
    error: String?,
    onSource: (String) -> Unit
) {
    val expandedExamples = remember(lesson.slug) { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackToCourse) { Text("← My course") }
            TextButton(onClick = onBack) { Text("Topics") }
            Spacer(Modifier.weight(1f))
            AssistChip(onClick = {}, label = { Text(if (cacheKind == "fresh") "🌟 Freshly researched" else "⚡ Cached · $cacheKind") })
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(lesson.emoji, fontSize = 36.sp)
                Text(lesson.title, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text("🌱 Beginner-friendly") })
                    if (lesson.sources.isNotEmpty()) AssistChip(onClick = {}, label = { Text("🔎 ${lesson.sources.size} sources") })
                }
                if (lesson.agreement != null) Text("✅ ${lesson.agreement}% source agreement", fontSize = 12.sp)
                if (lesson.ownerLessons.isNotEmpty()) Text("👑 Uses owner lessons: ${lesson.ownerLessons.joinToString()}", fontSize = 12.sp)
            }
        }
        Text(lesson.intro, fontSize = 15.sp)
        LearnSectionTitle("1", "Learn it step by step")
        lesson.steps.forEachIndexed { i, step ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
                        Text("${i + 1}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(step.title.ifBlank { "Step ${i + 1}" }, fontWeight = FontWeight.Bold)
                        Text(step.body)
                        if (step.example.isNotBlank()) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                            Text(step.example, Modifier.fillMaxWidth().padding(10.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        if (lesson.examples.isNotEmpty()) {
            LearnSectionTitle("2", "Worked examples")
            lesson.examples.forEachIndexed { i, example ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(example.problem, fontWeight = FontWeight.Bold)
                        if (i in expandedExamples) Text(example.solution, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = { if (i in expandedExamples) expandedExamples.remove(i) else expandedExamples.add(i) }) {
                            Text(if (i in expandedExamples) "Hide solution" else "Show solution")
                        }
                    }
                }
            }
        }
        if (lesson.mistakes.isNotEmpty()) {
            LearnSectionTitle("3", "Watch out! Common mistakes")
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { lesson.mistakes.forEach { Text("• $it", modifier = Modifier.padding(vertical = 3.dp)) } } }
        }
        if (lesson.glossary.isNotEmpty()) {
            LearnSectionTitle("📖", "Words to know")
            lesson.glossary.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(item.term, Modifier.widthIn(min = 90.dp), fontWeight = FontWeight.Bold)
                    Text(item.meaning, Modifier.weight(1f))
                }
            }
        }
        if (lesson.sources.isNotEmpty()) {
            LearnSectionTitle("🔗", "Where this was checked")
            lesson.sources.forEach { source ->
                TextButton(enabled = source.url.startsWith("https://") || source.url.startsWith("http://"), onClick = { onSource(source.url) }) {
                    Text("🌐 ${source.name.ifBlank { source.url }}")
                }
            }
        }
        Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) { Text("🏃 Practice now · ${lesson.exercises.size} questions") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("🦉 Learn something else") }
        if (isOwner) OutlinedButton(onClick = onRebuild, modifier = Modifier.fillMaxWidth()) { Text("↻ Rebuild this lesson") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun LearnSectionTitle(number: String, title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
            Text(number, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
