package club.matix.mathclub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import club.matix.mathclub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Math Learn: units > chapters > lessons, hearts, XP, crowns. Port of the web app's renderLearn. */
@Composable
fun LearnScreen(me: String, isOwner: Boolean) {
    var state by remember { mutableStateOf<LearnState?>(null) }
    var failed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<Lesson?>(null) }
    var teaching by remember { mutableStateOf<Lesson?>(null) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(me) {
        state = withContext(Dispatchers.IO) { runCatching { Learn.load(me) }.getOrNull() }
        failed = state == null
    }
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (failed) Text("Couldn't load Math Learn. Check your internet.") else CircularProgressIndicator()
        }
        return
    }

    val t = teaching
    val p = playing
    when {
        p != null -> QuizScreen(
            title = p.title, exercises = p.ex, showHearts = !isOwner, state = s, me = me, lessonId = p.id,
            onExit = { playing = null; tick++ }
        )
        t != null -> TeachScreen(t, onStart = { playing = t; teaching = null }, onBack = { teaching = null })
        else -> key(tick) { LearnPath(s, isOwner, onOpen = { l ->
            when {
                l.ex.isEmpty() -> {}
                !isOwner && s.currentHearts() <= 0 -> {}
                l.howto.isBlank() -> playing = l
                else -> teaching = l
            }
        }) }
    }
}

@Composable
private fun LearnPath(s: LearnState, isOwner: Boolean, onOpen: (Lesson) -> Unit) {
    val hearts = s.currentHearts()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🦉 Math Learn", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⭐ ${s.xp} XP"); Text("💎 ${s.gems}"); Text("🔥 ${s.streak}")
            Text(if (isOwner) "❤ ∞" else "❤ $hearts/$LRN_MAX_HEARTS")
        }
        if (!isOwner && hearts <= 0) Text("Out of hearts — one comes back every 20 minutes.", color = MaterialTheme.colorScheme.error)
        if (s.lessons.isEmpty()) Text("No lessons yet — check back soon!")
        s.units.forEach { u ->
            Text(u.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            s.chapters.filter { it.unitId == u.id }.forEach { c ->
                Text(c.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                s.lessonsOf(c.id).forEach { l -> LessonRow(l, s.crowns(l.id), onOpen) }
            }
            s.looseLessons(u.id).forEach { l -> LessonRow(l, s.crowns(l.id), onOpen) }
        }
        val orphan = s.lessons.filter { l -> l.unitId == null && s.chapters.none { it.id == l.chapterId } }
        orphan.forEach { l -> LessonRow(l, s.crowns(l.id), onOpen) }
    }
}

@Composable
private fun LessonRow(l: Lesson, crowns: Int, onOpen: (Lesson) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(l) }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.title, fontWeight = FontWeight.Bold)
                Text(if (l.ex.isEmpty()) "Still being written" else "${l.ex.size} questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (crowns > 0) "👑".repeat(crowns) else "▶")
        }
    }
}

@Composable
private fun TeachScreen(l: Lesson, onStart: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(l.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(l.howto)
        if (l.hints.isNotEmpty()) {
            Text("💡 Keep in mind", fontWeight = FontWeight.Bold)
            l.hints.forEach { Text("• $it") }
        }
        Button(onClick = onStart, Modifier.fillMaxWidth()) { Text("Got it — let's go") }
        TextButton(onClick = onBack) { Text("Back to the map") }
    }
}

/** Plays a list of exercises. Handles mc, tf, input, order, match, blank, multi. */
@Composable
fun QuizScreen(
    title: String, exercises: List<Exercise>, showHearts: Boolean, state: LearnState?, me: String,
    lessonId: String?, onExit: () -> Unit
) {
    var i by remember { mutableStateOf(0) }
    var right by remember { mutableStateOf(0) }
    var wrong by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf<Boolean?>(null) }
    var xpGot by remember { mutableStateOf<Int?>(null) }
    var outOfHearts by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (outOfHearts) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💔 Out of hearts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("A heart comes back every 20 minutes.")
            Button(onClick = onExit, Modifier.padding(top = 16.dp)) { Text("Back to the map") }
        }
        return
    }
    if (i >= exercises.size) {
        LaunchedEffect(Unit) {
            xpGot = withContext(Dispatchers.IO) {
                if (state != null) runCatching { Learn.finish(me, state, lessonId, right, wrong) }.getOrDefault(0) else 0
            }
        }
        val asked = right + wrong
        val pct = if (asked == 0) 0 else Math.round(right * 100f / asked)
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (wrong == 0) "🎉 Perfect!" else if (pct >= LRN_PASS) "✅ Lesson complete!" else "Keep practising!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("$right of $asked right ($pct%)")
            xpGot?.let { Text("⭐ +$it XP") }
            Button(onClick = onExit, Modifier.padding(top = 16.dp)) { Text("Continue") }
        }
        return
    }

    val e = exercises[i]
    var sel by remember(i) { mutableStateOf<String?>(null) }
    var multi by remember(i) { mutableStateOf(listOf<String>()) }
    var typed by remember(i) { mutableStateOf("") }
    var order by remember(i) { mutableStateOf(listOf<String>()) }
    var gaps by remember(i) { mutableStateOf(mapOf<Int, String>()) }
    var matchL by remember(i) { mutableStateOf<String?>(null) }
    var matched by remember(i) { mutableStateOf(mapOf<String, String>()) }
    val shuffledSteps = remember(i) { e.steps.shuffled() }
    val shuffledRights = remember(i) { e.pairs.map { it.second }.shuffled() }
    val bank = remember(i) { (e.answer.split("|") + e.choices).filter { it.isNotBlank() }.distinct().shuffled() }

    fun given(): Any? = when (e.type) {
        "mc", "tf" -> sel
        "multi" -> multi
        "order" -> order
        "match" -> matched
        "blank" -> (0 until e.blankCount).map { gaps[it] }
        else -> typed
    }
    fun complete(): Boolean = when (e.type) {
        "mc", "tf" -> sel != null
        "multi" -> multi.isNotEmpty()
        "order" -> order.size == e.steps.size
        "match" -> matched.size == e.pairs.size
        "blank" -> (0 until e.blankCount).all { gaps[it] != null }
        else -> typed.isNotBlank()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕") }
            LinearProgressIndicator(progress = { i / exercises.size.toFloat() }, Modifier.weight(1f))
            if (showHearts && state != null) Text("  ❤ ${state.currentHearts()}")
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$title · ${i + 1} of ${exercises.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(e.prompt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (e.type != "blank" && e.q.isNotBlank()) Text(e.q, fontSize = 26.sp, fontWeight = FontWeight.Black)
            val locked = answered != null
            when (e.type) {
                "mc", "tf" -> (if (e.type == "tf" && e.choices.isEmpty()) listOf("True", "False") else e.choices).forEach { c ->
                    OptionRow(c, sel == c, locked) { sel = c }
                }
                "multi" -> e.choices.forEach { c ->
                    OptionRow(c, c in multi, locked) { multi = if (c in multi) multi - c else multi + c }
                }
                "order" -> {
                    Text("Tap the steps in order:", fontSize = 13.sp)
                    Text(order.mapIndexed { n, s -> "${n + 1}. $s" }.joinToString("\n").ifEmpty { "—" })
                    shuffledSteps.forEach { st -> OptionRow(st, st in order, locked || st in order) { order = order + st } }
                    if (order.isNotEmpty() && !locked) TextButton(onClick = { order = emptyList() }) { Text("Reset") }
                }
                "match" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        e.pairs.forEach { (l, _) -> OptionRow(l + (matched[l]?.let { " → $it" } ?: ""), matchL == l, locked || matched.containsKey(l)) { matchL = l } }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shuffledRights.forEach { r ->
                            OptionRow(r, false, locked || r in matched.values || matchL == null) {
                                matchL?.let { matched = matched + (it to r); matchL = null }
                            }
                        }
                    }
                }
                "blank" -> {
                    Text(e.blanks.mapIndexed { n, part -> part + if (n < e.blankCount) "[${gaps[n] ?: "___"}]" else "" }.joinToString(""), fontSize = 20.sp)
                    bank.forEach { w ->
                        OptionRow(w, false, locked) {
                            val next = (0 until e.blankCount).firstOrNull { gaps[it] == null }
                            if (next != null) gaps = gaps + (next to w)
                        }
                    }
                    if (gaps.isNotEmpty() && !locked) TextButton(onClick = { gaps = emptyMap() }) { Text("Clear") }
                }
                else -> OutlinedTextField(typed, { typed = it }, enabled = !locked, singleLine = true, label = { Text("Your answer") }, modifier = Modifier.fillMaxWidth())
            }
            answered?.let { ok ->
                Text(
                    if (ok) "✅ Correct!" else "❌ Not quite. Answer: " + (if (e.type == "order") e.steps.joinToString(" → ") else if (e.type == "match") e.pairs.joinToString { "${it.first}=${it.second}" } else e.answer.replace("|", " / ")),
                    fontWeight = FontWeight.Bold, color = if (ok) Color(0xFF1F9D55) else MaterialTheme.colorScheme.error
                )
                if (!ok && e.hint.isNotBlank()) Text("💡 ${e.hint}")
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(), enabled = answered != null || complete(),
            onClick = {
                if (answered == null) {
                    val ok = e.isRight(given())
                    answered = ok
                    if (ok) right++ else {
                        wrong++
                        if (showHearts && state != null) {
                            state.hearts = (state.currentHearts() - 1).coerceAtLeast(0); state.heartsAt = System.currentTimeMillis()
                            scope.launch(Dispatchers.IO) { Learn.saveHearts(me, state) }
                        }
                    }
                } else {
                    if (showHearts && state != null && state.currentHearts() <= 0 && answered == false) outOfHearts = true
                    else { i++; answered = null }
                }
            }
        ) { Text(if (answered == null) "Check" else "Continue") }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, disabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !disabled, onClick = onClick).padding(14.dp)
    ) { Text(text, color = if (disabled && !selected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface) }
}
