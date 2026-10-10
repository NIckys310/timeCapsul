package com.timecapsule.app.ui.screens

import com.timecapsule.app.ui.theme.NightSurface
import com.timecapsule.app.ui.components.paper
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Repo
import com.timecapsule.app.ui.MainViewModel
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.Night

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(
    Tab("Cápsulas", Icons.Filled.Inventory2),
    Tab("Línea de tiempo", Icons.Filled.Timeline),
    Tab("Mapa", Icons.Filled.Map),
    Tab("Perfil", Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "Mis cápsulas" else tabs[tab].label, style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (tab == 0) ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Nueva cápsula") },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = NightSurface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label, maxLines = 1) })
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> CapsuleList(vm, onOpen)
                1 -> TimelineScreen(vm, onOpen)
                2 -> MapScreen(vm, onOpen)
                else -> ProfileScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapsuleList(vm: MainViewModel, onOpen: (String) -> Unit) {
    val caps by vm.capsules.collectAsState()
    val loading by vm.loading.collectAsState()
    val me = Repo.userId()
    val ready = caps.filter { !it.isOpen && it.dateReady && it.requirePlace }
    val open = caps.filter { it.isOpen }
    val locked = caps.filter { !it.isOpen && !(it.dateReady && it.requirePlace) }
        .sortedBy { it.unlockInstant }

    PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        if (caps.isEmpty() && !loading) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    EmptyState("Tu primera cápsula",
                        "Guarda fotos, videos y una carta para tu yo del futuro. Elige cuándo o dónde se podrá abrir.")
                }
            }
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            section("Te esperan en su lugar", ready, me, onOpen)
            section("Cerradas", locked, me, onOpen)
            section("Abiertas", open, me, onOpen)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String, list: List<Capsule>, me: String?, onOpen: (String) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "h_$title") { Text(title, color = Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
    items(list, key = { it.id }) { c -> CapsuleCard(c, me) { onOpen(c.id) } }
}
