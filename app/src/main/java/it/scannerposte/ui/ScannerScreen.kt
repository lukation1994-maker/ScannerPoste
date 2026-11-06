package it.scannerposte.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import it.scannerposte.AppViewModel
import it.scannerposte.Lettura
import it.scannerposte.MAX_CODICI
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    vm: AppViewModel,
    id: Long,
    onConferma: () -> Unit,
    onCompleto: () -> Unit
) {
    val context = LocalContext.current
    val codici by vm.codici(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val duplicato by vm.duplicato.collectAsStateWithLifecycle()
    val tutti by vm.aggregati.collectAsStateWithLifecycle()
    val completato by vm.completato.collectAsStateWithLifecycle()

    var permesso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val richiesta = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permesso = it
    }

    LaunchedEffect(Unit) {
        vm.iniziaScansione()
        if (!permesso) richiesta.launch(Manifest.permission.CAMERA)
    }
    // A 600/600 l'aggregato e' pieno: salvataggio automatico e ritorno alla lista.
    LaunchedEffect(completato) {
        if (completato) {
            vm.completato.value = false
            onCompleto()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permesso) {
            CameraPreview(onCodici = { vm.onCodiciLetti(id, it) })
        } else {
            Text(
                "Serve il permesso della fotocamera per leggere i Data Matrix.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }

        Text(
            "${codici.size}/$MAX_CODICI",
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
        Text(
            "Inquadra il Data Matrix: la lettura è automatica",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp, start = 16.dp, end = 96.dp)
        )
        FloatingActionButton(
            onClick = onConferma,
            containerColor = Green, contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Default.Check, "Conferma") }
    }

    duplicato?.let {
        val stesso = it.aggregatoId == id
        val numero = tutti.indexOfFirst { a -> a.id == it.aggregatoId } + 1
        val dove = if (stesso) "in questo aggregato (Aggregato $numero)" else "nell'Aggregato $numero"
        AlertDialog(
            onDismissRequest = { vm.annullaDuplicato() },
            title = { Text("Codice già presente") },
            text = {
                Text(
                    "Questo codice è già presente $dove:\n${it.valore}\n\n" +
                        "Non è stato aggiunto. Vuoi eliminarlo da lì oppure annullare e tornare alla scansione?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.eliminaDuplicato() }) {
                    Text(if (stesso) "Elimina" else "Elimina dall'Aggr. $numero", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { vm.annullaDuplicato() }) { Text("Annulla") } }
        )
    }
}

/** Dai 4 angoli del simbolo ricava il rapporto lati: oltre 1,6 e' un Data Matrix rettangolare. */
private fun eRettangolare(b: Barcode): Boolean {
    val p = b.cornerPoints
    val (larghezza, altezza) = if (p != null && p.size >= 4) {
        Math.hypot((p[1].x - p[0].x).toDouble(), (p[1].y - p[0].y).toDouble()) to
            Math.hypot((p[2].x - p[1].x).toDouble(), (p[2].y - p[1].y).toDouble())
    } else {
        val r = b.boundingBox ?: return false
        r.width().toDouble() to r.height().toDouble()
    }
    if (larghezza <= 0 || altezza <= 0) return false
    return maxOf(larghezza, altezza) / minOf(larghezza, altezza) > 1.6
}

@Composable
private fun CameraPreview(onCodici: (List<Lettura>) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onCodici)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX).build()
        )
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val preview = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val cameraProvider = future.get()
                provider = cameraProvider
                val anteprima = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
                val analisi = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analisi.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(img)
                        .addOnSuccessListener { list ->
                            callback(list.mapNotNull { b ->
                                b.rawValue?.let { Lettura(it, eRettangolare(b)) }
                            })
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, anteprima, analisi
                )
            }, ContextCompat.getMainExecutor(ctx))
            preview
        }
    )
}
