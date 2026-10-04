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

@Composable
fun PointsScreen(me: String, isOwner: Boolean) {
    var data by remember { mutableStateOf<PointsData?>(null) }
    var failed by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf("ledger") }
    var open by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tick) {
        val r = withContext(Dispatchers.IO) { runCatching { Points.load() }.getOrNull() }
        if (r == null) failed = true else data = r
    }
    val d = data
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (failed) Text("Could not load the points — check your connection.") else CircularProgressIndicator() }
        return
    }
    val canDirect = isOwner || d.role(me) == "manager"
    val canAsk = d.role(me) == "admin"
    fun run(block: () -> String) = scope.launch { msg = withContext(Dispatchers.IO) { block() }; tick++ }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⭐ Points", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Earn points for doing maths, spend them on club perks.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.score(me)}", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("Your points", fontSize = 11.sp) }
        }
        val segs = buildList {
            add("ledger" to "🏆 Ledger"); add("earn" to "➕ Earn"); add("rewards" to "🎁 Rewards")
            if (isOwner) add("reqs" to "📥 Requests (${d.pending().size})")
        }
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            segs.forEach { (id, l) -> FilterChip(view == id, { view = id; open = null }, label = { Text(l, fontSize = 12.sp) }) }
        }
        if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

        when {
            view == "earn" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EarnList.forEach { e ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(e.icon, fontSize = 24.sp); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Text(e.title, fontWeight = FontWeight.Bold); Text(e.desc, fontSize = 12.sp) }
                            Text(e.pts, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Points are given out by the owner and the managers — do the work, then ask them.", fontSize = 12.sp)
            }
            view == "rewards" -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val mine = d.score(me)
                RewardList.forEach { r ->
                    val can = mine >= r.cost
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.icon, fontSize = 24.sp); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, fontWeight = FontWeight.Bold)
                                Text(if (can) "You can afford this one." else "You need ${r.cost - mine} more.", fontSize = 12.sp)
                            }
                            Text("${r.cost}  ", fontWeight = FontWeight.Bold)
                            Button(enabled = can, onClick = { run { if (Points.askReward(me, r)) "Asked for “${r.title}”. The owner will see it in Requests." else "Could not send that request." } }) { Text("Ask") }
                        }
                    }
                }
            }
            view == "reqs" && isOwner -> {
                val p = d.pending()
                if (p.isEmpty()) Text("Nothing waiting for you — nice.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(p) { (id, r) ->
                        val what = if (r.optString("reward").isNotEmpty()) "the reward “${r.optString("reward")}”"
                        else if (r.opt("amount") == "reset") "a reset to 0" else "${r.optInt("amount")} points"
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${d.nameOf(r.optString("requestedBy"))} wants $what", fontWeight = FontWeight.Bold)
                                Text("for ${d.nameOf(r.optString("targetUser"))} · @${r.optString("targetUser")}", fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { run { if (Points.decide(d, me, id, r, true)) "Approved" else "Could not save that decision." } }) { Text("Approve") }
                                    OutlinedButton(onClick = { run { if (Points.decide(d, me, id, r, false)) "Denied" else "Could not save that decision." } }) { Text("Deny") }
                                }
                            }
                        }
                    }
                }
            }
            open != null -> PointsUser(d, open!!, me, isOwner, canDirect, canAsk, onBack = { open = null }) { amt -> run { Points.change(d, me, isOwner, open!!, amt) } }
            else -> {
                val people = d.people()
                if (people.isEmpty()) Text("Nobody has any points yet.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(people.withIndex().toList()) { (i, u) ->
                        val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${i + 1}" }
                        Card(Modifier.fillMaxWidth().clickable { open = u }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(medal, Modifier.width(44.dp), fontWeight = FontWeight.Bold)
                                Column(Modifier.weight(1f)) {
                                    val role = d.role(u)
                                    Text(d.nameOf(u) + (if (role != "member") "  [$role]" else "") + (if (u == me) "  (you)" else ""), fontWeight = FontWeight.SemiBold)
                                    Text("@$u", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${d.score(u)}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsUser(d: PointsData, u: String, me: String, isOwner: Boolean, canDirect: Boolean, canAsk: Boolean, onBack: () -> Unit, change: (Int?) -> Unit) {
    var custom by remember { mutableStateOf("") }
    var note by remember(u) { mutableStateOf(d.notes.optString(u)) }
    val scope = rememberCoroutineScope()
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("← Back to the ledger") }
        Text("${d.score(u)} points", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("${d.nameOf(u)} · @$u")
        if (canDirect || canAsk) {
            Text(if (canAsk && !canDirect) "Ask the owner to change this" else "Change their points", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 5, 10, 25, -1, -5, -10).forEach { n -> Button(onClick = { change(n) }) { Text(if (n > 0) "+$n" else "$n") } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(custom, { custom = it.filter { c -> c.isDigit() || c == '-' } }, Modifier.weight(1f), placeholder = { Text("Custom (e.g. -3)") }, singleLine = true)
                TextButton(enabled = custom.toIntOrNull()?.let { it != 0 } == true, onClick = { change(custom.toInt()); custom = "" }) { Text("Apply") }
            }
            OutlinedButton(onClick = { change(null) }) { Text("Reset to 0") }
        } else Text("Only the owner and the managers can change points.", fontSize = 13.sp)
        if (isOwner) {
            Text("Your private note about them", fontWeight = FontWeight.Bold)
            OutlinedTextField(note, { note = it }, placeholder = { Text("e.g. really good at geometry") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { withContext(Dispatchers.IO) { Points.saveNote(u, note) } } }) { Text("Save note") }
        }
    }
}
