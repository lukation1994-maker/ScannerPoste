package it.scannerposte.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.datamatrix.DataMatrixWriter
import java.io.File

/**
 * Crea il file "scan": invece di una foto, ridisegna lo stesso Data Matrix letto a partire dal suo valore.
 * Il risultato e' sempre dritto, simmetrico, nitido e con il bordo bianco (quiet zone) corretto.
 */
object DataMatrixImage {

    fun cartella(context: Context, aggregatoId: Long): File =
        File(context.filesDir, "aggregati/$aggregatoId")

    fun salva(context: Context, aggregatoId: Long, testo: String): File {
        // Richiede 600x600: la libreria usa un multiplo intero della dimensione del modulo, quindi niente sfocature.
        val matrice = DataMatrixWriter().encode(testo, BarcodeFormat.DATA_MATRIX, 600, 600)
        val margine = maxOf(matrice.width, matrice.height) / 8
        val w = matrice.width + 2 * margine
        val h = matrice.height + 2 * margine
        val pixel = IntArray(w * h) { Color.WHITE }
        for (y in 0 until matrice.height) {
            for (x in 0 until matrice.width) {
                if (matrice.get(x, y)) pixel[(y + margine) * w + x + margine] = Color.BLACK
            }
        }
        val bitmap = Bitmap.createBitmap(pixel, w, h, Bitmap.Config.ARGB_8888)
        val dir = cartella(context, aggregatoId).apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }
}
