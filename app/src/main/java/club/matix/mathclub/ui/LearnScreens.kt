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
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Math Learn: units > chapters > lessons, hearts, XP, crowns. Port of the web app's renderLearn. */
@Composable
fun LearnScreen(store: Store, me: String, isOwner: Boolean) {
    var state by remember { mutableStateOf<LearnState?>(null) }
    var failed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<Lesson?>(null) }
    var teaching by remember { mutableStateOf<Lesson?>(null) }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var sub by remember { mutableStateOf("anything") }
    var assessment by remember { mutableStateOf<LearningAssessment?>(null) }

    LaunchedEffect(me, tick) {
        state = withContext(Dispatchers.IO) { runCatching { Learn.load(me) }.getOrNull() }
        failed = state == null
    }
    LaunchedEffect(me, sub) {
        if (sub == "quests") {
            while (isActive) {
                delay(15_000)
                tick++
            }
        }
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
        assessment != null -> {
            val a = assessment!!
            QuizScreen(
                title = a.title, exercises = a.exercises, showHearts = false, state = s,
                me = me, lessonId = null, mode = a.mode, targetId = a.targetId,
                onExit = { assessment = null; tick++ }
            )
        }
        p != null -> QuizScreen(
            title = p.title, exercises = p.ex, showHearts = !isOwner, state = s, me = me, lessonId = p.id,
            mode = "lesson",
            onExit = {
                playing = null; tick++
                scope.launch(Dispatchers.IO) { Learn.awardBadges(me, s) }
            }
        )
        t != null -> TeachScreen(t, onStart = { playing = t; teaching = null }, onBack = { teaching = null })
        else -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.horizontalScrollCompat().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = buildList {
                    add("anything" to "✨ Learn anything")
                    add("path" to "🗺 Course path")
                    add("practice" to "🧠 Practice")
                    add("quests" to "🎯 Quests")
                    add("tests" to "🧭 Tests")
                    add("shop" to "🛒 Shop")
                    add("badges" to "🏅 Badges")
                    add("board" to "🏆 Board")
                    if (isOwner) add("edit" to "✏️ Edit course")
                }
                tabs.forEach { (id, l) ->
                    FilterChip(sub == id, { sub = id }, label = { Text(l) })
                }
            }
            when (sub) {
                "anything" -> LearnAnythingScreen(store, me, isOwner, s, onPractice = { assessment = it }, onRefresh = { tick++ })
                "practice" -> LearnPracticeScreen(me, s) { run ->
                    assessment = run
                }
                "quests" -> LearnQuestsScreen(me, s) { friend ->
                    scope.launch {
                        withContext(Dispatchers.IO) { Learn.startFriendQuest(me, friend) }
                        tick++
                    }
                }
                "tests" -> LearnTestsScreen(store, me, s) { assessment = it }
                "edit" -> if (isOwner) LearnCourseEditor(me, s) { tick++ }
                "shop" -> LearnShop(s, me) { tick++ }
                "badges" -> LearnBadges(s)
                "board" -> LearnBoard(me)
                else -> key(tick) { LearnPath(s, isOwner, onOpen = { l ->
                    when {
                        l.ex.isEmpty() -> {}
                        !isOwner && s.currentHearts() <= 0 -> {}
                        l.howto.isBlank() -> playing = l
                        else -> teaching = l
                    }
                }, onTest = { kind, id ->
                    val tests = when (kind) {
                        "unit-test" -> s.units.firstOrNull { it.id == id }?.test.orEmpty()
                        else -> s.chapters.firstOrNull { it.id == id }?.test.orEmpty()
                    }
                    assessment = LearningAssessment(
                        if (kind == "unit-test") s.units.firstOrNull { it.id == id }?.title ?: "Unit final test"
                        else s.chapters.firstOrNull { it.id == id }?.title ?: "Chapter final test",
                        tests, kind, id
                    )
                }) }
            }
        }
    }
}

@Composable
private fun LearnPath(s: LearnState, isOwner: Boolean, onOpen: (Lesson) -> Unit, onTest: (String, String) -> Unit) {
    val hearts = s.currentHearts()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🦉 Math Learn", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⭐ ${s.xp} XP"); Text("💎 ${s.gems}"); Text("🔥 ${s.streak}")
            Text(if (isOwner) "❤ ∞" else "❤ $hearts/$LRN_MAX_HEARTS")
        }
        if (!isOwner && hearts <= 0) Text("Out of hearts — one comes back every 20 minutes.", color = MaterialTheme.colorScheme.error)
        if (s.lessons.isEmpty()) Text("No lessons yet — check back soon!")
        s.units.forEachIndexed { unitIndex, u ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(u.icon, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (u.description.isNotBlank()) Text(u.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (s.unitDone(u)) "✅" else "⭐")
            }
            if (u.test.isNotEmpty()) {
                OutlinedButton(enabled = isOwner || s.units.take(unitIndex).all { s.unitDone(it) }, onClick = { onTest("unit-test", u.id) }) {
                    Text("🏆 Unit final test · ${u.test.size} questions · no hearts")
                }
            }
            s.chapters.filter { it.unitId == u.id }.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.icon, fontSize = 17.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(c.title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Text(if (s.chapterSkipped(c.id) || s.lessonsOf(c.id).isNotEmpty() && s.lessonsOf(c.id).all { s.lessonCleared(it) }) "✅" else "")
                }
                if (c.test.isNotEmpty()) {
                    OutlinedButton(enabled = isOwner || (s.units.take(unitIndex).all { s.unitDone(it) } && s.lessonsOf(c.id).any { s.lessonCleared(it) }),
                        onClick = { onTest("chapter-test", c.id) }) {
                        Text("⚡ Chapter skip test · ${c.test.size} questions")
                    }
                }
                s.lessonsOf(c.id).forEach { l -> LessonRow(l, s.crowns(l.id), s.lessonOpen(l, isOwner), onOpen) }
            }
            s.looseLessons(u.id).forEach { l -> LessonRow(l, s.crowns(l.id), s.lessonOpen(l, isOwner), onOpen) }
        }
        val orphan = s.lessons.filter { l -> l.unitId == null && s.chapters.none { it.id == l.chapterId } }
        orphan.forEach { l -> LessonRow(l, s.crowns(l.id), isOwner, onOpen) }
    }
}

@Composable
private fun LessonRow(l: Lesson, crowns: Int, open: Boolean, onOpen: (Lesson) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = open && l.ex.isNotEmpty()) { onOpen(l) }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.title, fontWeight = FontWeight.Bold)
                Text(if (l.ex.isEmpty()) "Still being written" else "${l.ex.size} questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (crowns > 0) "👑".repeat(crowns) else if (open) "▶" else "🔒")
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
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Got it — let's go") }
        TextButton(onClick = onBack) { Text("Back to the map") }
    }
}

/** Plays a list of exercises. Handles mc, tf, input, order, match, blank, multi. */
@Composable
fun QuizScreen(
    title: String, exercises: List<Exercise>, showHearts: Boolean, state: LearnState?, me: String,
    lessonId: String?, mode: String = "lesson", targetId: String? = null, onExit: () -> Unit
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
            Button(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) { Text("Back to the map") }
        }
        return
    }
    if (i >= exercises.size) {
        LaunchedEffect(Unit) {
            xpGot = withContext(Dispatchers.IO) {
                if (state == null) 0 else runCatching {
                    if (lessonId != null) {
                        val xp = Learn.finish(me, state, lessonId, right, wrong)
                        Learn.markLessonFinished(me, state, right, wrong, xp)
                        xp
                    } else Learn.finishTest(me, state, mode, targetId, right, wrong)
                }.getOrDefault(0)
            }
        }
        val asked = right + wrong
        val pct = if (asked == 0) 0 else Math.round(right * 100f / asked)
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (wrong == 0) "🎉 Perfect!" else if (pct >= LRN_PASS) "✅ Lesson complete!" else "Keep practising!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("$right of $asked right ($pct%)")
            xpGot?.let { Text("⭐ +$it XP") }
            Button(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) { Text("Continue") }
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
            LinearProgressIndicator(progress = { i / exercises.size.toFloat() }, modifier = Modifier.weight(1f))
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
                    if (state != null && mode != "placement") {
                        scope.launch(Dispatchers.IO) {
                            if (ok) Learn.clearMistake(me, state, e) else Learn.recordMistake(me, state, e)
                        }
                    }
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

@Composable
private fun LearnShop(s: LearnState, me: String, refresh: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    var gems by remember { mutableStateOf(s.gems) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🛒 Shop", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("💎 $gems gems in your pocket", fontWeight = FontWeight.SemiBold)
        Text("Earn more from perfect lessons.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary)
        Learn.Shop.forEach { it ->
            val off = (it.id == "hearts" && s.currentHearts() >= LRN_MAX_HEARTS) || (it.id == "boost" && s.boosted()) || (it.id == "freeze" && s.freeze)
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it.icon, fontSize = 26.sp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(it.name, fontWeight = FontWeight.Bold); Text(it.desc, fontSize = 12.sp) }
                    Button(enabled = !off && gems >= it.cost, onClick = {
                        scope.launch {
                            msg = withContext(Dispatchers.IO) { Learn.buy(me, s, it.id) }
                            gems = s.gems; refresh()
                        }
                    }) { Text(if (off) "Owned" else "💎 ${it.cost}") }
                }
            }
        }
    }
}

@Composable
private fun LearnBadges(s: LearnState) {
    val st = Learn.stats(s)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🏅 Badges", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("You have ${s.badges.size} of ${Learn.Badges.size}. ${s.xp} XP · ${st.lessons} lessons done · ${st.streak} day streak.", fontSize = 13.sp)
        Learn.Badges.forEach { b ->
            val have = b.id in s.badges
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (have) b.icon else "🔒", fontSize = 24.sp); Spacer(Modifier.width(10.dp))
                    Column { Text(b.name, fontWeight = FontWeight.Bold); Text(b.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun LearnBoard(me: String) {
    var rows by remember { mutableStateOf<List<Pair<String, Int>>?>(null) }
    LaunchedEffect(Unit) { rows = withContext(Dispatchers.IO) { runCatching { Learn.leaderboard() }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("🏆 Leaderboard", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Everyone in the club, ranked by total XP.", fontSize = 13.sp)
        val r = rows
        if (r == null) CircularProgressIndicator()
        else if (r.isEmpty()) Text("Nobody has earned any XP yet — be the first!")
        else r.forEachIndexed { i, (u, xp) ->
            val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${i + 1}" }
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(medal, Modifier.width(44.dp), fontWeight = FontWeight.Bold)
                    Text(u + if (Auth.normalize(u) == Auth.normalize(me)) "  (you)" else "", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("$xp XP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
