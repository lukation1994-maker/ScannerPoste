package it.scannerposte.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scannerposte.AppViewModel
import it.scannerposte.MAX_CODICI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AggregatoScreen(
    vm: AppViewModel,
    id: Long,
    onScan: () -> Unit,
    onPlay: () -> Unit,
    onSalva: () -> Unit,
    onBack: () -> Unit
) {
    val codici by vm.codici(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)
    // Numero mostrato = posizione nella lista (sempre 1, 2, 3 senza buchi).
    val tutti by vm.aggregati.collectAsStateWithLifecycle()
    val numero = tutti.indexOfFirst { it.id == id } + 1

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SCANNER POSTE", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy, titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Soft).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    enabled = codici.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) { Icon(Icons.Default.PlayArrow, null); Text(" PLAY") }
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("SCAN") }
                Button(
                    onClick = onSalva,
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Icon(Icons.Default.Check, null); Text(" SALVA") }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${codici.size}/$MAX_CODICI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    "AGGREGATO $numero",
                    color = Blue, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFE7ECFB), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            if (codici.isEmpty()) {
                Text("Nessun codice. Premi SCAN per iniziare.", color = Color.Gray)
            }
            LazyColumn {
                items(codici.asReversed(), key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• ${c.valore}", modifier = Modifier.weight(1f), maxLines = 1)
                        Text(formato.format(Date(c.scansionatoIl)), color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
