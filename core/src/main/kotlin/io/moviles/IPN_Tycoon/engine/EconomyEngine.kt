package io.moviles.IPN_Tycoon.engine

import io.moviles.IPN_Tycoon.GameState

/** Paso 4: sin ingresos automáticos por alumno ni por tiempo. */
class EconomyEngine : GameCycleListener {
    override val resolutionOrder = ResolutionOrder.ECONOMY
    data class CycleResult(val ingresos: Long)
    var lastResult: CycleResult? = null
        private set
    override fun onResolveCycle(cycle: Int) {
        lastResult = CycleResult(0L)
        GameState.ciclosJugados++
    }
}
