package club.matix.mathclub.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

private val PageIcons = listOf("📄", "🚀", "🧮", "📊", "🎲", "🧩", "📚", "🎨", "🔬", "⚙️", "🏆", "🔥", "⭐", "🌐", "🔎", "📝")
private const val HTML_PAGE_MAX_BYTES = 2 * 1024 * 1024

private data class AppPage(val id: String, val name: String, val icon: String, val html: String, val searchOnly: Boolean, val at: Long)

private fun loadAppPages(): List<AppPage> {
    val root = Firebase.get("/apppages") as? JSONObject ?: return emptyList()
    return root.keys().asSequence().mapNotNull { id ->
        val row = root.optJSONObject(id) ?: return@mapNotNull null
        AppPage(id, row.optString("name", "Untitled"), row.optString("icon", "📄"), row.optString("html"), row.optBoolean("searchOnly"), row.optLong("at"))
    }.sortedBy { it.at }.toList()
}

private fun readHtml(context: Context, uri: android.net.Uri): String {
    val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open file")
    input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            if (out.size() + n > HTML_PAGE_MAX_BYTES) error("HTML files must be under 2 MB")
            out.write(buffer, 0, n)
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }
}

@Composable
fun CustomPagesScreen(user: String, isOwner: Boolean, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<AppPage>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var html by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(PageIcons.first()) }
    var searchOnly by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) { pages = withContext(Dispatchers.IO) { loadAppPages() } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = runCatching { withContext(Dispatchers.IO) { readHtml(context, uri) } }
            result.onSuccess { html = it; pickedFile = uri.lastPathSegment ?: "index.html"; error = null }
                .onFailure { error = it.message ?: "Couldn't read that HTML file." }
            busy = false
        }
    }

    val shown = pages.orEmpty().filter { page ->
        (!page.searchOnly || isOwner || (query.isNotBlank() && page.name.contains(query, ignoreCase = true))) &&
            (query.isBlank() || page.name.contains(query, ignoreCase = true))
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("📄 App Pages", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Extra pages shared by the club owner.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { refresh++ }) { Text("Refresh") }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
            label = { Text("Search pages") })
        if (isOwner) {
            OutlinedButton(onClick = { create = !create }) { Text(if (create) "Cancel" else "＋ New page") }
            if (create) Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create owner page", fontWeight = FontWeight.Bold)
                    OutlinedTextField(name, { name = it.take(48) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Page name") })
                    Text("Icon", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PageIcons.forEach { symbol -> FilterChip(icon == symbol, { icon = symbol }, label = { Text(symbol) }) }
                    }
                    OutlinedButton(enabled = !busy, onClick = { picker.launch("text/html") }) {
                        Text(if (pickedFile.isBlank()) "Choose HTML file" else "HTML: $pickedFile")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(searchOnly, { searchOnly = it })
                        Text("Search-only (hide from the page list unless searched)", fontSize = 13.sp)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(enabled = !busy && name.isNotBlank() && html.isNotBlank(), onClick = {
                        busy = true
                        val pageId = UUID.randomUUID().toString().replace("-", "")
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                Firebase.put("/apppages/${Firebase.enc(pageId)}", JSONObject()
                                    .put("name", name.trim()).put("icon", icon).put("html", html).put("by", user)
                                    .put("at", System.currentTimeMillis()).put("searchOnly", searchOnly))
                            }
                            busy = false
                            if (ok) {
                                val newName = name.trim()
                                pages = withContext(Dispatchers.IO) { loadAppPages() }
                                create = false; name = ""; html = ""; pickedFile = ""; searchOnly = false; error = null
                                pages?.firstOrNull { it.name == newName }?.let { onOpen(it.id) }
                            } else error = "Page wasn't saved. Check the connection and try again."
                        }
                    }) { Text(if (busy) "Saving…" else "Create page") }
                }
            }
        }
        when {
            pages == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isOwner) "No pages yet — add one above." else "No pages match that search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { page ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(page.id) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(page.icon, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(page.name, fontWeight = FontWeight.Bold)
                                if (page.searchOnly) Text("Search-only", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isOwner) TextButton(onClick = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { Firebase.delete("/apppages/${Firebase.enc(page.id)}") }
                                    if (ok) refresh++ else error = "Couldn't delete the page."
                                }
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPageViewer(pageId: String, onBack: () -> Unit) {
    var page by remember(pageId) { mutableStateOf<AppPage?>(null) }
    LaunchedEffect(pageId) {
        page = withContext(Dispatchers.IO) { loadAppPages().firstOrNull { it.id == pageId } }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Pages") }
            Text(page?.let { "${it.icon} ${it.name}" } ?: "Loading page…", fontWeight = FontWeight.Bold)
        }
        when {
            page == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            page!!.html.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This page has no content yet.") }
            else -> HtmlPreview(page!!.html, Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(12.dp)))
        }
    }
}
