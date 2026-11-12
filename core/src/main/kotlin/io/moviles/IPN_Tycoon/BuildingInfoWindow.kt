package io.moviles.IPN_Tycoon

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisWindow

/** Gestión de edificios con diseño táctil, información clara y bloqueos por conocimiento. */
class BuildingInfoWindow(
    private val data: Propiedad,
    private val onBuildingChanged: () -> Unit,
    private val onInteriorRequested: ((Propiedad) -> Unit)? = null,
    private val onChallengeRequested: ((Propiedad) -> Unit)? = null,
    private val onRestRequested: ((Propiedad) -> Unit)? = null,
    private val onFoodRequested: ((Propiedad) -> Unit)? = null
) : VisWindow("Gestión del plantel") {

    init {
        addCloseButton()
        closeOnEscape()
        isModal = false

        val puedeSubir = !data.comprada || data.nivel < data.mejoraMax
        val costo = when {
            !data.comprada -> data.precio
            puedeSubir -> GameState.costoMejora(data)
            else -> 0L
        }
        val req = if (puedeSubir) ProgresoAdministrador.conocimientoRequerido(data) else 0
        val conocimientoSuficiente = !puedeSubir || ProgresoAdministrador.nivelConocimiento >= req

        val contenido = Table().apply { background = UIEstiloPaso5.panel; pad(16f) }
        contenido.add(UIEstiloPaso5.etiqueta(data.nombre.uppercase(), UIEstiloPaso5.oro))
            .width(425f).padBottom(7f).left().row()
        val descripcion = UIEstiloPaso5.etiqueta(data.descripcion)
        descripcion.setWrap(true)
        contenido.add(descripcion).width(425f).padBottom(12f).left().row()

        val estadisticas = Table().apply { background = UIEstiloPaso5.panelSecundario; pad(12f) }
        estadisticas.add(UIEstiloPaso5.etiqueta("CAPACIDAD", UIEstiloPaso5.celeste)).left().width(140f)
        estadisticas.add(UIEstiloPaso5.etiqueta("${data.capacidad} alumnos")).left().row()
        estadisticas.add(UIEstiloPaso5.etiqueta("EDIFICIO", UIEstiloPaso5.celeste)).left().padTop(5f)
        estadisticas.add(UIEstiloPaso5.etiqueta(if (data.comprada) "Nivel ${data.nivel} de ${data.mejoraMax}" else "Sin adquirir"))
            .left().padTop(5f).row()
        estadisticas.add(UIEstiloPaso5.etiqueta("RETOS", UIEstiloPaso5.celeste)).left().padTop(5f)
        estadisticas.add(UIEstiloPaso5.etiqueta("${ProgresoAdministrador.completados(data.id)} de 5"))
            .left().padTop(5f).row()
        estadisticas.add(UIEstiloPaso5.etiqueta("CONOCIMIENTO", UIEstiloPaso5.celeste)).left().padTop(5f)
        estadisticas.add(UIEstiloPaso5.etiqueta("Nivel ${ProgresoAdministrador.nivelConocimiento}"))
            .left().padTop(5f)
        contenido.add(estadisticas).width(425f).padBottom(12f).left().row()

        if (puedeSubir) {
            val estado = if (conocimientoSuficiente) "DESBLOQUEADA" else "BLOQUEADA"
            contenido.add(UIEstiloPaso5.etiqueta(
                "MEJORA: requiere conocimiento N$req · $estado",
                if (conocimientoSuficiente) UIEstiloPaso5.verde else UIEstiloPaso5.rojo
            )).left().width(425f).padBottom(6f).row()
            contenido.add(UIEstiloPaso5.etiqueta("Costo: \$${fmt(costo)}   |   Saldo: \$${fmt(GameState.dinero)}"))
                .left().padBottom(8f).row()
        } else {
            contenido.add(UIEstiloPaso5.etiqueta("EDIFICIO AL MÁXIMO NIVEL VISUAL", UIEstiloPaso5.verde))
                .padBottom(8f).left().row()
        }

        val subir = UIEstiloPaso5.boton(when {
            !puedeSubir -> "NIVEL MÁXIMO"
            !data.comprada -> "COMPRAR EDIFICIO"
            else -> "MEJORAR A NIVEL ${data.nivel + 1}"
        }, true)
        subir.isDisabled = !puedeSubir || !conocimientoSuficiente
        subir.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (subir.isDisabled || !puedeSubir) return
                if (!ProgresoAdministrador.puedeConstruirOMejorar(data)) return
                if (!GameState.puedeComprar(costo)) {
                    subir.setText("SALDO INSUFICIENTE")
                    return
                }
                if (!GameState.gastar(costo)) return
                if (!data.comprada) { data.comprada = true; data.nivel = 1 }
                else data.nivel++
                onBuildingChanged()
                remove()
            }
        })
        contenido.add(subir).width(425f).height(46f).padBottom(8f).row()

        fun accion(titulo: String, callback: (() -> Unit)?) {
            if (callback == null) return
            val boton = UIEstiloPaso5.boton(titulo)
            boton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    remove()
                    callback()
                }
            })
            contenido.add(boton).width(425f).height(44f).padBottom(7f).row()
        }
        if (data.comprada)
            accion("JUGAR RETO · ${ProgresoAdministrador.completados(data.id)}/5") { onChallengeRequested?.invoke(data) }
        if (data.id == "Dormitorios")
            accion("DESCANSAR · RECUPERAR ENERGÍA") { onRestRequested?.invoke(data) }
        if (data.id == "cafeteria" || data.id == "Mac_and_cheese")
            accion("COMPRAR ALIMENTOS · RECUPERAR SALUD") { onFoodRequested?.invoke(data) }
        if (data.comprada && data.nivel >= 1 && data.interiorTexturePrefix != null)
            accion("ENTRAR AL INTERIOR · NIVEL ${data.nivel}") { onInteriorRequested?.invoke(data) }

        val desplazar = ScrollPane(contenido)
        desplazar.setFadeScrollBars(false)
        add(desplazar).width(470f).height(386f)
        pack()
        centerWindow()
    }

    fun show(stage: Stage) { stage.addActor(this) }

    private fun fmt(v: Long) = when {
        v >= 1_000_000L -> "${"%.2f".format(v / 1_000_000.0)}M"
        v >= 1_000L -> "${"%.1f".format(v / 1_000.0)}K"
        else -> v.toString()
    }
}
