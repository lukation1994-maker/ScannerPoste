package it.scannerposte

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import it.scannerposte.data.Aggregato
import it.scannerposte.data.Codice
import it.scannerposte.data.DataMatrixImage
import it.scannerposte.data.Db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val MAX_CODICI = 600

/** Un Data Matrix letto dalla fotocamera: valore + forma (rettangolare o quadrata) del simbolo originale. */
data class Lettura(val valore: String, val rettangolare: Boolean)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = Room.databaseBuilder(app, Db::class.java, "scanner.db").build()
    private val dao = db.dao()
    private val feedback = Feedback(app)

    override fun onCleared() {
        feedback.rilascia()
    }

    val aggregati = dao.aggregati()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun codici(aggregatoId: Long) = dao.codici(aggregatoId)

    suspend fun nuovoAggregato(): Long = withContext(Dispatchers.IO) {
        dao.inserisciAggregato(Aggregato(creatoIl = System.currentTimeMillis()))
    }

    /** Elimina l'aggregato, i suoi codici e tutti i file scan della sua cartella. */
    fun eliminaAggregato(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        DataMatrixImage.cartella(getApplication(), id).deleteRecursively()
        dao.eliminaAggregato(id)
        // Il numero progressivo si riallinea: il prossimo aggregato sara' (numero piu' alto presente) + 1,
        // quindi se non ne resta nessuno si riparte da 1.
        db.openHelper.writableDatabase.execSQL(
            "UPDATE sqlite_sequence SET seq = (SELECT COALESCE(MAX(id), 0) FROM aggregati) WHERE name = 'aggregati'"
        )
    }

    // ---------- Scansione ----------

    /** Codice gia' presente nell'aggregato: se non null mostra l'avviso. */
    val duplicato = MutableStateFlow<Codice?>(null)

    /** Diventa true quando si arriva a 600/600: l'aggregato e' pieno. */
    val completato = MutableStateFlow(false)

    private var ultimo: String? = null
    private var ultimoVisto = 0L

    fun iniziaScansione() {
        ultimo = null
        duplicato.value = null
        completato.value = false
    }

    /** Chiamata a ogni frame dall'analizzatore con i Data Matrix visibili. */
    fun onCodiciLetti(aggregatoId: Long, letture: List<Lettura>) {
        if (duplicato.value != null || completato.value) return
        val valori = letture.map { it.valore }
        val ora = SystemClock.elapsedRealtime()
        // Lo stesso codice che resta inquadrato non va contato due volte.
        if (ultimo != null && ultimo in valori) ultimoVisto = ora
        if (ultimo != null && ora - ultimoVisto > 800) ultimo = null
        val lettura = letture.firstOrNull { it.valore != ultimo } ?: return
        val nuovo = lettura.valore
        ultimo = nuovo
        ultimoVisto = ora

        viewModelScope.launch(Dispatchers.IO) {
            val esistente = dao.trovaOvunque(aggregatoId, nuovo)
            if (esistente != null) {
                duplicato.value = esistente
                return@launch
            }
            if (dao.conta(aggregatoId) >= MAX_CODICI) {
                completato.value = true
                return@launch
            }
            val file = DataMatrixImage.salva(getApplication(), aggregatoId, nuovo, lettura.rettangolare)
            dao.inserisciCodice(
                Codice(
                    aggregatoId = aggregatoId,
                    valore = nuovo,
                    scansionatoIl = System.currentTimeMillis(),
                    file = file.absolutePath
                )
            )
            feedback.codiceAcquisito()
            if (dao.conta(aggregatoId) >= MAX_CODICI) completato.value = true
        }
    }

    /** "Elimina" nell'avviso del duplicato: rimuove il codice (e il suo file) dall'aggregato. */
    fun eliminaDuplicato() {
        val c = duplicato.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            File(c.file).delete()
            dao.eliminaCodice(c.id)
            duplicato.value = null
        }
    }

    /** "Annulla" nell'avviso del duplicato: si torna alla scansione. */
    fun annullaDuplicato() {
        duplicato.value = null
    }

    suspend fun codiciOra(aggregatoId: Long) = withContext(Dispatchers.IO) { dao.codiciOra(aggregatoId) }
}
