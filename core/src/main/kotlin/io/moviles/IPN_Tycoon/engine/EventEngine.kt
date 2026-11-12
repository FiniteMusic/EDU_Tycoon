package io.moviles.IPN_Tycoon.engine

import io.moviles.IPN_Tycoon.GameState
import io.moviles.IPN_Tycoon.PropiedadRepository
import kotlin.random.Random

class EventEngine(
    private val onEvento: (GameEvent) -> Unit
) : GameCycleListener {

    override val resolutionOrder = ResolutionOrder.EVENTS

    private val probabilidadEvento = 0.40f

    private val eventos = listOf(

        // ── Gastos ────────────────────────────────────────────────────
        GameEvent(
            id               = "fuga_agua",
            titulo           = "Fuga de agua",
            descripcion      = "Una tuberia rota requiere reparacion urgente.",
            probabilidad     = 0.25f,
            efecto           = EventoEfecto.Gasto(25_000L),
            requiereEdificios = 1
        ),
        GameEvent(
            id               = "corte_energia",
            titulo           = "Corte de energia",
            descripcion      = "Fallo electrico. Clases suspendidas un dia.",
            probabilidad     = 0.20f,
            efecto           = EventoEfecto.Gasto(40_000L),
            requiereEdificios = 1
        ),
        GameEvent(
            id               = "huelga_estudiantil",
            titulo           = "Huelga estudiantil",
            descripcion      = "Los alumnos exigen mejores condiciones.",
            probabilidad     = 0.15f,
            efecto           = EventoEfecto.Gasto(80_000L),
            requiereEdificios = 2
        ),
        GameEvent(
            id               = "mantenimiento_emergencia",
            titulo           = "Mantenimiento de emergencia",
            descripcion      = "Reparaciones urgentes en infraestructura.",
            probabilidad     = 0.15f,
            efecto           = EventoEfecto.Gasto(50_000L),
            requiereEdificios = 2
        ),
        GameEvent(
            id               = "inspeccion_stps",
            titulo           = "Inspeccion de STPS",
            descripcion      = "Irregularidades detectadas en una revision laboral.",
            probabilidad     = 0.10f,
            efecto           = EventoEfecto.Gasto(60_000L),
            requiereEdificios = 3
        ),

        // ── Ingresos ──────────────────────────────────────────────────
        GameEvent(
            id               = "feria_ciencias",
            titulo           = "Feria de Ciencias",
            descripcion      = "Exitosa feria atrae visitantes y nuevas matriculas.",
            probabilidad     = 0.20f,
            efecto           = EventoEfecto.Ingreso(80_000L),
            requiereEdificios = 1
        ),
        GameEvent(
            id               = "donacion_exalumno",
            titulo           = "Donacion de exalumno",
            descripcion      = "Un exalumno exitoso dona fondos al plantel.",
            probabilidad     = 0.15f,
            efecto           = EventoEfecto.Ingreso(120_000L),
            requiereEdificios = 2
        ),
        GameEvent(
            id               = "beca_conahcyt",
            titulo           = "Beca CONAHCYT",
            descripcion      = "Proyecto de investigacion aprobado. Fondos recibidos.",
            probabilidad     = 0.15f,
            efecto           = EventoEfecto.Ingreso(150_000L),
            requiereEdificios = 2
        ),
        GameEvent(
            id               = "convenio_empresa",
            titulo           = "Convenio empresarial",
            descripcion      = "Empresa tecnologica firma convenio de colaboracion.",
            probabilidad     = 0.10f,
            efecto           = EventoEfecto.Ingreso(200_000L),
            requiereEdificios = 3
        ),
        GameEvent(
            id               = "premio_excelencia",
            titulo           = "Premio nacional",
            descripcion      = "El plantel recibe reconocimiento de excelencia educativa.",
            probabilidad     = 0.05f,
            efecto           = EventoEfecto.Ingreso(350_000L),
            requiereEdificios = 4
        )
    )

    override fun onResolveCycle(cycle: Int) {
        val edificiosComprados = PropiedadRepository.propiedades.values.count { it.comprada }
        if (edificiosComprados == 0) return
        if (Random.nextFloat() > probabilidadEvento) return

        val disponibles = eventos.filter { it.requiereEdificios <= edificiosComprados && it.efecto is EventoEfecto.Gasto } // EDU_PASO4_MAIN_V1
        if (disponibles.isEmpty()) return

        val evento = seleccionarEvento(disponibles) ?: return
        aplicarEfecto(evento)
        onEvento(evento)
    }

    private fun seleccionarEvento(disponibles: List<GameEvent>): GameEvent? {
        val totalPeso = disponibles.sumOf { it.probabilidad.toDouble() }.toFloat()
        var aleatorio = Random.nextFloat() * totalPeso
        return disponibles.firstOrNull { evento ->
            aleatorio -= evento.probabilidad
            aleatorio <= 0f
        } ?: disponibles.last()
    }

    private fun aplicarEfecto(evento: GameEvent) {
        when (val efecto = evento.efecto) {
            is EventoEfecto.Gasto   -> GameState.gastar(efecto.cantidad)
            is EventoEfecto.Ingreso -> Unit // Deshabilitado: ingreso solo por retos/mininiveles
        }
    }
}
