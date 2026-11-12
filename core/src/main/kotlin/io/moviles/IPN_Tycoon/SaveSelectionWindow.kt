package io.moviles.IPN_Tycoon

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onChange
import ktx.scene2d.*
import java.text.SimpleDateFormat
import java.util.*

class SaveSelectionWindow(
    private val game: Main,
    private val onSaveComplete: (Boolean) -> Unit
) : VisWindow("Seleccionar Slot para Guardar") {

    init {
        addCloseButton()
        closeOnEscape()
        isModal = true

        loadSlotsAndBuild()
    }

    private fun loadSlotsAndBuild() {
        game.saveManager.cargarSlots { s1, s2, s3 ->
            buildUI(s1, s2, s3)
        }
    }

    private fun buildUI(s1: GameSaveData?, s2: GameSaveData?, s3: GameSaveData?) {
        // EDU_FIX_GUARDADO_CIERRE_V1: el botón X se añade una sola vez en init.
        // No vaciar la ventana: clearChildren() destruía parte del encabezado.
        add(scene2d.table {
            defaults().pad(10f)

            // Slot 1
            addSlotUI(this, 1, s1)
            // Slot 2
            addSlotUI(this, 2, s2)
            // Slot 3
            addSlotUI(this, 3, s3)

            row()
            if (s1 != null && s2 != null && s3 != null) {
                label("Los tres slots están ocupados. Regresa al menú sin modificar tus partidas.") {
                    color = Color.LIGHT_GRAY
                    setWrap(true)
                    setAlignment(com.badlogic.gdx.utils.Align.center)
                }.cell(colspan = 3, width = 510f, padTop = 10f)
                row()
            }

            // Siempre disponible, incluso si no queda ningún slot libre.
            textButton("CERRAR / VOLVER") {
                onChange {
                    this@SaveSelectionWindow.remove()
                }
            }.cell(colspan = 3, fillX = true, padTop = 12f, height = 42f)

        }).pad(10f)

        pack()
        centerWindow()
    }

    private fun addSlotUI(table: KTableWidget, slotNum: Int, data: GameSaveData?) {
        table.table {
            val isOccupied = data != null

            label("SLOT $slotNum") { color = Color.GOLD }.cell(padBottom = 5f)
            row()

            if (isOccupied) {
                val fecha = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    .format(Date(data!!.fechaGuardado))
                label("${data.nombreEscuela}\n${data.nombreJugador}\n$fecha") {
                    setAlignment(com.badlogic.gdx.utils.Align.center)
                }.cell(width = 180f, height = 80f)
            } else {
                label("VACÍO") {
                    color = Color.LIGHT_GRAY
                }.cell(width = 180f, height = 80f)
            }
            row()

            textButton(if (isOccupied) "OCUPADO" else "GUARDAR AQUÍ") {
                isDisabled = isOccupied
                if (!isOccupied) {
                    onChange {
                        isDisabled = true
                        setText("Guardando...")
                        game.saveManager.guardar(slotNum) { ok ->
                            if (ok) {
                                GameState.slotActual = slotNum
                                ProgresoAdministrador.guardar(slotNum) // EDU_PASO4_MAIN_V1
                                onSaveComplete(true)
                                this@SaveSelectionWindow.remove()
                            } else {
                                setText("Error")
                                isDisabled = false
                            }
                        }
                    }
                }
            }.cell(fillX = true, padTop = 10f)
        }
    }

    fun show(stage: Stage) { stage.addActor(this) }
}
