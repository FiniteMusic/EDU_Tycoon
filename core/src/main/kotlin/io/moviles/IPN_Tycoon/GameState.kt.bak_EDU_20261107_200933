package io.moviles.IPN_Tycoon

object GameState {

    // ── Identidad de sesión ───────────────────────────────────────────
    var saveId: Int       = 0   // 0 = sin guardar aún
    var slotActual: Int   = 0   // 1, 2, 3 = slots reales
    var nombreJugador: String = ""
    var nombreEscuela: String = ""
    var ciclosJugados: Int    = 0
    var musicaActiva: Boolean = true

    // ── Economía ──────────────────────────────────────────────────────
    const val DINERO_INICIAL_REAL: Long = 500_000L
    const val DINERO_INICIAL_TEST: Long = 100_000_000L

    var dinero: Long = DINERO_INICIAL_REAL
    var alumnosTotales: Int = 0

    // ── API de dinero ─────────────────────────────────────────────────
    fun puedeComprar(costo: Long) = dinero >= costo

    fun gastar(cantidad: Long): Boolean {
        if (dinero < cantidad) return false
        dinero -= cantidad
        return true
    }

    fun acreditar(cantidad: Long) { dinero += cantidad }

    fun costoMejora(propiedad: Propiedad): Long =
        propiedad.precio * propiedad.nivel

    // ── Reset completo para nueva partida ─────────────────────────────
    fun reset(presupuestoInicial: Long = DINERO_INICIAL_REAL) {
        saveId         = 0
        slotActual     = 0
        nombreJugador  = ""
        nombreEscuela  = ""
        ciclosJugados  = 0
        dinero         = presupuestoInicial
        alumnosTotales = 0
    }
}
