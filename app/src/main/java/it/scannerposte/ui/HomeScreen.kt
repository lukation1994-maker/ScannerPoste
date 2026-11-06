package it.scannerposte.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scannerposte.AppViewModel
import it.scannerposte.MAX_CODICI
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, onApri: (Long) -> Unit) {
    val lista by vm.aggregati.collectAsStateWithLifecycle()
    var daEliminare by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SCANNER POSTE", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy, titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { scope.launch { onApri(vm.nuovoAggregato()) } },
                containerColor = Red, contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Nuovo aggregato") }
        }
    ) { pad ->
        if (lista.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Nessun aggregato\nTocca + per iniziare", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
                items(lista, key = { it.id }) { a ->
                    // Swipe da destra a sinistra: non elimina subito, chiede conferma.
                    val stato = rememberSwipeToDismissBoxState(confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) daEliminare = a.id
                        false
                    })
                    SwipeToDismissBox(
                        state = stato,
                        enableDismissFromStartToEnd = false,
                        modifier = Modifier.padding(vertical = 4.dp),
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Red, RoundedCornerShape(12.dp))
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) { Icon(Icons.Default.Delete, null, tint = Color.White) }
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onApri(a.id) },
                            colors = CardDefaults.cardColors(containerColor = Soft),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aggregato ${a.id}", fontWeight = FontWeight.Bold, color = Navy)
                                Text("${a.conteggio}/$MAX_CODICI", color = Blue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    daEliminare?.let { id ->
        AlertDialog(
            onDismissRequest = { daEliminare = null },
            title = { Text("Eliminare l'aggregato $id?") },
            text = { Text("Verranno eliminati anche tutti i suoi codici e i file scan.") },
            confirmButton = {
                TextButton(onClick = { vm.eliminaAggregato(id); daEliminare = null }) {
                    Text("Elimina", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { daEliminare = null }) { Text("Annulla") } }
        )
    }
}
