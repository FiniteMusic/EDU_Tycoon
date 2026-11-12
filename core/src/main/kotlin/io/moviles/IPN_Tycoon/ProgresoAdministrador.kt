package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx

/** Paso 4: progreso, necesidades y recompensas; no altera niveles visuales de edificio. */
object ProgresoAdministrador {
    const val ENERGIA_MAX = 20
    const val SALUD_MAX = 100
    const val RETOS_POR_EDIFICIO = 5

    var experiencia: Int = 0
        private set
    var energia: Int = ENERGIA_MAX
        private set
    var salud: Int = SALUD_MAX
        private set
    var tiempoSinComer: Float = 0f
        private set
    private val retosTerminados = mutableMapOf<String, Int>()

    /** Requisitos de experiencia PARA avanzar desde un nivel determinado. */
    fun xpNecesariaParaNivel(nivel: Int): Int = when (nivel) {
        1 -> 250
        2 -> 400
        3 -> 600
        4 -> 850
        5 -> 1150
        else -> 1150 + (nivel - 5).coerceAtLeast(0) * 350
    }

    val nivelConocimiento: Int get() {
        var restante = experiencia
        var nivel = 1
        while (nivel < 1000) {
            val requerida = xpNecesariaParaNivel(nivel)
            if (restante < requerida) break
            restante -= requerida
            nivel++
        }
        return nivel
    }
    val xpNivel: Int get() {
        var restante = experiencia
        var nivel = 1
        while (nivel < 1000) {
            val requerida = xpNecesariaParaNivel(nivel)
            if (restante < requerida) break
            restante -= requerida
            nivel++
        }
        return restante
    }
    val xpSiguienteNivel: Int get() = xpNecesariaParaNivel(nivelConocimiento)

    data class ResultadoReto(
        val correcto: Boolean,
        val experienciaGanada: Int = 0,
        val dineroGanado: Long = 0L,
        val energiaRestante: Int = energia,
        val mensaje: String = ""
    )

    data class Alimento(val nombre: String, val precio: Long, val recuperacion: Int)
    val menuRestaurante = listOf(
        Alimento("Ensalada completa", 2500L, 20),
        Alimento("Menú universitario", 4500L, 35),
        Alimento("Plato nutritivo", 7000L, 55)
    )
    val menuPuesto = listOf(
        Alimento("Fruta de temporada", 1000L, 12),
        Alimento("Sándwich", 2500L, 24),
        Alimento("Comida casera", 4000L, 36)
    )

    fun reset() {
        experiencia = 0
        energia = ENERGIA_MAX
        salud = SALUD_MAX
        tiempoSinComer = 0f
        retosTerminados.clear()
    }

    fun completados(id: String): Int = (retosTerminados[id] ?: 0).coerceIn(0, RETOS_POR_EDIFICIO)
    fun siguienteReto(id: String): Int = (completados(id) + 1).coerceAtMost(RETOS_POR_EDIFICIO)

    /** Cada lista indica el nivel exigido para pasar el edificio a niveles 2, 3 y 4. */
    private val requisitosDeMejora = mapOf(
        "escom_hitbox" to intArrayOf(5, 6, 7),
        "Direccion" to intArrayOf(5, 6, 7),
        "Mac_and_cheese" to intArrayOf(2, 4, 6),
        "cafeteria" to intArrayOf(2, 3, 5),
        "auditorio" to intArrayOf(4, 6, 7),
        "Arquitectura" to intArrayOf(4, 6, 7),
        "Biologicas" to intArrayOf(3, 5, 7),
        "Bioquimica" to intArrayOf(5, 6, 7),
        "Matematicas" to intArrayOf(4, 6, 7),
        "Edificio1" to intArrayOf(3, 5, 7),
        "Edificio2" to intArrayOf(4, 6, 7),
        "Museo" to intArrayOf(5, 6, 7),
        "Turismo" to intArrayOf(3, 6, 7),
        "palapas" to intArrayOf(2, 4, 6),
        "Biblioteca" to intArrayOf(3, 5, 7),
        "Arte_diseno_deporte" to intArrayOf(4, 6, 7),
        "Dormitorios" to intArrayOf(2, 3, 5),
        "Gastronomia" to intArrayOf(3, 5, 7)
    )
    fun conocimientoRequerido(propiedad: Propiedad): Int {
        if (!propiedad.comprada) {
            return if (propiedad.id in setOf("Direccion", "Museo", "Bioquimica")) 3 else 1
        }
        val indice = (propiedad.nivel - 1).coerceAtLeast(0)
        val escalones = requisitosDeMejora[propiedad.id] ?: intArrayOf(3, 5, 7)
        return escalones.getOrElse(indice) { escalones.last() + (indice - escalones.lastIndex) * 3 }
    }
    fun puedeConstruirOMejorar(propiedad: Propiedad): Boolean =
        nivelConocimiento >= conocimientoRequerido(propiedad)

    fun jugar(propiedad: Propiedad, respuestaCorrecta: Boolean): ResultadoReto {
        if (!propiedad.comprada) return ResultadoReto(false, mensaje = "Debes comprar el edificio para comenzar.")
        if (energia <= 0) return ResultadoReto(false, mensaje = "No tienes energía. Descansa en Dormitorios.")
        val nivel = siguienteReto(propiedad.id)
        val repaso = completados(propiedad.id) >= RETOS_POR_EDIFICIO
        if (!respuestaCorrecta) {
            energia = (energia - 2).coerceAtLeast(0)
            return ResultadoReto(false, energiaRestante = energia,
                mensaje = "Respuesta incorrecta. Energía -2. Puedes volver a intentarlo.")
        }
        val xp = if (repaso) 0 else 25 + nivel * 15
        val base = 35_000L + nivel * 20_000L + (nivelConocimiento - 1) * 12_000L
        val recompensa = if (repaso) base / 2L else base
        retosTerminados[propiedad.id] = nivel
        experiencia += xp
        GameState.acreditar(recompensa)
        return ResultadoReto(true, xp, recompensa, energia,
            if (repaso) "¡Repaso superado! +\$$recompensa. Los cinco niveles ya están completados."
            else "¡Reto $nivel superado! +$xp conocimiento y +\$$recompensa.")
    }

    fun descansarEnDormitorios(): Int {
        val recuperado = ENERGIA_MAX - energia
        energia = ENERGIA_MAX
        return recuperado
    }

    fun comer(alimento: Alimento): Boolean {
        if (salud <= 0 || !GameState.gastar(alimento.precio)) return false
        salud = (salud + alimento.recuperacion).coerceAtMost(SALUD_MAX)
        tiempoSinComer = 0f
        return true
    }

    /** Un minuto real en el campus sin comer: -5 de salud. Retos no avanzan el reloj extra. */
    fun avanzarTiempo(delta: Float): Boolean {
        if (salud <= 0) return true
        tiempoSinComer += delta.coerceIn(0f, 0.25f)
        while (tiempoSinComer >= 60f && salud > 0) {
            tiempoSinComer -= 60f
            salud = (salud - 5).coerceAtLeast(0)
        }
        return salud == 0
    }

    private fun prefs() = Gdx.app.getPreferences("edu_tycoon_paso4")
    /** Persistencia complementaria a Room: un prefijo independiente por slot. */
    fun guardar(slot: Int) {
        if (slot !in 1..3) return
        val p = prefs()
        val clave = "s${slot}_"
        p.putInteger(clave + "xp", experiencia)
        p.putInteger(clave + "energia", energia)
        p.putInteger(clave + "salud", salud)
        p.putFloat(clave + "sinComer", tiempoSinComer)
        for (id in PropiedadRepository.propiedades.keys) {
            p.putInteger(clave + "reto_" + id, completados(id))
        }
        p.flush()
    }
    fun cargar(slot: Int) {
        reset()
        if (slot !in 1..3) return
        val p = prefs()
        val clave = "s${slot}_"
        experiencia = p.getInteger(clave + "xp", 0).coerceAtLeast(0)
        energia = p.getInteger(clave + "energia", ENERGIA_MAX).coerceIn(0, ENERGIA_MAX)
        salud = p.getInteger(clave + "salud", SALUD_MAX).coerceIn(0, SALUD_MAX)
        tiempoSinComer = p.getFloat(clave + "sinComer", 0f).coerceIn(0f, 60f)
        for (id in PropiedadRepository.propiedades.keys) {
            val n = p.getInteger(clave + "reto_" + id, 0).coerceIn(0, RETOS_POR_EDIFICIO)
            if (n > 0) retosTerminados[id] = n
        }
    }
    fun borrar(slot: Int) {
        if (slot !in 1..3) return
        val p = prefs()
        val clave = "s${slot}_"
        for (campo in listOf("xp", "energia", "salud", "sinComer")) p.remove(clave + campo)
        for (id in PropiedadRepository.propiedades.keys) p.remove(clave + "reto_" + id)
        p.flush()
    }
}
