package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music

/**
 * EDU_AUDIO_V4: música original continua y ambientes por edificio.
 * El final de un OGG no deja la pantalla en silencio; el ciclo se reinicia
 * mediante OnCompletionListener y una comprobación de recuperación.
 */
class AudioAmbiente {
    private var actual: Music? = null
    private var saliente: Music? = null
    private var rutaActual: String? = null
    private var transicion = 0f
    private var suspendido = false
    private var tiempoReintento = 0f
    private var audioDisponible = true

    fun campus() = cambiar("audio/campus.ogg")
    fun interior(propiedad: Propiedad) = cambiar("audio/interiores/${propiedad.id}.ogg")

    private fun cambiar(ruta: String) {
        if (ruta == rutaActual && actual != null) {
            // Puede ocurrir tras regresar del menú o después de un evento de Android.
            suspendido = false
            tiempoReintento = 0f
            return
        }
        val archivo = Gdx.files.internal(ruta)
        if (!archivo.exists()) {
            Gdx.app.error("EDU_AUDIO", "No existe la pista: $ruta")
            if (ruta != "audio/campus.ogg") campus()
            return
        }
        try {
            val nueva = Gdx.audio.newMusic(archivo)
            nueva.isLooping = true
            nueva.volume = 0f
            nueva.setOnCompletionListener { pista ->
                // Algunas implementaciones Android no repiten OGG aun con looping.
                if (pista === actual && !suspendido && GameState.musicaActiva) {
                    try {
                        pista.setPosition(0f)
                        pista.play()
                    } catch (e: Exception) {
                        Gdx.app.error("EDU_AUDIO", "Error al repetir $ruta", e)
                    }
                }
            }
            saliente?.stop()
            saliente?.dispose()
            saliente = actual
            actual = nueva
            rutaActual = ruta
            transicion = 0f
            tiempoReintento = 0f
            suspendido = false
            audioDisponible = true
            if (GameState.musicaActiva) nueva.play()
            Gdx.app.log("EDU_AUDIO", "Ambiente activo: $ruta (loop)")
        } catch (e: Exception) {
            Gdx.app.error("EDU_AUDIO", "No fue posible reproducir $ruta", e)
            audioDisponible = false
        }
    }

    fun actualizar(delta: Float) {
        val dt = delta.coerceIn(0f, 0.25f)
        if (suspendido || !GameState.musicaActiva) {
            actual?.pause()
            saliente?.pause()
            return
        }
        val pista = actual ?: return
        tiempoReintento += dt
        // Recupera fallos de reproducción y reinicia el OGG al terminar.
        if (!pista.isPlaying && tiempoReintento >= 0.7f) {
            try {
                pista.isLooping = true
                pista.setPosition(0f)
                pista.play()
                audioDisponible = true
                tiempoReintento = 0f
            } catch (e: Exception) {
                if (audioDisponible) Gdx.app.error("EDU_AUDIO", "Reproducción interrumpida; se reintentará", e)
                audioDisponible = false
                tiempoReintento = 0f
            }
        }
        transicion = (transicion + dt / 1.4f).coerceIn(0f, 1f)
        // Música base más discreta que los efectos característicos de interiores.
        val destino = if (rutaActual == "audio/campus.ogg") 0.19f else 0.31f
        pista.volume = destino * transicion
        saliente?.volume = 0.28f * (1f - transicion)
        if (transicion >= 1f) {
            saliente?.stop()
            saliente?.dispose()
            saliente = null
        }
    }

    fun suspender() {
        suspendido = true
        actual?.pause()
        saliente?.pause()
    }
    fun reanudar() {
        suspendido = false
        tiempoReintento = 0.7f
    }
    fun dispose() {
        actual?.stop(); actual?.dispose()
        saliente?.stop(); saliente?.dispose()
        actual = null
        saliente = null
        rutaActual = null
    }
}
