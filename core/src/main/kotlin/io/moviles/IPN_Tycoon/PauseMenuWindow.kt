package io.moviles.IPN_Tycoon

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onChange
import ktx.scene2d.*

class PauseMenuWindow(
    private val game: Main,
    private val onGoToMainMenu: () -> Unit
) : VisWindow("  MENÚ") {

    init {
        addCloseButton()
        closeOnEscape()
        isModal = true

        add(scene2d.table {
            defaults().expandX().fillX().pad(6f)

            val saveBtn = textButton("Guardar partida") {}
            saveBtn.onChange {
                if (GameState.slotActual == 0) {
                    SaveSelectionWindow(game) { ok ->
                        if (ok) ProgresoAdministrador.guardar(GameState.slotActual)
                        saveBtn.setText(if (ok) "Guardado correctamente" else "Error al guardar")
                    }.show(stage)
                    return@onChange
                }

                saveBtn.isDisabled = true
                saveBtn.setText("Guardando...")
                game.saveManager.guardar(GameState.slotActual) { ok ->
                    if (ok) ProgresoAdministrador.guardar(GameState.slotActual)
                    saveBtn.setText(if (ok) "Guardado correctamente" else "Error al guardar")
                    saveBtn.isDisabled = false
                }
            }
            row()

            textButton("Estadísticas") {
                onChange { StatsWindow().show(stage) }
            }
            row()

            val audioBtn = textButton(audioLabel()) {}
            audioBtn.onChange {
                GameState.musicaActiva = !GameState.musicaActiva
                audioBtn.setText(audioLabel())
            }
            row()

            textButton("Menu principal") {
                onChange {
                    this@PauseMenuWindow.remove()
                    onGoToMainMenu()
                }
            }
        }).pad(20f).minWidth(280f)

        pack()
        centerWindow()
    }

    private fun audioLabel() =
        if (GameState.musicaActiva) "Audio: ON" else "Audio: OFF"

    fun show(stage: Stage) { stage.addActor(this) }
}

// ── Ventana de estadísticas ───────────────────────────────────────────
class StatsWindow : VisWindow("Estadísticas") {

    init {
        addCloseButton()
        closeOnEscape()
        isModal = false

        val edificios     = PropiedadRepository.propiedades.values.count { it.comprada }
        val alumnos       = PropiedadRepository.propiedades.values
            .filter { it.comprada }.sumOf { it.baseAlumnos * it.nivel }

        add(scene2d.table {

            label("Dinero:") { color = Color.GOLD }.cell(padRight = 10f, padBottom = 8f)
            label("\$${fmt(GameState.dinero)}").cell(padBottom = 8f)
            row()

            label("Edificios:") { color = Color.CYAN }.cell(padRight = 10f, padBottom = 8f)
            label("$edificios comprados").cell(padBottom = 8f)
            row()

            label("Alumnos:") { color = Color.GREEN }.cell(padRight = 10f, padBottom = 8f)
            label("$alumnos").cell(padBottom = 8f)
            row()

            label("Conocimiento:") { color = Color.YELLOW }.cell(padRight = 10f, padBottom = 8f)
            label("Nivel ${ProgresoAdministrador.nivelConocimiento} (${ProgresoAdministrador.experiencia} XP)").cell(padBottom = 8f)
            row()
            label("Energía / Salud:") { color = Color.GREEN }.cell(padRight = 10f, padBottom = 8f)
            label("${ProgresoAdministrador.energia}/20 · ${ProgresoAdministrador.salud}/100").cell(padBottom = 8f)
            row()

            label("Ciclos:") { color = Color.LIGHT_GRAY }.cell(padRight = 10f, padBottom = 8f)
            label("${GameState.ciclosJugados}").cell(padBottom = 8f)
            row()

            label("Jugador:") { color = Color.LIGHT_GRAY }.cell(padRight = 10f, padBottom = 8f)
            label(GameState.nombreJugador).cell(padBottom = 8f)
            row()

        }).pad(20f).minWidth(260f)

        pack()
        centerWindow()
    }

    fun show(stage: Stage) { stage.addActor(this) }

    private fun fmt(v: Long) = when {
        v >= 1_000_000L -> "${"%.1f".format(v / 1_000_000.0)}M"
        v >= 1_000L     -> "${"%.0f".format(v / 1_000.0)}K"
        else            -> v.toString()
    }
}
