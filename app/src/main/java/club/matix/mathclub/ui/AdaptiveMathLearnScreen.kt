package club.matix.mathclub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import org.json.JSONObject
import kotlin.math.roundToInt

data class LearnAnythingPractice(val title: String, val exercises: List<Exercise>)

private data class Attempt(val question: MathQuestion, val correct: Boolean, val hinted: Boolean)
private data class QuizSummary(val score: Int, val right: Int, val total: Int, val bestStreak: Int, val attempts: List<Attempt>)
private data class CompletedRun(val node: CourseNode, val stars: Int, val xp: Int, val score: Int, val right: Int, val total: Int, val bestStreak: Int, val best: Boolean, val missed: List<Attempt>)

private val MathBadges = listOf(
    Triple("first", "👣", "First steps"),
    Triple("star3", "🌟", "Perfectionist"),
    Triple("hot", "🔥", "Hot streak"),
    Triple("blitz", "⚡", "Speedster"),
    Triple("pop", "🫧", "Bubble master"),
    Triple("streak3", "📅", "On a roll"),
    Triple("done", "👑", "Course complete")
)

@Composable
fun LearnScreen(store: Store, me: String, isOwner: Boolean) {
    var anything by remember(me) { mutableStateOf(false) }
    var practice by remember(me) { mutableStateOf<LearnAnythingPractice?>(null) }
    when {
        practice != null -> ExternalExerciseQuiz(practice!!.title, practice!!.exercises) { practice = null }
        anything -> LearnAnythingScreen(
            store = store,
            me = me,
            isOwner = isOwner,
            onPractice = { practice = it },
            onBackToCourse = { anything = false }
        )
        else -> AdaptiveMathLearnScreen(store, me, onAnything = { anything = true })
    }
}

@Composable
private fun AdaptiveMathLearnScreen(store: Store, user: String, onAnything: () -> Unit) {
    var state by remember(user) { mutableStateOf(AdaptiveMathLearn.read(store.mathLearnProfile(user))) }
    var page by remember(user) { mutableStateOf(if (state.placed && state.course.isNotEmpty()) "course" else "intro") }
    var draftLevel by remember(user) { mutableStateOf<Int?>(null) }
    val draftTopics = remember(user) { mutableStateListOf<String>() }
    var draftText by remember(user) { mutableStateOf("") }
    var warmupTopics by remember { mutableStateOf(emptyList<String>()) }
    var warmupLevel by remember { mutableIntStateOf(1) }
    var warmupSummary by remember { mutableStateOf<QuizSummary?>(null) }
    var placementSkills by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var activeNode by remember { mutableStateOf<CourseNode?>(null) }
    var result by remember { mutableStateOf<CompletedRun?>(null) }
    var showReset by remember { mutableStateOf(false) }

    fun save(next: PersonalMathLearn) {
        state = next
        store.saveMathLearnProfile(user, next.toJson())
    }

    fun startWarmup(skip: Boolean) {
        val parsed = AdaptiveMathLearn.parseClaim(draftText)
        val topics = if (skip) emptyList() else (draftTopics.toList() + parsed.second).distinct().take(4)
        val level = if (skip) 0 else (draftLevel ?: parsed.first ?: if (topics.isNotEmpty()) 2 else 1)
        val selectedLevel = level.coerceIn(0, 3)
        val nextState = state.copy(claimLevel = selectedLevel, claimTopics = topics, claimText = if (skip) "" else draftText.trim())
        save(nextState)
        warmupLevel = selectedLevel
        warmupTopics = AdaptiveMathLearn.planTopics(selectedLevel, topics)
        warmupSummary = null
        page = "warmup"
    }

    fun buildPersonalCourse() {
        val nodes = AdaptiveMathLearn.buildCourse(state.levels)
        save(state.copy(placed = true, course = nodes, done = emptyMap()))
        page = "course"
    }

    fun finishNode(node: CourseNode, summary: QuizSummary, arcade: Boolean) {
        val previous = state.done[node.id]
        val pct = if (summary.total == 0) 0.0 else summary.right.toDouble() / summary.total
        val stars = if (arcade) {
            if (node.kind == "blitz") if (summary.score >= 80) 3 else if (summary.score >= 50) 2 else 1
            else if (summary.score >= 100) 3 else if (summary.score >= 50) 2 else 1
        } else if (pct >= .9) 3 else if (pct >= .6) 2 else 1
        var gain = if (arcade) summary.score / if (node.kind == "blitz") 2 else 3 else summary.score + stars * 10
        if (node.free || previous != null) gain = (gain * .5).roundToInt()
        val done = state.done.toMutableMap()
        if (!node.free) done[node.id] = NodeResult(maxOf(stars, previous?.stars ?: 0), maxOf(summary.score, previous?.best ?: 0))
        val missedTopics = summary.attempts.filterNot { it.correct }.map { it.question.topicId }
        val revise = (state.revise.filterNot { it in missedTopics } + missedTopics).takeLast(5)
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val newDay = state.streakDate != today
        val streak = if (!newDay) state.streak else if (state.streakDate == yesterday) state.streak + 1 else 1
        val badges = state.badges.toMutableMap()
        fun badge(id: String) { if (id !in badges) badges[id] = today }
        if (!arcade && node.kind == "lesson") badge("first")
        if (!arcade && stars == 3 && node.kind == "lesson") badge("star3")
        if (summary.bestStreak >= 5) badge("hot")
        if (newDay && streak >= 3) badge("streak3")
        if (node.kind == "boss") badge("done")
        if (arcade && node.kind == "blitz" && summary.score >= 80) badge("blitz")
        if (arcade && node.kind == "game" && summary.score >= 100) badge("pop")
        val levels = state.levels.toMutableMap()
        if (node.kind == "lesson" && node.part == "b" && stars >= 2) {
            levels[node.topicId] = maxOf(levels[node.topicId] ?: 0, node.level)
        }
        val bestValue = if (node.kind == "blitz") maxOf(state.bestBlitz, summary.score) else if (node.kind == "game") maxOf(state.bestPop, summary.score) else 0
        val newBest = when (node.kind) {
            "blitz" -> summary.score > state.bestBlitz
            "game" -> summary.score > state.bestPop
            else -> false
        }
        val next = state.copy(
            levels = levels, done = done, xp = state.xp + gain, streak = streak,
            streakDate = if (newDay) today else state.streakDate,
            bestBlitz = if (node.kind == "blitz") bestValue else state.bestBlitz,
            bestPop = if (node.kind == "game") bestValue else state.bestPop,
            badges = badges, revise = revise
        )
        save(next)
        result = CompletedRun(node, stars, gain, summary.score, summary.right, summary.total, summary.bestStreak, newBest, summary.attempts.filterNot { it.correct })
        page = "result"
    }

    when (page) {
        "intro" -> AdaptiveIntroScreen(
            user = user,
            selectedLevel = draftLevel,
            onSelectLevel = { draftLevel = it },
            selectedTopics = draftTopics,
            onToggleTopic = { id -> if (id in draftTopics) draftTopics.remove(id) else if (draftTopics.size < 4) draftTopics.add(id) },
            text = draftText,
            onText = { draftText = it.take(300) },
            onStart = { startWarmup(false) },
            onSkip = { startWarmup(true) },
            onAnything = onAnything
        )
        "warmup" -> WarmupScreen(
            topics = warmupTopics,
            claimLevel = warmupLevel,
            onExit = { page = "intro" },
            onDone = { summary, skills ->
                warmupSummary = summary
                placementSkills = skills
                save(state.copy(levels = skills, tested = warmupTopics))
                page = "map"
            }
        )
        "map" -> SkillMapScreen(
            tested = warmupTopics,
            levels = placementSkills,
            summary = warmupSummary,
            onBuild = ::buildPersonalCourse,
            onBack = { page = "intro" }
        )
        "course" -> PersonalCourseDashboard(
            state = state,
            onOpenNode = { node -> activeNode = node; page = if (node.kind == "lesson") "lesson" else if (node.kind == "blitz" || node.kind == "game") "arcade" else "quiz" },
            onAnything = onAnything,
            onRetakeWarmup = { draftLevel = null; draftTopics.clear(); draftText = ""; save(state.copy(placed = false)); page = "intro" },
            onReset = { showReset = true }
        )
        "lesson" -> {
            val node = activeNode
            if (node == null) page = "course" else LessonCardsScreen(node, onBack = { page = "course" }) {
                page = "quiz"
            }
        }
        "quiz" -> {
            val node = activeNode
            if (node == null) page = "course" else {
                val questionTopics = if (node.kind == "boss") node.topics else listOf(node.topicId).filter(String::isNotBlank)
                val levels = if (node.kind == "boss") state.levels else mapOf(node.topicId to node.level)
                val total = if (node.kind == "boss") 8 else 5
                CourseQuizScreen(
                    title = AdaptiveMathLearn.nodeTitle(node),
                    topics = questionTopics.ifEmpty { listOf("arith", "frac") },
                    levels = levels,
                    total = total,
                    warmup = false,
                    lessonNode = node.takeIf { it.kind == "lesson" },
                    speed = node.kind == "boss" || (node.kind == "lesson" && node.part == "b"),
                    onExit = { page = "course" },
                    onDone = { finishNode(node, it, arcade = false) }
                )
            }
        }
        "arcade" -> {
            val node = activeNode
            if (node == null) page = "course" else ArcadeScreen(
                node = node,
                levels = state.levels,
                onExit = { page = "course" },
                onDone = { finishNode(node, it, arcade = true) }
            )
        }
        "result" -> result?.let { completed -> RunResultScreen(completed, onContinue = { page = "course" }, onReplay = {
            val node = completed.node
            activeNode = node
            page = if (node.kind == "lesson") "lesson" else if (node.kind == "blitz" || node.kind == "game") "arcade" else "quiz"
        }, onRetake = {
            draftLevel = null; draftTopics.clear(); draftText = ""
            save(state.copy(placed = false)); page = "intro"
        }) } ?: run { page = "course" }
        else -> page = "intro"
    }

    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("Reset Math Learn?") },
        text = { Text("This clears this device’s personal course, progress, XP, streak and badges.") },
        confirmButton = {
            Button(onClick = {
                store.clearMathLearnProfile(user)
                state = PersonalMathLearn()
                draftLevel = null; draftTopics.clear(); draftText = ""
                showReset = false
                page = "intro"
            }) { Text("Reset progress") }
        },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
    )
}

@Composable
private fun AdaptiveIntroScreen(
    user: String,
    selectedLevel: Int?,
    onSelectLevel: (Int) -> Unit,
    selectedTopics: List<String>,
    onToggleTopic: (String) -> Unit,
    text: String,
    onText: (String) -> Unit,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onAnything: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("🦉 Math Learn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Hi @$user! Let’s find your perfect starting point.", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("A quick, friendly game. No grades, no pressure.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("What do you know?", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Pick what sounds most like you:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                AdaptiveMathLearn.levelLabels.forEachIndexed { index, label ->
                    val icons = listOf("🌱", "🌿", "🌳", "🚀")
                    Card(
                        Modifier.fillMaxWidth().clickable { onSelectLevel(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLevel == index) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(icons[index], fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold)
                                Text(AdaptiveMathLearn.levelDescriptions[index], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = selectedLevel == index, onClick = { onSelectLevel(index) })
                        }
                    }
                }
                Text("Anything you already enjoy? (Choose up to 4)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                AdaptiveMathLearn.topics.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { topic ->
                            FilterChip(
                                selected = topic.id in selectedTopics,
                                onClick = { onToggleTopic(topic.id) },
                                label = { Text("${topic.emoji} ${topic.name}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Or tell me in your own words (optional)") },
                    placeholder = { Text("I’m okay with fractions but algebra confuses me") },
                    minLines = 2, maxLines = 3
                )
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start my warm-up 🚀") }
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Skip — start from the beginning") }
                Text("✨ About 8 quick questions help build your course. Mistakes help me more than perfect answers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = onAnything, modifier = Modifier.fillMaxWidth()) {
            Text("💡 Ask Matix to teach me anything →")
        }
    }
}

@Composable
private fun WarmupScreen(
    topics: List<String>,
    claimLevel: Int,
    onExit: () -> Unit,
    onDone: (QuizSummary, Map<String, Int>) -> Unit
) {
    val seen = remember(topics) { mutableSetOf<String>() }
    val previous = remember(topics) { topics.associateWith { mutableListOf<Pair<Int, Boolean>>() } }
    val initial = if (claimLevel == 0) 1 else claimLevel.coerceIn(1, 3)
    CourseQuizScreen(
        title = "Warm-up",
        topics = topics,
        levels = topics.associateWith { initial },
        total = topics.size * 2,
        warmup = true,
        speed = false,
        onExit = onExit,
        questionProvider = { index ->
            val topic = topics[index % topics.size]
            val attempts = previous.getValue(topic)
            val currentLevel = if (attempts.isEmpty()) initial else
                (attempts.last().first + if (attempts.last().second) 1 else -1).coerceIn(1, 3)
            AdaptiveMathLearn.question(topic, currentLevel, seen)
        },
        onWarmupAnswer = { question, correct ->
            previous.getValue(question.topicId).add(question.level to correct)
        },
        onDone = { summary ->
            val levels = AdaptiveMathLearn.topics.associate { topic ->
                val attempted = previous[topic.id].orEmpty()
                val score = attempted.filter { it.second }.maxOfOrNull { it.first } ?: 0
                val defaultKnown = claimLevel >= 2 && topic.id in setOf("frac", "dec", "arith", "neg", "ops")
                topic.id to maxOf(score, if (defaultKnown) 1 else 0)
            }
            onDone(summary, levels)
        }
    )
}

@Composable
private fun SkillMapScreen(tested: List<String>, levels: Map<String, Int>, summary: QuizSummary?, onBuild: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎉 Warm-up done — nice work!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        summary?.let { Text("You got ${it.right} of ${it.total}. Here’s your starting map.") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your starting map", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                tested.forEach { id ->
                    val topic = AdaptiveMathLearn.topic(id)
                    val level = levels[id] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${topic.emoji} ${topic.name}", Modifier.weight(1f))
                        Text("●".repeat(level) + "○".repeat(3 - level), color = Color(topic.color))
                    }
                    Text(AdaptiveMathLearn.skillLabel(level), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("🌱 Every topic starts somewhere. I’ll spend more time on the sprouts and cheer you on.", fontSize = 12.sp)
            }
        }
        Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) { Text("Build my course ✨") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
private fun PersonalCourseDashboard(
    state: PersonalMathLearn,
    onOpenNode: (CourseNode) -> Unit,
    onAnything: () -> Unit,
    onRetakeWarmup: () -> Unit,
    onReset: () -> Unit
) {
    val next = AdaptiveMathLearn.nextNode(state)
    val totalStars = state.done.values.sumOf { it.stars }
    val courseLevel = (state.xp / 120).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🦉", fontSize = 36.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Hi! Ready to level up?", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(if (next == null) "You finished the whole course — amazing!" else "Your next quest is waiting. Small steps count.")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("⭐", state.xp.toString(), "points", Modifier.weight(1f))
            StatCard("🔥", state.streak.toString(), "day streak", Modifier.weight(1f))
            StatCard("🌟", totalStars.toString(), "stars", Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Level ${courseLevel + 1} · ${AdaptiveMathLearn.levelNames[courseLevel.coerceAtMost(7)]}", fontWeight = FontWeight.Bold)
                LinearProgressIndicator((state.xp % 120) / 120f, Modifier.fillMaxWidth())
                Text("${state.xp % 120} / 120 XP to next level", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (next != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("UP NEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(AdaptiveMathLearn.nodeTitle(next), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(AdaptiveMathLearn.nodeSubtitle(next), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onOpenNode(next) }, modifier = Modifier.fillMaxWidth()) { Text("Let’s go ▶") }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Your course  ·  ${state.done.size} / ${state.course.size} done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                state.course.forEachIndexed { index, node ->
                    val result = state.done[node.id]
                    val unlocked = result != null || node.id == next?.id
                    val mark = when {
                        result != null -> "✓"
                        unlocked -> "${index + 1}"
                        else -> "🔒"
                    }
                    Card(
                        Modifier.fillMaxWidth().clickable(enabled = unlocked) { onOpenNode(node) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (result != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mark, Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                            Column(Modifier.weight(1f)) {
                                Text(AdaptiveMathLearn.nodeTitle(node), fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (result != null) "★".repeat(result.stars) + " · best ${result.best} pts" else AdaptiveMathLearn.nodeSubtitle(node),
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Skill map", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                AdaptiveMathLearn.topics.forEach { topic ->
                    val level = state.levels[topic.id] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${topic.emoji} ${topic.name}", Modifier.weight(1f))
                        Text("●".repeat(level) + "○".repeat(3 - level), color = Color(topic.color))
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Play & practise", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val topics = (state.tested + state.course.flatMap { it.topics }).distinct().ifEmpty { listOf("arith", "frac") }
                        onOpenNode(CourseNode("free-pop", "game", topics = topics, free = true))
                    }, modifier = Modifier.weight(1f)) { Text("🫧 Bubble Pop\nBest ${state.bestPop}") }
                    OutlinedButton(onClick = {
                        val topics = (state.tested + state.course.flatMap { it.topics }).distinct().ifEmpty { listOf("arith", "frac") }
                        onOpenNode(CourseNode("free-blitz", "blitz", topics = topics, free = true))
                    }, modifier = Modifier.weight(1f)) { Text("⚡ 60s Blitz\nBest ${state.bestBlitz}") }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Badges", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MathBadges.forEach { (id, icon, label) ->
                        val earned = id in state.badges
                        Surface(
                            color = if (earned) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.widthIn(min = 62.dp).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (earned) icon else "🔒")
                                Text(label, fontSize = 9.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAnything, modifier = Modifier.fillMaxWidth()) { Text("💡 Ask Matix to teach me anything") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onRetakeWarmup, modifier = Modifier.weight(1f)) { Text("🔄 Retake warm-up") }
            TextButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset my progress") }
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 19.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class LessonCardContent(val title: String, val body: String, val example: String = "")

@Composable
private fun LessonCardsScreen(node: CourseNode, onBack: () -> Unit, onStartQuiz: () -> Unit) {
    val context = LocalContext.current
    val topic = AdaptiveMathLearn.topic(node.topicId)
    val tip = AdaptiveMathLearn.tips[node.topicId]?.getOrNull((node.level - 1).coerceIn(0, 2)).orEmpty()
    val knowledge = remember(node.topicId) {
        runCatching {
            val raw = context.assets.open("math_learn_kb.json").bufferedReader().use { it.readText() }
            JSONObject(raw).optJSONObject(node.topicId)
        }.getOrNull()
    }
    val cards = remember(node.id, knowledge, tip) {
        val output = mutableListOf<LessonCardContent>()
        fun step(index: Int) {
            knowledge?.optJSONArray("steps")?.optJSONObject(index)?.let { row ->
                if (row.optString("title").isNotBlank()) output += LessonCardContent(row.optString("title"), row.optString("body"), row.optString("example"))
            }
        }
        if (node.part == "a") {
            output += LessonCardContent(topic.name + " — the big idea", knowledge?.optString("intro").orEmpty().ifBlank { tip })
            step(0); step(1)
            if (tip.isNotBlank()) output += LessonCardContent("💡 Level ${node.level} trick", tip)
        } else {
            if (tip.isNotBlank()) output += LessonCardContent("💡 Level ${node.level} trick", tip)
            step(2); step(3)
            knowledge?.optJSONArray("mistakes")?.let { mistakes ->
                val warning = (0 until minOf(2, mistakes.length())).map { mistakes.optString(it) }.filter(String::isNotBlank).joinToString(" • ")
                if (warning.isNotBlank()) output += LessonCardContent("⚠️ Watch out for this", warning)
            }
            knowledge?.optJSONArray("examples")?.optJSONObject(0)?.let { example ->
                output += LessonCardContent("Worked example", example.optString("problem"), example.optString("solution"))
            }
        }
        output.filter { it.body.isNotBlank() || it.example.isNotBlank() }.take(4)
    }
    var index by remember(node.id) { mutableIntStateOf(0) }
    if (cards.isEmpty()) {
        LaunchedEffect(node.id) { onStartQuiz() }
        return
    }
    val card = cards[index.coerceIn(cards.indices)]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← My course") }
        LinearProgressIndicator({ (index + 1f) / (cards.size + 1) }, Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${topic.emoji}  Quick idea ${index + 1} of ${cards.size}", color = Color(topic.color), fontWeight = FontWeight.Bold)
                Text(card.title, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text(card.body, fontSize = 16.sp)
                if (card.example.isNotBlank()) {
                    Text(card.example, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp))
                }
            }
        }
        Button(onClick = { if (index == cards.lastIndex) onStartQuiz() else index++ }, modifier = Modifier.fillMaxWidth()) {
            Text(if (index == cards.lastIndex) "Let’s play! 🎮" else "Got it →")
        }
    }
}

@Composable
private fun CourseQuizScreen(
    title: String,
    topics: List<String>,
    levels: Map<String, Int>,
    total: Int,
    warmup: Boolean,
    speed: Boolean,
    onExit: () -> Unit,
    onDone: (QuizSummary) -> Unit,
    lessonNode: CourseNode? = null,
    questionProvider: ((Int) -> MathQuestion)? = null,
    onWarmupAnswer: ((MathQuestion, Boolean) -> Unit)? = null
) {
    val seen = remember(title, topics) { mutableSetOf<String>() }
    var index by remember(title, topics) { mutableIntStateOf(0) }
    var score by remember(title, topics) { mutableIntStateOf(0) }
    var right by remember(title, topics) { mutableIntStateOf(0) }
    var streak by remember(title, topics) { mutableIntStateOf(0) }
    var bestStreak by remember(title, topics) { mutableIntStateOf(0) }
    var hintUsed by remember(title, topics) { mutableStateOf(false) }
    var hintShown by remember(title, topics) { mutableStateOf(false) }
    var current by remember(title, topics) { mutableStateOf<MathQuestion?>(null) }
    var typed by remember(title, topics) { mutableStateOf("") }
    var revealed by remember(title, topics) { mutableStateOf<Boolean?>(null) }
    var startedAt by remember(title, topics) { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedTick by remember(title, topics) { mutableIntStateOf(0) }
    val attempts = remember(title, topics) { mutableStateListOf<Attempt>() }

    LaunchedEffect(index, title, topics) {
        if (index < total && topics.isNotEmpty()) {
            val topicId = if (lessonNode != null) lessonNode.topicId else topics[index % topics.size]
            val base = (levels[topicId] ?: 1).coerceIn(1, 3)
            val level = when {
                lessonNode == null && title.contains("Final Quest") && index >= 4 -> (base + 1).coerceAtMost(3)
                lessonNode == null -> base
                lessonNode.part == "a" && index < 2 -> (base - 1).coerceAtLeast(1)
                lessonNode.part == "b" && index >= 3 -> (base + 1).coerceAtMost(3)
                else -> base
            }
            current = questionProvider?.invoke(index) ?: AdaptiveMathLearn.question(topicId, level, seen)
            typed = ""
            hintUsed = false
            hintShown = false
            revealed = null
            startedAt = System.currentTimeMillis()
            elapsedTick = 0
        }
    }
    LaunchedEffect(index, revealed, speed) {
        if (speed && revealed == null) {
            while (true) {
                delay(350)
                elapsedTick++
            }
        }
    }

    fun submit(answer: String) {
        val question = current ?: return
        if (revealed != null) return
        val correct = question.isCorrect(answer)
        if (correct) {
            right++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            if (!warmup) {
                val elapsed = (System.currentTimeMillis() - startedAt) / 1000f
                score += if (hintUsed) 5 else 10
                score += minOf(streak, 5) * 2
                if (speed && elapsed < 10f && !hintUsed) score += 5
            }
        } else streak = 0
        attempts += Attempt(question, correct, hintUsed)
        if (warmup) onWarmupAnswer?.invoke(question, correct)
        revealed = correct
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕  Exit") }
            LinearProgressIndicator(index.toFloat() / total.coerceAtLeast(1), Modifier.weight(1f).padding(horizontal = 8.dp))
            if (warmup) Text("🎯 ${minOf(index + 1, total)} / $total", fontWeight = FontWeight.Bold)
            else Text("⭐ $score", fontWeight = FontWeight.Bold)
        }
        if (speed) {
            val seconds = elapsedTick * .35f
            LinearProgressIndicator((1f - seconds / 12f).coerceIn(0f, 1f), Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
        }
        val question = current
        if (question != null) {
            val topic = AdaptiveMathLearn.topic(question.topicId)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${topic.emoji} ${topic.name}${if (warmup) "" else " · L${question.level}"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (question.prompt.isNotBlank()) Text(question.prompt, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(question.body, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (question.numericInput) {
                        OutlinedTextField(
                            typed, { typed = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Type your answer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) submit(typed) }),
                            enabled = revealed == null,
                            singleLine = true
                        )
                        Button(enabled = revealed == null && typed.isNotBlank(), onClick = { submit(typed) }, modifier = Modifier.fillMaxWidth()) { Text("Check") }
                    } else {
                        question.choices.forEachIndexed { choiceIndex, choice ->
                            val correctChoice = question.isCorrect(choice)
                            val bg = when {
                                revealed != null && correctChoice -> MaterialTheme.colorScheme.primaryContainer
                                revealed == false && typed == choice -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(
                                Modifier.fillMaxWidth().clickable(enabled = revealed == null) { typed = choice; submit(choice) },
                                color = bg, shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${choiceIndex + 1}.", Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                                    Text(choice, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    if (revealed == null && !warmup) TextButton(enabled = !hintShown, onClick = {
                        hintUsed = true
                        hintShown = true
                    }) { Text("💡 Hint") }
                    if (hintShown) Text("💡 ${question.hint}", color = MaterialTheme.colorScheme.tertiary)
                    revealed?.let { correct ->
                        Text(if (correct) "✅ Nice one!" else "🌱 No worries — that’s how brains grow.", fontWeight = FontWeight.Bold,
                            color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        if (!correct) Text("Answer: ${question.answer}", fontWeight = FontWeight.SemiBold)
                        Text(question.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {
                            if (index + 1 >= total) {
                                onDone(QuizSummary(score, right, total, bestStreak, attempts.toList()))
                            } else index++
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (index + 1 >= total) "Finish 🎉" else "Next →") }
                    }
                }
            }
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}


@Composable
private fun ArcadeScreen(
    node: CourseNode,
    levels: Map<String, Int>,
    onExit: () -> Unit,
    onDone: (QuizSummary) -> Unit
) {
    val isBlitz = node.kind == "blitz"
    val duration = if (isBlitz) 60 else 45
    val livesMax = if (isBlitz) Int.MAX_VALUE else 3
    val topicIds = node.topics.ifEmpty { listOf("arith", "frac") }
    val seen = remember(node.id) { mutableSetOf<String>() }
    var remaining by remember(node.id) { mutableIntStateOf(duration) }
    var lives by remember(node.id) { mutableIntStateOf(livesMax) }
    var score by remember(node.id) { mutableIntStateOf(0) }
    var right by remember(node.id) { mutableIntStateOf(0) }
    var total by remember(node.id) { mutableIntStateOf(0) }
    var streak by remember(node.id) { mutableIntStateOf(0) }
    var bestStreak by remember(node.id) { mutableIntStateOf(0) }
    var index by remember(node.id) { mutableIntStateOf(0) }
    var question by remember(node.id) { mutableStateOf<MathQuestion?>(null) }
    var answered by remember(node.id) { mutableStateOf(false) }
    var startedAt by remember(node.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var finished by remember(node.id) { mutableStateOf(false) }
    val attempts = remember(node.id) { mutableStateListOf<Attempt>() }

    fun nextQuestion() {
        val topicId = topicIds[index % topicIds.size]
        val level = (levels[topicId] ?: 1).coerceIn(1, 3)
        var picked = AdaptiveMathLearn.question(topicId, level, seen)
        var tries = 0
        while (picked.choices.isEmpty() && tries++ < 20) picked = AdaptiveMathLearn.question(topicId, level, seen)
        if (picked.choices.isEmpty()) {
            val value = picked.answer.replace("−", "-").toDoubleOrNull() ?: 0.0
            picked = picked.copy(choices = listOf(picked.answer, (value + 1).toString(), (value - 1).toString(), (value + 2).toString()).distinct())
        }
        question = picked
        startedAt = System.currentTimeMillis()
        answered = false
    }
    fun finish() {
        if (finished) return
        finished = true
        onDone(QuizSummary(score, right, total, bestStreak, attempts.toList()))
    }
    LaunchedEffect(node.id) { nextQuestion() }
    LaunchedEffect(node.id) {
        while (!finished && remaining > 0 && lives > 0) {
            delay(1_000)
            remaining--
        }
        if (!finished && (remaining <= 0 || lives <= 0)) finish()
    }
    LaunchedEffect(node.id, index) {
        if (!isBlitz && question != null && !finished) {
            val questionIndex = index
            val interval = (8_500L - ((duration - remaining) * 1_000L / 12L)).coerceAtLeast(4_800L)
            delay(interval)
            if (!finished && index == questionIndex && !answered && lives > 0) {
                val missed = question
                if (missed != null) { total++; attempts += Attempt(missed, false, false) }
                lives--; streak = 0; index++
                if (lives <= 0) finish() else nextQuestion()
            }
        }
    }

    fun answer(value: String) {
        if (answered || finished) return
        val q = question ?: return
        val correct = q.isCorrect(value)
        answered = true
        total++
        attempts += Attempt(q, correct, false)
        if (correct) {
            right++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            if (isBlitz) score += 10 * minOf(4, 1 + streak / 3)
            else {
                val elapsed = (System.currentTimeMillis() - startedAt) / 1000f
                score += 10 + minOf(streak, 6) * 2 + if (elapsed < 4f) 5 else 0
            }
        } else {
            streak = 0
            if (!isBlitz) lives--
        }
        index++
        if (!isBlitz && lives <= 0) finish() else nextQuestion()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕ Exit") }
            Spacer(Modifier.weight(1f))
            Text(if (isBlitz) "⚡ 60s Blitz" else "🫧 Bubble Pop", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(remaining / duration.toFloat(), Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⏱ $remaining s", fontWeight = FontWeight.Bold)
            Text(if (isBlitz) "🔥 $streak streak" else (1..3).joinToString("") { if (it <= lives) "❤️" else "♡" }, fontWeight = FontWeight.Bold)
            Text("⭐ $score", fontWeight = FontWeight.Bold)
        }
        question?.let { q ->
            val topic = AdaptiveMathLearn.topic(q.topicId)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${topic.emoji} ${topic.name} · L${q.level}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (q.prompt.isNotBlank()) Text(q.prompt, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(q.body, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    if (isBlitz) {
                        q.choices.forEach { choice ->
                            Button(onClick = { answer(choice) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(choice, fontSize = 19.sp) }
                        }
                    } else {
                        val bubbleColors = listOf(Color(0xFFFF7C98), Color(0xFF64C7FF), Color(0xFFFFC857), Color(0xFF9D83FF))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            q.choices.forEachIndexed { choiceIndex, choice ->
                                Surface(
                                    onClick = { answer(choice) },
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    shape = CircleShape,
                                    color = bubbleColors[choiceIndex % bubbleColors.size],
                                    shadowElevation = 5.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text(choice, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(if (isBlitz) "Answer quickly to build a combo." else "You have three lives. Tap the right answer before time runs out.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RunResultScreen(run: CompletedRun, onContinue: () -> Unit, onReplay: () -> Unit, onRetake: () -> Unit) {
    val title = AdaptiveMathLearn.nodeTitle(run.node)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (run.stars == 3) "🌟" else if (run.stars == 2) "⭐" else "💪", fontSize = 58.sp)
        Text("${if (run.node.kind == "boss") "Final Quest" else "Quest complete!"}", fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"★".repeat(run.stars)}${"☆".repeat(3 - run.stars)}", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
                Text("${run.xp} XP earned", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${run.right} / ${run.total} correct  ·  ${run.score} points")
                if (run.bestStreak > 0) Text("🔥 Best streak: ${run.bestStreak}")
                Text(if (run.best) "🏆 New personal best!" else "Keep practising to beat your best.", color = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (run.missed.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick revision", fontWeight = FontWeight.Bold)
                    run.missed.take(5).forEach { miss ->
                        val topic = AdaptiveMathLearn.topic(miss.question.topicId)
                        Text("${topic.emoji} ${miss.question.prompt} ${miss.question.body}\nAnswer: ${miss.question.answer}\n${miss.question.explanation}", fontSize = 13.sp)
                    }
                }
            }
        } else Text("🌟 Flawless — nothing to revise!", color = MaterialTheme.colorScheme.tertiary)
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Back to my course") }
        TextButton(onClick = onReplay) { Text("Play again") }
        if (run.node.kind == "boss") TextButton(onClick = onRetake) { Text("Retake the warm-up for a new course") }
    }
}

@Composable
fun ExternalExerciseQuiz(title: String, exercises: List<Exercise>, onExit: () -> Unit) {
    var index by remember(title, exercises) { mutableIntStateOf(0) }
    var text by remember(title, exercises, index) { mutableStateOf("") }
    var blanks by remember(title, exercises, index) { mutableStateOf(emptyList<String>()) }
    var selected by remember(title, exercises, index) { mutableStateOf(setOf<String>()) }
    var reveal by remember(title, exercises, index) { mutableStateOf<Boolean?>(null) }
    var correctCount by remember(title, exercises) { mutableIntStateOf(0) }
    if (exercises.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onExit) { Text("← Back") }
            Text("No practice questions are available yet.")
        }
        return
    }
    if (index >= exercises.size) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉", fontSize = 56.sp)
            Text("Practice complete!", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("$correctCount of ${exercises.size} correct")
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Back to lesson") }
        }
        return
    }
    val ex = exercises[index]
    var ordered by remember(title, exercises, index) { mutableStateOf(ex.steps.shuffled()) }
    var matches by remember(title, exercises, index) { mutableStateOf(ex.pairs.associate { it.first to "" }) }
    fun submit(given: Any?) {
        if (reveal != null) return
        val ok = ex.isRight(given)
        if (ok) correctCount++
        reveal = ok
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕ Exit") }
            LinearProgressIndicator(index.toFloat() / exercises.size, Modifier.weight(1f).padding(horizontal = 8.dp))
            Text("${index + 1}/${exercises.size}", fontWeight = FontWeight.Bold)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ex.prompt.ifBlank { title }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(ex.q, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                when {
                    ex.type == "order" -> {
                        ordered.forEachIndexed { row, item ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(item, Modifier.weight(1f))
                                TextButton(enabled = reveal == null && row > 0, onClick = { ordered = ordered.toMutableList().also { val v = it[row]; it[row] = it[row - 1]; it[row - 1] = v } }) { Text("↑") }
                                TextButton(enabled = reveal == null && row < ordered.lastIndex, onClick = { ordered = ordered.toMutableList().also { val v = it[row]; it[row] = it[row + 1]; it[row + 1] = v } }) { Text("↓") }
                            }
                        }
                    }
                    ex.type == "match" -> ex.pairs.forEach { pair ->
                        OutlinedTextField(matches[pair.first].orEmpty(), { v -> matches = matches.toMutableMap().also { it[pair.first] = v } },
                            modifier = Modifier.fillMaxWidth(), label = { Text(pair.first) }, enabled = reveal == null, singleLine = true)
                    }
                    ex.type == "multi" -> ex.choices.forEach { choice ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = reveal == null) {
                            selected = if (choice in selected) selected - choice else selected + choice
                        }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(choice in selected, onCheckedChange = { checked -> selected = if (checked) selected + choice else selected - choice }, enabled = reveal == null)
                            Text(choice)
                        }
                    }
                    ex.type == "blank" -> {
                        val count = ex.blankCount
                        if (blanks.size != count) blanks = List(count) { "" }
                        repeat(count) { pos ->
                            OutlinedTextField(blanks[pos], { v -> blanks = blanks.toMutableList().also { it[pos] = v } },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Blank ${pos + 1}") }, enabled = reveal == null, singleLine = true)
                        }
                    }
                    ex.choices.isNotEmpty() -> ex.choices.forEach { choice ->
                        Surface(Modifier.fillMaxWidth().clickable(enabled = reveal == null) { text = choice; submit(choice) },
                            color = if (text == choice && reveal == false) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)) {
                            Text(choice, Modifier.padding(12.dp))
                        }
                    }
                    else -> OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Your answer") }, enabled = reveal == null, singleLine = true)
                }
                if (ex.type == "multi" && reveal == null) Button(onClick = { submit(selected.toList()) }, modifier = Modifier.fillMaxWidth()) { Text("Check answers") }
                if (ex.type == "order" && reveal == null) Button(onClick = { submit(ordered) }, modifier = Modifier.fillMaxWidth()) { Text("Check order") }
                if (ex.type == "match" && reveal == null) Button(onClick = { submit(matches) }, enabled = matches.values.all(String::isNotBlank), modifier = Modifier.fillMaxWidth()) { Text("Check matches") }
                if (ex.type == "blank" && reveal == null) Button(onClick = { submit(blanks) }, enabled = blanks.all(String::isNotBlank), modifier = Modifier.fillMaxWidth()) { Text("Check") }
                if (ex.choices.isEmpty() && ex.type !in setOf("blank", "multi", "order", "match") && reveal == null) Button(onClick = { submit(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Check") }
                if (ex.hint.isNotBlank()) Text("💡 ${ex.hint}", color = MaterialTheme.colorScheme.tertiary)
                reveal?.let { ok ->
                    Text(if (ok) "✅ Correct!" else "🌱 Keep going — the worked steps can help.", fontWeight = FontWeight.Bold,
                        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    if (!ok) Text("Answer: ${ex.answer}")
                    if (ex.steps.isNotEmpty()) Text(ex.steps.joinToString("\n"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { index++; reveal = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (index + 1 == exercises.size) "See results" else "Next →")
                    }
                }
            }
        }
    }
}
