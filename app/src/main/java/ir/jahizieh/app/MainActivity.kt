package ir.jahizieh.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

val Rose = Color(0xFFD81B60)
val RoseDark = Color(0xFF880E4F)
val Blush = Color(0xFFFFF5F8)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("jahizieh", Context.MODE_PRIVATE)
    fun checked(): Set<String> = sp.getStringSet("checked", emptySet())!!.toSet()
    fun custom(): Set<String> = sp.getStringSet("custom", emptySet())!!.toSet()
    fun saveChecked(s: Set<String>) = sp.edit().putStringSet("checked", s).apply()
    fun saveCustom(s: Set<String>) = sp.edit().putStringSet("custom", s).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Rose, secondary = RoseDark, background = Blush)) {
                    App(store)
                }
            }
        }
    }
}

@Composable
fun App(store: Store) {
    var checked by remember { mutableStateOf(store.checked()) }
    var custom by remember { mutableStateOf(store.custom()) }
    var open by remember { mutableStateOf<Cat?>(null) }

    fun itemsOf(c: Cat): List<String> =
        c.items.map { "${c.id}|$it" } + custom.filter { it.startsWith("${c.id}|") }.sorted()
    fun doneOf(c: Cat) = itemsOf(c).count { it in checked }

    BackHandler(enabled = open != null) { open = null }

    val cat = open
    if (cat == null) {
        val total = Categories.sumOf { itemsOf(it).size }
        val done = Categories.sumOf { doneOf(it) }
        Home(total, done, ::itemsOf, ::doneOf) { open = it }
    } else {
        Detail(
            cat = cat, keys = itemsOf(cat), checked = checked,
            onToggle = { k ->
                checked = if (k in checked) checked - k else checked + k
                store.saveChecked(checked)
            },
            onAdd = { name ->
                custom = custom + "${cat.id}|$name"
                store.saveCustom(custom)
            },
            onDelete = { k ->
                custom = custom - k; checked = checked - k
                store.saveCustom(custom); store.saveChecked(checked)
            },
            onBack = { open = null }
        )
    }
}

@Composable
fun Home(total: Int, done: Int, itemsOf: (Cat) -> List<String>, doneOf: (Cat) -> Int, onOpen: (Cat) -> Unit) {
    val p = if (total == 0) 0f else done / total.toFloat()
    Column(Modifier.fillMaxSize().background(Blush)) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(RoseDark, Rose)), RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .padding(24.dp)
        ) {
            Column {
                Text("جهیزیه من 💕", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("همه‌چیز برای شروع یک زندگی قشنگ", color = Color(0xFFFFE0EC), fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = { p }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Color.White, trackColor = Color(0x55FFFFFF)
                )
                Spacer(Modifier.height(8.dp))
                Text("$done از $total مورد آماده شد  (${(p * 100).toInt()}٪)", color = Color.White, fontSize = 13.sp)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            gridItems(Categories) { c ->
                val t = itemsOf(c).size
                val d = doneOf(c)
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(c.c1, c.c2)))
                        .clickable { onOpen(c) }.padding(16.dp)
                ) {
                    Text(c.emoji, fontSize = 40.sp, modifier = Modifier.align(Alignment.TopStart))
                    Column(Modifier.align(Alignment.BottomStart)) {
                        Text(c.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("$d از $t", color = Color(0xFFFFF3F8), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (t == 0) 0f else d / t.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Color.White, trackColor = Color(0x44FFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Detail(
    cat: Cat, keys: List<String>, checked: Set<String>,
    onToggle: (String) -> Unit, onAdd: (String) -> Unit, onDelete: (String) -> Unit, onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val done = keys.count { it in checked }
    val prefix = cat.id + "|"

    Box(Modifier.fillMaxSize().background(Blush)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(cat.c1, cat.c2)), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Column {
                    TextButton(onClick = onBack) { Text("→ بازگشت", color = Color.White, fontSize = 15.sp) }
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.emoji, fontSize = 38.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(cat.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("$done از ${keys.size} مورد", color = Color(0xFFFFF3F8), fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (keys.isEmpty()) 0f else done / keys.size.toFloat() },
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color.White, trackColor = Color(0x44FFFFFF)
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(keys) { k ->
                    val name = k.removePrefix(prefix)
                    val isChecked = k in checked
                    val isCustom = name !in cat.items
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isChecked) Color(0xFFFCE4EC) else Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(k) }
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked, onCheckedChange = { onToggle(k) },
                                colors = CheckboxDefaults.colors(checkedColor = cat.c2)
                            )
                            Text(
                                name, modifier = Modifier.weight(1f), fontSize = 16.sp,
                                color = if (isChecked) Color.Gray else Color(0xFF3E2723),
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null
                            )
                            if (isCustom) TextButton(onClick = { onDelete(k) }) { Text("✕", color = Color(0xFFB71C1C)) }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            containerColor = cat.c2, contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Text("＋ افزودن مورد", fontSize = 15.sp) }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("مورد جدید") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("مثلاً: سرویس قابلمه") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = text.trim().replace("|", " ")
                    if (t.isNotEmpty()) onAdd(t)
                    text = ""; showDialog = false
                }) { Text("افزودن") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("انصراف") } }
        )
    }
}
