package club.matix.mathclub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LearningAssessment(
    val title: String,
    val exercises: List<Exercise>,
    val mode: String,
    val targetId: String? = null
)

private data class DailyQuest(val id: String, val title: String, val goal: Int, val reward: Int)
private data class PracticeRound(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<Exercise>
)
private val dailyQuests = listOf(
    DailyQuest("xp30", "Earn 30 XP", 30, 5),
    DailyQuest("les2", "Finish 2 lessons", 2, 5),
    DailyQuest("cor15", "Get 15 answers right", 15, 5),
    DailyQuest("perf", "One perfect lesson", 1, 10)
)

@Composable
fun LearnQuestsScreen(me: String, state: LearnState, onStartFriendQuest: (String) -> Unit) {
    var selectedFriend by remember { mutableStateOf("") }
    var friendMenu by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    val totalGems = dailyQuests.sumOf { it.reward }
    LaunchedEffect(state.friendQuests.size) { starting = false }
    val friendQuest = state.friendQuests.entries.firstOrNull()
    val finishedFriendQuests = state.friendQuests.values.count { it.optBoolean("done") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎯 Daily quests", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Quests reset each day. Complete them to earn up to $totalGems gems.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        dailyQuests.forEach { quest ->
            val current = (state.quests[quest.id] ?: 0).coerceIn(0, quest.goal)
            val completed = current >= quest.goal
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (completed) "✅" else "🎯", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(quest.title, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator({ current / quest.goal.toFloat() }, Modifier.fillMaxWidth().padding(vertical = 5.dp))
                        Text("$current / ${quest.goal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("+${quest.reward} 💎", fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Friend quests", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        if (friendQuest != null) {
            val quest = friendQuest.value
            val a = Auth.normalize(quest.optString("a"))
            val b = Auth.normalize(quest.optString("b"))
            val friend = if (a == Auth.normalize(me)) b else a
            val total = quest.optInt("pa") + quest.optInt("pb")
            val goal = quest.optInt("goal", 200).coerceAtLeast(1)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (quest.optBoolean("done")) "✅ Quest complete · You + @$friend" else "👥 You + @$friend", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator({ (total / goal.toFloat()).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                    Text(if (quest.optBoolean("done")) "$total / $goal XP · +15 💎 each" else "$total / $goal XP together · +15 💎 each when complete",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!quest.optBoolean("done")) Text("Finish lessons and tests to contribute XP to your shared goal.")
        } else {
            Text("Team up with a club member and earn 200 XP between you for 15 gems each.")
            Box {
                OutlinedButton(onClick = { friendMenu = true }, enabled = state.friendMembers.isNotEmpty()) {
                    Text(if (selectedFriend.isBlank()) "Choose a friend" else "@$selectedFriend")
                }
                DropdownMenu(expanded = friendMenu, onDismissRequest = { friendMenu = false }) {
                    state.friendMembers.forEach { friend ->
                        DropdownMenuItem(text = { Text("@$friend") }, onClick = { selectedFriend = friend; friendMenu = false })
                    }
                }
            }
            Button(
                enabled = selectedFriend.isNotBlank() && !starting,
                onClick = { starting = true; onStartFriendQuest(selectedFriend) }
            ) { Text(if (starting) "Starting…" else "Start 200-XP friend quest") }
            if (state.friendMembers.isEmpty()) Text("No other club members are available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (finishedFriendQuests > 0) {
            Text("Completed friend quests: $finishedFriendQuests · each awarded 15 💎.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LearnPracticeScreen(me: String, state: LearnState, onStart: (LearningAssessment) -> Unit) {
    val completedLessons = state.lessons.filter { state.lessonCleared(it) }
    val all = completedLessons.flatMap { it.ex }
    val difficult = all.filter { it.type in setOf("order", "match", "blank", "multi") }
    val rounds = listOf(
        PracticeRound("mistakes", "My mistakes", "${state.mistakes.size} questions you previously missed. Correct answers leave your practice list.", state.mistakes.toList()),
        PracticeRound("mix", "Quick mix", "Ten random questions from completed lessons.", all.shuffled().take(10)),
        PracticeRound("long", "Marathon", "Twenty-five questions in a row. No hearts at stake.", all.shuffled().take(25)),
        PracticeRound("hard", "Tricky only", "Ordering, matching, multi-select and fill-the-gap questions.", difficult.shuffled().take(12))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🧠 Practice", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Practice never costs hearts. Questions you miss return in My mistakes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        rounds.forEach { (id, title, description, questions) ->
            val enabled = questions.isNotEmpty()
            Card(Modifier.fillMaxWidth().clickable(enabled = enabled) {
                onStart(LearningAssessment(title, questions, "practice"))
            }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when (id) { "mistakes" -> "💔"; "mix" -> "✨"; "long" -> "⚡"; else -> "🧩" }, fontSize = 25.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(if (enabled) description else if (id == "mistakes") "Empty — you haven't missed anything yet." else "Finish lessons to unlock this practice set.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (enabled) Text("${questions.size} →", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LearnTestsScreen(store: Store, me: String, state: LearnState, onStart: (LearningAssessment) -> Unit) {
    var building by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🧭 Skip and placement tests", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Prove what you already know. These tests do not cost hearts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("🎯 Placement test", fontWeight = FontWeight.Bold)
                Text(if (state.placed) "You have completed the placement test. You can take it again." else "Eight questions that get harder. A strong score unlocks earlier units.", fontSize = 13.sp)
                Button(enabled = !building, onClick = {
                    if (store.aiKey.isBlank()) { error = "Placement test needs an AI key. Add one in Settings, or start with Unit 1."; return@Button }
                    building = true; error = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val topicList = state.units.joinToString(" | ") { unit ->
                                "${unit.title} (${state.chapters.filter { it.unitId == unit.id }.joinToString { it.title }})"
                            }
                            val prompt = "Reply with strict JSON only: {\"ex\":[{\"type\":\"input\",\"prompt\":\"Solve for x\",\"q\":\"x + 5 = 12\",\"answer\":\"7\"}]}. " +
                                "Write exactly eight progressively harder maths placement questions covering these units in order: $topicList. " +
                                "Use input, mc (four choices), or tf. Use correct answers and answer alternatives joined with |."
                            runCatching {
                                val response = Ai.complete(store.aiKey, "You are a careful maths placement-test writer. Output valid JSON only.", listOf(ChatMsg(true, prompt)))
                                val jsonText = Regex("\\{[\\s\\S]*}").find(response)?.value ?: error("The AI did not return JSON.")
                                val ex = anyList(JSONObject(jsonText).opt("ex")).mapNotNull { (it as? JSONObject)?.let(Exercise::from) }
                                require(ex.size >= 3) { "The placement test was incomplete." }
                                LearningAssessment("Placement test", ex.take(8), "placement")
                            }
                        }
                        building = false
                        result.onSuccess(onStart).onFailure { error = it.message ?: "Could not make a placement test right now." }
                    }
                }) { Text(if (building) "Writing your test…" else "Start placement test") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Unit final tests", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        state.units.forEach { unit ->
            if (unit.test.isNotEmpty()) TestCard("🏆 ${unit.title}", "${unit.test.size} questions · ${(if (unit.id in state.skippedUnits) "Passed" else "80% to pass")}") {
                onStart(LearningAssessment("${unit.title} · Unit test", unit.test, "unit-test", unit.id))
            }
        }
        Text("Chapter skip tests", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        state.chapters.forEach { chapter ->
            if (chapter.test.isNotEmpty()) TestCard("⚡ ${chapter.title}", "${chapter.test.size} questions · ${(if (chapter.id in state.skippedChapters) "Passed" else "80% to pass")}") {
                onStart(LearningAssessment("${chapter.title} · Chapter test", chapter.test, "chapter-test", chapter.id))
            }
        }
        if (state.units.none { it.test.isNotEmpty() } && state.chapters.none { it.test.isNotEmpty() }) {
            Text("No owner-authored skip tests yet. The owner can add them under Edit course.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TestCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private data class CourseDraft(
    val kind: String, val id: String? = null, val name: String = "", val parentId: String = "", val desc: String = "",
    val howto: String = "", val hints: String = "", val order: String = "1", val icon: String = "📚", val color: String = "#58CC02",
    val exercises: List<Exercise> = emptyList()
)

@Composable
fun LearnCourseEditor(me: String, state: LearnState, onSaved: () -> Unit) {
    var draft by remember { mutableStateOf<CourseDraft?>(null) }
    var exerciseIndex by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun beginUnit(unit: CourseUnit? = null) {
        draft = CourseDraft("unit", unit?.id, unit?.title.orEmpty(), desc = unit?.description.orEmpty(), order = (unit?.order ?: state.units.size + 1).toString(), icon = unit?.icon ?: "📚", color = unit?.color ?: "#58CC02", exercises = unit?.test.orEmpty())
    }
    fun beginChapter(chapter: Chapter? = null, unitId: String = state.units.firstOrNull()?.id.orEmpty()) {
        draft = CourseDraft("chapter", chapter?.id, chapter?.title.orEmpty(), chapter?.unitId ?: unitId, order = (chapter?.order ?: (state.chapters.count { it.unitId == unitId } + 1)).toString(), icon = chapter?.icon ?: "📘", color = chapter?.color.orEmpty(), exercises = chapter?.test.orEmpty())
    }
    fun beginLesson(lesson: Lesson? = null, chapterId: String = state.chapters.firstOrNull()?.id.orEmpty()) {
        draft = CourseDraft("lesson", lesson?.id, lesson?.title.orEmpty(), lesson?.chapterId ?: chapterId, lesson?.description.orEmpty(), lesson?.howto.orEmpty(), lesson?.hints.orEmpty().joinToString("\n"), (lesson?.order ?: (state.lessons.count { it.chapterId == chapterId } + 1)).toString(), lesson?.icon ?: "⭐", lesson?.color.orEmpty(), lesson?.ex.orEmpty())
    }
    fun delete(kind: String, id: String) {
        scope.launch {
            loading = true
            val ok = withContext(Dispatchers.IO) {
                when (kind) {
                    "units" -> {
                        val childChapters = state.chapters.filter { it.unitId == id }
                        val childLessons = state.lessons.filter { lesson -> lesson.unitId == id || childChapters.any { it.id == lesson.chapterId } }
                        childLessons.forEach { Learn.deleteCourseItem("lessons", it.id) }
                        childChapters.forEach { Learn.deleteCourseItem("chapters", it.id) }
                        Learn.deleteCourseItem("units", id)
                    }
                    "chapters" -> {
                        state.lessons.filter { it.chapterId == id }.forEach { Learn.deleteCourseItem("lessons", it.id) }
                        Learn.deleteCourseItem("chapters", id)
                    }
                    else -> Learn.deleteCourseItem("lessons", id)
                }
            }
            loading = false
            if (ok) { error = null; onSaved() } else error = "Couldn't delete that course item."
        }
    }
    fun save(d: CourseDraft) {
        if (d.name.isBlank()) { error = "Add a name before saving."; return }
        if (d.kind == "lesson" && d.exercises.isEmpty()) { error = "Add at least one exercise to the lesson."; return }
        if ((d.kind == "chapter" && d.parentId.isBlank()) || (d.kind == "lesson" && d.parentId.isBlank())) { error = "Choose the parent unit or chapter."; return }
        val collection = when (d.kind) { "unit" -> "units"; "chapter" -> "chapters"; else -> "lessons" }
        val json = when (d.kind) {
            "unit" -> JSONObject().put("name", d.name.trim()).put("desc", d.desc.trim()).put("order", d.order.toIntOrNull() ?: 1)
                .put("icon", d.icon).put("color", d.color).put("test", JSONArray().apply { d.exercises.forEach { put(it.toJson()) } })
            "chapter" -> JSONObject().put("unitId", d.parentId).put("name", d.name.trim()).put("order", d.order.toIntOrNull() ?: 1)
                .put("icon", d.icon).put("color", d.color).put("test", JSONArray().apply { d.exercises.forEach { put(it.toJson()) } })
            else -> {
                val chapter = state.chapters.firstOrNull { it.id == d.parentId }
                JSONObject().put("chapterId", d.parentId).put("unitId", chapter?.unitId.orEmpty()).put("title", d.name.trim())
                    .put("desc", d.desc.trim()).put("order", d.order.toIntOrNull() ?: 1).put("icon", d.icon).put("color", d.color)
                    .put("howto", d.howto).put("hints", JSONArray().apply { d.hints.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { put(it) } })
                    .put("ex", JSONArray().apply { d.exercises.forEach { put(it.toJson()) } })
            }
        }
        scope.launch {
            loading = true
            val key = withContext(Dispatchers.IO) { Learn.saveCourseItem(collection, d.id, json) }
            loading = false
            if (key != null) { draft = null; error = null; onSaved() } else error = "Couldn't save that item."
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("✏️ Build the course", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Units contain chapters; chapters contain lessons. Add skip tests to let students prove what they know.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { beginUnit() }) { Text("＋ Unit") }
            OutlinedButton(enabled = state.units.isNotEmpty(), onClick = { beginChapter() }) { Text("＋ Chapter") }
            OutlinedButton(enabled = state.chapters.isNotEmpty(), onClick = { beginLesson() }) { Text("＋ Lesson") }
        }
        OutlinedButton(onClick = {
            if (state.units.isEmpty()) {
                scope.launch {
                    loading = true
                    val ok = withContext(Dispatchers.IO) {
                        listOf("Number foundations", "Fractions", "Algebra basics").mapIndexed { i, title ->
                            Learn.saveCourseItem(
                                "units", "starter-${i + 1}",
                                JSONObject().put("name", title).put("order", i + 1).put("desc", "Starter course unit")
                                    .put("icon", "📘").put("color", "#58CC02").put("test", JSONArray())
                            ) != null
                        }.all { it }
                    }
                    loading = false
                    if (ok) { error = null; onSaved() } else error = "The starter units could not all be saved. Refresh and check the course."
                }
            } else error = "Starter units are only added to an empty course."
        }, enabled = !loading) { Text("🌱 Starter course") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.units, key = { "unit-${it.id}" }) { unit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(unit.icon, fontSize = 22.sp); Spacer(Modifier.width(8.dp))
                            Text(unit.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { beginUnit(unit) }) { Text("Edit") }
                            TextButton(onClick = { delete("units", unit.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                        Text("${state.chapters.count { it.unitId == unit.id }} chapters · final test: ${unit.test.size} questions", fontSize = 12.sp)
                        state.chapters.filter { it.unitId == unit.id }.forEach { chapter ->
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("↳ ${chapter.icon} ${chapter.title}", Modifier.weight(1f), fontSize = 13.sp)
                                TextButton(onClick = { beginChapter(chapter) }) { Text("Edit") }
                                TextButton(onClick = { beginLesson(chapterId = chapter.id) }) { Text("＋ Lesson") }
                                TextButton(onClick = { delete("chapters", chapter.id) }) { Text("×") }
                            }
                            state.lessonsOf(chapter.id).forEach { lesson ->
                                Row(Modifier.fillMaxWidth().padding(start = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("• ${lesson.title} · ${lesson.ex.size} exercises", Modifier.weight(1f), fontSize = 12.sp)
                                    TextButton(onClick = { beginLesson(lesson) }) { Text("Edit") }
                                    TextButton(onClick = { delete("lessons", lesson.id) }) { Text("×") }
                                }
                            }
                        }
                        state.looseLessons(unit.id).forEach { lesson ->
                            Row(Modifier.fillMaxWidth().padding(start = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("• ${lesson.title}", Modifier.weight(1f), fontSize = 12.sp)
                                TextButton(onClick = { beginLesson(lesson) }) { Text("Edit") }
                                TextButton(onClick = { delete("lessons", lesson.id) }) { Text("×") }
                            }
                        }
                    }
                }
            }
        }
    }

    draft?.let { current -> CourseItemDialog(current, state, loading, onDismiss = { draft = null }, onSave = ::save,
        onUpdate = { draft = it }, onEditExercise = { exerciseIndex = it }) }
    if (draft != null && exerciseIndex >= 0) {
        val current = draft!!
        val existing = current.exercises.getOrNull(exerciseIndex)
        ExerciseEditorDialog(existing, onDismiss = { exerciseIndex = -1 }) { exercise ->
            val list = current.exercises.toMutableList()
            if (exerciseIndex >= list.size) list.add(exercise) else list[exerciseIndex] = exercise
            draft = current.copy(exercises = list)
            exerciseIndex = -1
        }
    }
}

@Composable
private fun CourseItemDialog(
    draft: CourseDraft,
    state: LearnState,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (CourseDraft) -> Unit,
    onUpdate: (CourseDraft) -> Unit,
    onEditExercise: (Int) -> Unit
) {
    val icons = listOf("📚", "📘", "🧮", "⭐", "🧠", "📐", "🎯", "🧩", "💡", "🏆")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (draft.id == null) "New" else "Edit"} ${draft.kind}") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.name, { onUpdate(draft.copy(name = it)) }, label = { Text(if (draft.kind == "lesson") "Lesson title" else "Name") }, singleLine = true)
                OutlinedTextField(draft.order, { onUpdate(draft.copy(order = it.filter(Char::isDigit))) }, label = { Text("Order") }, singleLine = true)
                when (draft.kind) {
                    "unit" -> OutlinedTextField(draft.desc, { onUpdate(draft.copy(desc = it)) }, label = { Text("Short description") })
                    "chapter" -> {
                        Text("Unit", fontWeight = FontWeight.Bold)
                        state.units.forEach { unit -> FilterChip(draft.parentId == unit.id, { onUpdate(draft.copy(parentId = unit.id)) }, label = { Text(unit.title) }) }
                    }
                    "lesson" -> {
                        Text("Chapter", fontWeight = FontWeight.Bold)
                        state.chapters.forEach { chapter -> FilterChip(draft.parentId == chapter.id, { onUpdate(draft.copy(parentId = chapter.id)) }, label = { Text(chapter.title) }) }
                        OutlinedTextField(draft.desc, { onUpdate(draft.copy(desc = it)) }, label = { Text("Short description") })
                        OutlinedTextField(draft.howto, { onUpdate(draft.copy(howto = it)) }, label = { Text("Teach it step by step") }, minLines = 3)
                        OutlinedTextField(draft.hints, { onUpdate(draft.copy(hints = it)) }, label = { Text("Hints, one per line") }, minLines = 2)
                    }
                }
                Text("Icon", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    icons.forEach { icon -> FilterChip(draft.icon == icon, { onUpdate(draft.copy(icon = icon)) }, label = { Text(icon) }) }
                }
                if (draft.kind != "lesson") {
                    Text(if (draft.kind == "unit") "Unit final test" else "Chapter skip test", fontWeight = FontWeight.Bold)
                    Text("Students need ${LRN_PASS}% to pass. No hearts are lost.", fontSize = 12.sp)
                } else Text("Exercises", fontWeight = FontWeight.Bold)
                draft.exercises.forEachIndexed { index, ex ->
                    Card(Modifier.fillMaxWidth().clickable { onEditExercise(index) }) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${ex.q}", Modifier.weight(1f), maxLines = 2)
                            TextButton(onClick = { onEditExercise(index) }) { Text("Edit") }
                            TextButton(onClick = { onUpdate(draft.copy(exercises = draft.exercises.filterIndexed { i, _ -> i != index })) }) { Text("×") }
                        }
                    }
                }
                OutlinedButton(onClick = { onEditExercise(draft.exercises.size) }) { Text("＋ Add exercise") }
            }
        },
        confirmButton = { Button(enabled = !busy, onClick = { onSave(draft) }) { Text(if (busy) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExerciseEditorDialog(existing: Exercise?, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var type by remember { mutableStateOf(existing?.type ?: "input") }
    var prompt by remember { mutableStateOf(existing?.prompt ?: "Solve it") }
    var question by remember { mutableStateOf(existing?.q ?: "") }
    var answer by remember { mutableStateOf(existing?.answer ?: "") }
    var choices by remember { mutableStateOf(existing?.choices.orEmpty().joinToString("\n")) }
    var hint by remember { mutableStateOf(existing?.hint ?: "") }
    var steps by remember { mutableStateOf(existing?.steps.orEmpty().joinToString("\n")) }
    var pairs by remember { mutableStateOf(existing?.pairs.orEmpty().joinToString("\n") { "${it.first} = ${it.second}" }) }
    var menu by remember { mutableStateOf(false) }
    val types = listOf("input", "mc", "tf", "order", "match", "blank", "multi")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add exercise" else "Edit exercise") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box {
                    OutlinedButton(onClick = { menu = true }) { Text("Type: $type ▾") }
                    DropdownMenu(menu, { menu = false }) { types.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { type = t; menu = false }) } }
                }
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Instruction") }, singleLine = true)
                OutlinedTextField(question, { question = it }, label = { Text(if (type == "blank") "Question (use ___ for each blank)" else "Question") }, minLines = 2)
                OutlinedTextField(answer, { answer = it }, label = { Text("Correct answer (use | for alternatives)") }, singleLine = true)
                if (type in setOf("mc", "multi")) OutlinedTextField(choices, { choices = it }, label = { Text("Choices, one per line") }, minLines = 3)
                if (type == "order") OutlinedTextField(steps, { steps = it }, label = { Text("Correct steps, in order") }, minLines = 3)
                if (type == "match") OutlinedTextField(pairs, { pairs = it }, label = { Text("Pairs, one 'left = right' per line") }, minLines = 3)
                OutlinedTextField(hint, { hint = it }, label = { Text("Hint") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = question.isNotBlank() && (answer.isNotBlank() || type == "order"), onClick = {
                val correctSteps = steps.lines().map(String::trim).filter(String::isNotEmpty)
                val correctPairs = pairs.lines().mapNotNull { row -> row.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }
                onSave(Exercise(type, prompt, question, choices.lines().map(String::trim).filter(String::isNotEmpty), answer, hint, correctSteps, correctPairs))
            }) { Text("Save exercise") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
