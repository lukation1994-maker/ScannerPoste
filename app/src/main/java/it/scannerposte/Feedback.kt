package it.scannerposte

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

/** Bip forte e chiaro + vibrazione lieve a ogni codice acquisito. */
class Feedback(context: Context) {

    private val vibratore: Vibrator? = context.getSystemService(Vibrator::class.java)

    // Volume massimo del generatore di toni (segue il volume "multimediale" del telefono).
    private val tono: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
    } catch (e: RuntimeException) {
        null
    }

    fun codiceAcquisito() {
        tono?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        vibratore?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createOneShot(40, 90)) // 40 ms, intensita' bassa
    }

    fun rilascia() {
        tono?.release()
    }
}
