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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import kotlin.math.roundToInt
import java.util.Locale
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

// Intervallo tra un file e il successivo, espresso in centesimi di secondo.
// Barra simmetrica: 0,50 s ... 3,50 s con la taratura di base (2,00 s) esattamente al centro.
private const val MIN_CS = 50
private const val BASE_CS = 200
private const val MAX_CS = 350
private const val PREFS = "impostazioni"
private const val CHIAVE_INTERVALLO = "intervallo_cs"

private fun secondi(cs: Int): String = String.format(Locale.ITALY, "%.2f s", cs / 100.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val codici by produceState<List<Codice>>(emptyList(), id) { value = vm.codiciOra(id) }
    val tutti by vm.aggregati.collectAsStateWithLifecycle()
    val numero = tutti.indexOfFirst { it.id == id } + 1
    var indice by remember { mutableIntStateOf(0) }
    var inRiproduzione by remember { mutableStateOf(false) }

    // Velocita' di scorrimento: si ricorda anche alla prossima apertura.
    val contesto = LocalContext.current
    val prefs = remember { contesto.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var centesimi by remember { mutableIntStateOf(prefs.getInt(CHIAVE_INTERVALLO, BASE_CS).coerceIn(MIN_CS, MAX_CS)) }
    val salvaVelocita = { prefs.edit().putInt(CHIAVE_INTERVALLO, centesimi).apply() }
    val imposta = { v: Int -> centesimi = v.coerceIn(MIN_CS, MAX_CS); salvaVelocita() }

    // Riproduzione automatica: cambio file ogni 2 secondi, si ferma sull'ultimo.
    LaunchedEffect(inRiproduzione, indice, codici.size, centesimi) {
        if (inRiproduzione) {
            delay(centesimi * 10L)
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

            // Regolazione velocita': barra con base 2,00 s al centro e passo di 0,01 s.
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imposta(centesimi - 1) }) {
                        Icon(Icons.Default.Remove, "Diminuisci di 0,01 secondi")
                    }
                    Text(
                        "Cambio file ogni ${secondi(centesimi)}",
                        fontWeight = FontWeight.Bold, color = Navy
                    )
                    IconButton(onClick = { imposta(centesimi + 1) }) {
                        Icon(Icons.Default.Add, "Aumenta di 0,01 secondi")
                    }
                }
                Slider(
                    value = centesimi.toFloat(),
                    onValueChange = { centesimi = it.roundToInt() },
                    onValueChangeFinished = { salvaVelocita() },
                    valueRange = MIN_CS.toFloat()..MAX_CS.toFloat(),
                    steps = MAX_CS - MIN_CS - 1,   // un passo per ogni centesimo di secondo
                    colors = SliderDefaults.colors(
                        thumbColor = Blue, activeTrackColor = Blue,
                        activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
                    )
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(secondi(MIN_CS), Modifier.weight(1f), color = Color.Gray)
                    TextButton(onClick = { imposta(BASE_CS) }) {
                        Text("Base ${secondi(BASE_CS)}", color = if (centesimi == BASE_CS) Green else Red)
                    }
                    Text(
                        secondi(MAX_CS), Modifier.weight(1f),
                        color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
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
