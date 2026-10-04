package club.matix.mathclub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private data class PrivateEntry(val id: String, val text: String, val at: Long, val done: Boolean = false)

private object PrivateWorkspace {
    private fun base(user: String) = "/private/${Firebase.enc(user)}"

    fun list(user: String, section: String): List<PrivateEntry> {
        val rows = Firebase.get("${base(user)}/$section") as? JSONObject ?: return emptyList()
        return rows.keys().asSequence().mapNotNull { key ->
            val item = rows.optJSONObject(key) ?: return@mapNotNull null
            PrivateEntry(key, item.optString("text"), item.optLong("at"), item.optBoolean("done"))
        }.sortedWith(if (section == "todos") compareBy<PrivateEntry> { it.at } else compareByDescending<PrivateEntry> { it.at })
            .toList()
    }

    fun add(user: String, section: String, text: String): Boolean {
        val id = UUID.randomUUID().toString().replace("-", "")
        val item = JSONObject().put("text", text.trim()).put("at", System.currentTimeMillis())
        if (section == "todos") item.put("done", false)
        return Firebase.put("${base(user)}/$section/${Firebase.enc(id)}", item)
    }

    fun toggle(user: String, id: String, done: Boolean) =
        Firebase.put("${base(user)}/todos/${Firebase.enc(id)}/done", done)

    fun delete(user: String, section: String, id: String) =
        Firebase.delete("${base(user)}/$section/${Firebase.enc(id)}")
}

@Composable
fun PrivateScreen(user: String) {
    var todos by remember { mutableStateOf<List<PrivateEntry>?>(null) }
    var notes by remember { mutableStateOf<List<PrivateEntry>?>(null) }
    var todoInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user, refresh) {
        val result = withContext(Dispatchers.IO) {
            runCatching { PrivateWorkspace.list(user, "todos") to PrivateWorkspace.list(user, "notes") }
        }
        result.onSuccess { (todoRows, noteRows) ->
            todos = todoRows
            notes = noteRows
            error = null
        }.onFailure { error = "Couldn't load your private space. Check your connection." }
    }

    fun mutate(block: () -> Boolean, onSuccess: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { block() }
            busy = false
            if (ok) { onSuccess(); refresh++ }
            else error = "That change didn't save. Check your connection and try again."
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔒 Private", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Your to-do list and notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        if (todos == null || notes == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✅ To-do", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(todoInput, { todoInput = it.take(200) }, Modifier.weight(1f), singleLine = true,
                                    label = { Text("Add a to-do") }, enabled = !busy)
                                Spacer(Modifier.width(8.dp))
                                Button(enabled = todoInput.isNotBlank() && !busy, onClick = {
                                    val value = todoInput.trim()
                                    mutate({ PrivateWorkspace.add(user, "todos", value) }) { todoInput = "" }
                                }) { Text("Add") }
                            }
                            if (todos!!.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(todos!!, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(item.done, onCheckedChange = { checked -> mutate({ PrivateWorkspace.toggle(user, item.id, checked) }) })
                            Text(item.text, Modifier.weight(1f), color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            TextButton(enabled = !busy, onClick = { mutate({ PrivateWorkspace.delete(user, "todos", item.id) }) }) { Text("×") }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📝 Notes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(noteInput, { noteInput = it.take(500) }, Modifier.weight(1f), singleLine = true,
                                    label = { Text("Add a note") }, enabled = !busy)
                                Spacer(Modifier.width(8.dp))
                                Button(enabled = noteInput.isNotBlank() && !busy, onClick = {
                                    val value = noteInput.trim()
                                    mutate({ PrivateWorkspace.add(user, "notes", value) }) { noteInput = "" }
                                }) { Text("Add") }
                            }
                            if (notes!!.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(notes!!, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.text, Modifier.weight(1f))
                            TextButton(enabled = !busy, onClick = { mutate({ PrivateWorkspace.delete(user, "notes", item.id) }) }) { Text("×") }
                        }
                    }
                }
            }
        }
    }
}
