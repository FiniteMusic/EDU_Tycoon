package io.moviles.IPN_Tycoon

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

/** HOTFIX_PASO5_V3_HUD: barra fina dentro de cada tarjeta, sin solapamientos. */
object HUDTematico {
    data class Componentes(
        val raiz: Table,
        val energia: ProgressBar,
        val salud: ProgressBar,
        val xp: ProgressBar
    )

    fun crear(
        dinero: Label, alumnos: Label, conocimiento: Label,
        energiaTexto: Label, saludTexto: Label,
        alMenu: () -> Unit
    ): Componentes {
        val energiaBar = UIEstiloPaso5.barra(20f, ProgresoAdministrador.energia.toFloat(), "energia")
        val saludBar = UIEstiloPaso5.barra(100f, ProgresoAdministrador.salud.toFloat(), "salud")
        val xpBar = UIEstiloPaso5.barra(
            ProgresoAdministrador.xpSiguienteNivel.toFloat(),
            ProgresoAdministrador.xpNivel.toFloat(), "xp"
        )

        fun tarjeta(
            titulo: String, icono: String, valor: Label,
            barra: ProgressBar?, ancho: Float
        ): Table {
            val tarjeta = Table().apply {
                background = UIEstiloPaso5.panel
                pad(4f)
            }
            UIEstiloPaso5.icono("ui/hud/$icono.png")?.let {
                tarjeta.add(it).size(27f).padRight(4f).center()
            }
            val contenido = Table()
            val rotulo = UIEstiloPaso5.etiqueta(titulo, UIEstiloPaso5.celeste).apply {
                setFontScale(0.70f)
            }
            valor.setFontScale(0.78f)
            contenido.add(rotulo).left().height(13f).row()
            contenido.add(valor).left().height(16f).row()
            if (barra != null) {
                contenido.add(barra).width(ancho - 43f).height(6f)
                    .padTop(2f).left().row()
            }
            tarjeta.add(contenido).left().expandX()
            return tarjeta
        }

        val fila = Table()
        fila.add(tarjeta("ENERGÍA", "energia", energiaTexto, energiaBar, 130f))
            .width(130f).height(50f).padRight(4f)
        fila.add(tarjeta("SALUD", "salud", saludTexto, saludBar, 126f))
            .width(126f).height(50f).padRight(4f)
        fila.add(tarjeta("DINERO", "monedas", dinero, null, 119f))
            .width(119f).height(50f).padRight(4f)
        fila.add(tarjeta("ALUMNOS", "alumnos", alumnos, null, 122f))
            .width(122f).height(50f).padRight(4f)
        fila.add(tarjeta("CONOCIMIENTO", "conocimiento", conocimiento, xpBar, 172f))
            .width(172f).height(50f).padRight(4f)
        val menu = UIEstiloPaso5.boton("MENÚ").apply { label.setFontScale(0.82f) }
        menu.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) = alMenu()
        })
        fila.add(menu).size(48f, 44f)

        val raiz = Table().apply {
            setFillParent(true)
            top()
            add(fila).padTop(3f).center()
        }
        return Componentes(raiz, energiaBar, saludBar, xpBar)
    }
}
