package it.scannerposte.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import java.io.File

/**
 * Crea il file "scan": ridisegna lo stesso Data Matrix letto a partire dal suo valore.
 * Il risultato e' dritto, nitido, senza spazi inutili e con la stessa forma dell'originale
 * (rettangolare se il codice sulla busta e' rettangolare).
 */
object DataMatrixImage {

    fun cartella(context: Context, aggregatoId: Long): File =
        File(context.filesDir, "aggregati/$aggregatoId")

    /** Matrice di moduli 1:1, senza bordo e senza padding. */
    private fun codifica(testo: String, rettangolare: Boolean): BitMatrix {
        val writer = DataMatrixWriter()
        if (rettangolare) {
            try {
                val hint = mapOf<EncodeHintType, Any>(
                    EncodeHintType.DATA_MATRIX_SHAPE to SymbolShapeHint.FORCE_RECTANGLE
                )
                // Con dimensione richiesta 1x1 la libreria restituisce esattamente i moduli, senza padding.
                return writer.encode(testo, BarcodeFormat.DATA_MATRIX, 1, 1, hint)
            } catch (e: Exception) {
                // Contenuto troppo lungo per un rettangolo: si ripiega sul quadrato.
            }
        }
        return writer.encode(testo, BarcodeFormat.DATA_MATRIX, 1, 1)
    }

    fun salva(context: Context, aggregatoId: Long, testo: String, rettangolare: Boolean): File {
        val matrice = codifica(testo, rettangolare)
        val lato = maxOf(matrice.width, matrice.height)
        val modulo = (1200 / lato).coerceIn(6, 20)      // pixel per modulo, sempre intero = bordi nitidi
        val margine = 3 * modulo                         // zona bianca (quiet zone)
        val w = matrice.width * modulo + 2 * margine
        val h = matrice.height * modulo + 2 * margine

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val nero = Paint().apply { color = Color.BLACK; isAntiAlias = false }
        for (y in 0 until matrice.height) {
            for (x in 0 until matrice.width) {
                if (matrice.get(x, y)) {
                    val left = (margine + x * modulo).toFloat()
                    val top = (margine + y * modulo).toFloat()
                    canvas.drawRect(left, top, left + modulo, top + modulo, nero)
                }
            }
        }

        val dir = cartella(context, aggregatoId).apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }
}
