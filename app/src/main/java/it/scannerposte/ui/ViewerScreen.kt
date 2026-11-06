package it.scannerposte.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scannerposte.AppViewModel
import it.scannerposte.data.Codice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private const val INTERVALLO_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val codici by produceState<List<Codice>>(emptyList(), id) { value = vm.codiciOra(id) }
    val tutti by vm.aggregati.collectAsStateWithLifecycle()
    val numero = tutti.indexOfFirst { it.id == id } + 1
    var indice by remember { mutableIntStateOf(0) }
    var inRiproduzione by remember { mutableStateOf(false) }

    // Riproduzione automatica: cambio file ogni 2 secondi, si ferma sull'ultimo.
    LaunchedEffect(inRiproduzione, indice, codici.size) {
        if (inRiproduzione) {
            delay(INTERVALLO_MS)
            if (indice < codici.size - 1) indice++ else inRiproduzione = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Aggregato $numero", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy, titleContentColor = Color.White
                )
            )
        }
    ) { pad ->
        if (codici.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Nessun file da mostrare", color = Color.Gray)
            }
            return@Scaffold
        }
        val corrente = codici[indice.coerceIn(0, codici.size - 1)]
        val bitmap by produceState<ImageBitmap?>(null, corrente.file) {
            value = withContext(Dispatchers.IO) {
                File(corrente.file).takeIf { it.exists() }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
            }
        }

        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(it, "Scan", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
                }
            }
            Text(corrente.valore, modifier = Modifier.padding(top = 12.dp), maxLines = 2)
            Text("${indice + 1} / ${codici.size}", color = Color.Gray)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { if (indice > 0) indice-- },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Navy)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro di 1 file") }
                FilledIconButton(
                    onClick = {
                        // Se e' fermo sull'ultimo file, il play riparte dall'inizio.
                        if (!inRiproduzione && indice >= codici.size - 1) indice = 0
                        inRiproduzione = !inRiproduzione
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)
                ) {
                    Icon(
                        if (inRiproduzione) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pausa"
                    )
                }
                FilledIconButton(
                    onClick = { if (indice < codici.size - 1) indice++ },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Navy)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Avanti di 1 file") }
            }
        }
    }
}
