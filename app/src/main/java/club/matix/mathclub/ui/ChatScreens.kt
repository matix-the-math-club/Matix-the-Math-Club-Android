package club.matix.mathclub.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.matix.mathclub.data.ChatPerson
import club.matix.mathclub.data.ConversationPreview
import club.matix.mathclub.data.DirectChatRepository
import club.matix.mathclub.data.DirectMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val ChatEmoji = listOf("🙂", "😂", "😍", "👍", "🎉", "🔥", "😎", "🤔", "🙌", "❤️", "👏", "✅")
private val MathStickers = listOf("π", "∞", "√", "∑", "∫", "θ", "Δ", "λ", "φ", "≠", "≈", "≤", "≥", "±", "÷", "×", "∈", "🧮", "📐", "📊")

@Composable
fun DiscussionsScreen(me: String, onOpenChat: (String) -> Unit) {
    var rows by remember { mutableStateOf<List<ConversationPreview>?>(null) }
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(me, refresh) {
        while (isActive) {
            try {
                rows = DirectChatRepository.discussions(me)
                loadError = null
            } catch (_: Exception) {
                loadError = "Couldn't load discussions. Check your connection and try again."
                rows = emptyList()
            }
            delay(12_000)
        }
    }

    val all = rows.orEmpty()
    val shown = all.filter {
        query.isBlank() || it.person.displayName.contains(query, ignoreCase = true) ||
            it.person.username.contains(query, ignoreCase = true)
    }
    val conversationCount = all.count { it.lastMessage != null }
    val unreadCount = all.sumOf { it.unread }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("💬 Discussions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Your one-to-one chats, newest first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { refresh++ }) { Text("Refresh") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            singleLine = true,
            label = { Text("Search people") },
            leadingIcon = { Text("🔎") }
        )
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip("$conversationCount chats")
            SummaryChip("$unreadCount unread", hot = unreadCount > 0)
            SummaryChip("${all.size} people")
        }
        when {
            rows == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { refresh++ }) { Text("Try again") }
                }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (query.isBlank()) "👋" else "🔎", fontSize = 34.sp)
                    Text(if (query.isBlank()) "No one to chat with yet." else "Nobody matches that search.")
                    Text("Members appear here when their profiles are available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (shown.any { it.lastMessage != null }) {
                    item { SectionLabel("YOUR CHATS") }
                    items(shown.filter { it.lastMessage != null }, key = { it.person.username }) { row ->
                        DiscussionRow(row, me) { onOpenChat(row.person.username) }
                    }
                }
                if (shown.any { it.lastMessage == null }) {
                    item { SectionLabel("START A NEW CHAT") }
                    items(shown.filter { it.lastMessage == null }, key = { it.person.username }) { row ->
                        DiscussionRow(row, me) { onOpenChat(row.person.username) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, hot: Boolean = false) {
    Surface(color = if (hot) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp,
            color = if (hot) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(start = 4.dp, top = 5.dp, bottom = 2.dp), fontSize = 11.sp,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DiscussionRow(row: ConversationPreview, me: String, onClick: () -> Unit) {
    val person = row.person
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(person, 46)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    RolePill(person.role)
                }
                Text(
                    previewText(row.lastMessage, me),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                row.lastMessage?.let { Text(chatTime(it.at), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (row.unread > 0) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                        Text(row.unread.toString(), Modifier.defaultMinSize(minWidth = 22.dp).padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MathChatScreen(me: String, initialPeer: String? = null) {
    var people by remember { mutableStateOf<List<ChatPerson>>(emptyList()) }
    var selected by remember { mutableStateOf<ChatPerson?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var equation by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var otherReadAt by remember { mutableLongStateOf(0L) }
    var showEmoji by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showEquation by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DirectMessage?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(me) {
        people = withContext(Dispatchers.IO) { DirectChatRepository.people(me) }
    }
    LaunchedEffect(me, initialPeer, people) {
        if (!initialPeer.isNullOrBlank()) {
            val normalized = initialPeer.lowercase().replace(Regex("\\s+"), "").trim()
            selected = people.firstOrNull { it.username == normalized }
                ?: ChatPerson(normalized, normalized.replaceFirstChar { it.uppercase() }, "user", "", "", false)
        }
    }
    LaunchedEffect(me) {
        while (isActive) {
            withContext(Dispatchers.IO) { DirectChatRepository.refreshPresence(me) }
            delay(30_000)
        }
    }
    LaunchedEffect(me, selected?.username) {
        val peer = selected ?: return@LaunchedEffect
        var initialReadDone = false
        var newestIncomingSeen = 0L
        while (isActive) {
            val state = withContext(Dispatchers.IO) { DirectChatRepository.snapshot(me, peer.username) }
            messages = state.messages
            otherReadAt = state.otherReadAt
            isOnline = state.otherOnline
            isTyping = state.otherTypingAt > 0 && System.currentTimeMillis() - state.otherTypingAt < 7_000
            val newestIncoming = state.messages.filter { it.from == peer.username }.maxOfOrNull { it.at } ?: 0L
            if (!initialReadDone || newestIncoming > newestIncomingSeen) {
                withContext(Dispatchers.IO) { DirectChatRepository.markRead(me, peer.username) }
                initialReadDone = true
                newestIncomingSeen = newestIncoming
            }
            delay(2_500)
        }
    }
    LaunchedEffect(input, selected?.username) {
        val peer = selected ?: return@LaunchedEffect
        if (input.isNotBlank()) {
            delay(450)
            withContext(Dispatchers.IO) { DirectChatRepository.setTyping(me, peer.username, true) }
        } else {
            withContext(Dispatchers.IO) { DirectChatRepository.setTyping(me, peer.username, false) }
        }
    }
    LaunchedEffect(messages.size, selected?.username) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val filteredPeople = people.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
    }

    fun send(type: String, text: String) {
        val peer = selected ?: return
        if (sending || text.isBlank()) return
        sending = true
        sendError = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { DirectChatRepository.send(me, peer, type, text) }
            sending = false
            if (ok) {
                if (type == "math") equation = "" else input = ""
                showEquation = false
            } else {
                sendError = "Message didn't send. Check your connection and try again."
            }
        }
    }

    if (selected == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("🧮 Math Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("One-to-one messages with members.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 10.dp),
                singleLine = true, label = { Text("Search the club") }, leadingIcon = { Text("🔎") })
            if (people.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(10.dp))
                        Text("Loading the club…")
                    }
                }
            } else if (filteredPeople.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nobody matches that search.") }
            } else {
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(filteredPeople, key = { it.username }) { person ->
                        Card(Modifier.fillMaxWidth().clickable { selected = person; messages = emptyList() }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                PersonAvatar(person, 42)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(person.displayName, fontWeight = FontWeight.SemiBold)
                                    Text("@${person.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                RolePill(person.role)
                                Spacer(Modifier.width(8.dp))
                                Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selected = null; messages = emptyList(); sendError = null }) { Text("← People") }
                PersonAvatar(selected!!, 38)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(selected!!.displayName, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        when { isTyping -> "typing…"; isOnline -> "online now"; else -> "@${selected!!.username}" },
                        fontSize = 12.sp,
                        color = if (isTyping || isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RolePill(selected!!.role)
            }
            HorizontalDivider()
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                        Text("Conversation with ${selected!!.displayName}.",
                        Modifier.fillMaxWidth().padding(bottom = 5.dp), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (messages.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                            Text("Say hello — no messages yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message, me, otherReadAt) { pendingDelete = message }
                }
                if (isTyping) item { Text("${selected!!.displayName} is typing…", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
            }
            if (showEmoji) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChatEmoji.forEach { emoji -> TextButton(onClick = { input = (input + emoji).take(2_000) }) { Text(emoji, fontSize = 20.sp) } }
                }
            }
            if (showStickers) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MathStickers.forEach { sticker -> TextButton(onClick = { send("sticker", sticker); showStickers = false }) { Text(sticker, fontSize = 20.sp) } }
                }
            }
            if (showEquation) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(equation, { equation = it.take(500) }, Modifier.weight(1f),
                        label = { Text("Write an equation") }, placeholder = { Text("x² + y² = r²") }, singleLine = true)
                    Spacer(Modifier.width(6.dp))
                    Button(enabled = equation.isNotBlank() && !sending, onClick = { send("math", equation) }) { Text("Send") }
                }
            }
            sendError?.let { Text(it, Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showEmoji = !showEmoji; showStickers = false }) { Text("🙂") }
                TextButton(onClick = { showStickers = !showStickers; showEmoji = false }) { Text("π") }
                TextButton(onClick = { showEquation = !showEquation }) { Text("📐") }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(2_000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message ${selected!!.displayName}…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send("text", input) })
                )
                Spacer(Modifier.width(6.dp))
                Button(enabled = input.isNotBlank() && !sending, onClick = { send("text", input) }) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Send")
                }
            }
        }
    }

    pendingDelete?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete message?") },
            text = { Text("This removes the message from the conversation for both people.") },
            confirmButton = {
                TextButton(onClick = {
                    val peer = selected
                    pendingDelete = null
                    if (peer != null) scope.launch {
                        val deleted = withContext(Dispatchers.IO) { DirectChatRepository.deleteOwnMessage(me, peer.username, message) }
                        if (!deleted) sendError = "Couldn't delete this message. Check your connection."
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ChatBubble(message: DirectMessage, me: String, otherReadAt: Long, onDelete: () -> Unit) {
    val outgoing = message.from == me.lowercase()
    val background = if (outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (outgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start) {
        Column(Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(18.dp)).background(background).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = when (message.type) { "math" -> "📐  ${message.text}"; else -> message.text },
                color = foreground,
                fontSize = if (message.type == "sticker") 27.sp else 15.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(chatTime(message.at) + if (outgoing) if (message.at <= otherReadAt) "  ✓✓" else "  ✓" else "",
                    color = foreground.copy(alpha = 0.75f), fontSize = 10.sp)
                if (outgoing) {
                    Spacer(Modifier.width(7.dp))
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text("Delete", fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun PersonAvatar(person: ChatPerson, size: Int) {
    val bg = try {
        Color(AndroidColor.parseColor(person.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (person.photo.isNotBlank()) {
            DataUriImage(person.photo, Modifier.fillMaxSize(), "${person.displayName}'s profile photo")
        } else {
            Text(person.emoji.ifBlank { person.displayName.firstOrNull()?.uppercase() ?: "?" },
                color = Color.White, fontSize = if (person.emoji.isBlank()) (size / 2.4).sp else (size / 2.0).sp,
                fontWeight = FontWeight.Bold)
        }
        if (person.hasBadge) Text("★", Modifier.align(Alignment.BottomEnd), fontSize = 11.sp, color = Color(0xFFFFD54F))
    }
}

@Composable
private fun RolePill(roleRaw: String) {
    val role = roleRaw.lowercase().ifBlank { "user" }
    val icon = when (role) { "owner" -> "👑"; "manager" -> "🛡️"; "admin" -> "⭐"; "member" -> "🎓"; else -> "🙂" }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Text("$icon ${role.replaceFirstChar { it.uppercase() }}", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
    }
}

private fun previewText(message: DirectMessage?, me: String): String = when {
    message == null -> "Say hello — no messages yet"
    message.type == "sticker" -> "${message.text}  (sticker)"
    message.type == "math" -> "📐 an equation"
    else -> (if (message.from == me.lowercase()) "You: " else "") + message.text
}

private fun chatTime(ms: Long): String = if (ms <= 0L) "" else DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
