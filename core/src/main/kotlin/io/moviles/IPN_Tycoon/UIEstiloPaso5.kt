package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.VisUI

/** Paleta azul petróleo, bordes turquesa y botones táctiles (sin bibliotecas adicionales). */
object UIEstiloPaso5 {
    private val texturas = mutableMapOf<String, Texture>()
    private val drawables = mutableMapOf<String, Drawable>()
    val celeste = Color.valueOf("54D9F4")
    val oro = Color.valueOf("FFD369")
    val verde = Color.valueOf("6FE89B")
    val rojo = Color.valueOf("FF687D")
    val texto = Color.valueOf("ECF5FF")

    private fun parche(clave: String, fondo: Color, borde: Color): Drawable {
        return drawables.getOrPut(clave) {
            val p = Pixmap(40, 40, Pixmap.Format.RGBA8888)
            fun redondeado(color: Color, margen: Int) {
                val radio = 10 - margen
                p.setColor(color)
                p.fillRectangle(margen + radio, margen, 40 - 2 * (margen + radio), 40 - margen * 2)
                p.fillRectangle(margen, margen + radio, 40 - margen * 2, 40 - 2 * (margen + radio))
                for (x in intArrayOf(margen + radio, 39 - margen - radio))
                    for (y in intArrayOf(margen + radio, 39 - margen - radio)) p.fillCircle(x, y, radio)
            }
            redondeado(borde, 0)
            redondeado(fondo, 2)
            val texture = Texture(p)
            p.dispose()
            texturas[clave] = texture
            val corte = if (clave.startsWith("barra")) 5 else 13
            NinePatchDrawable(NinePatch(texture, corte, corte, corte, corte))
        }
    }
    val panel: Drawable get() = parche("panel", Color.valueOf("0D253F"), Color.valueOf("2677A5"))
    val panelSecundario: Drawable get() = parche("panel2", Color.valueOf("123351"), Color.valueOf("265875"))
    val botonNormal: Drawable get() = parche("botonNormal", Color.valueOf("11669A"), Color.valueOf("49BDE2"))
    val botonPresionado: Drawable get() = parche("botonPresionado", Color.valueOf("08416D"), Color.valueOf("F5CA65"))
    val botonVerde: Drawable get() = parche("botonVerde", Color.valueOf("15695C"), Color.valueOf("64E3B3"))
    val botonDeshabilitado: Drawable get() = parche("botonOff", Color.valueOf("303F51"), Color.valueOf("526071"))
    val barraFondo: Drawable get() = parche("barraFondo", Color.valueOf("091A2F"), Color.valueOf("236187"))
    val barraVerde: Drawable get() = parche("barraVerde", Color.valueOf("3CCF69"), Color.valueOf("78F39C"))
    val barraRoja: Drawable get() = parche("barraRoja", Color.valueOf("E63D54"), Color.valueOf("FF8797"))
    val barraDorada: Drawable get() = parche("barraDorada", Color.valueOf("DCA639"), Color.valueOf("FFE48D"))

    fun etiqueta(valor: String, color: Color = texto): Label {
        val font = VisUI.getSkin().get(TextButton.TextButtonStyle::class.java).font
        return Label(valor, Label.LabelStyle(font, color))
    }

    fun boton(valor: String, positivo: Boolean = false): TextButton {
        val fuente = VisUI.getSkin().get(TextButton.TextButtonStyle::class.java).font
        val estilo = TextButton.TextButtonStyle().apply {
            up = if (positivo) botonVerde else botonNormal
            down = botonPresionado
            disabled = botonDeshabilitado
            font = fuente
            fontColor = texto
            disabledFontColor = Color.LIGHT_GRAY
        }
        return TextButton(valor, estilo)
    }

    // HOTFIX_PASO5_V3_BARRAS: Drawables de 6 píxeles de altura real.
    // Las barras anteriores reutilizaban un NinePatch de 40 px; la ProgressBar
    // lo dibujaba por encima de la tarjeta incluso con Cell.height(12).
    private fun barraCompacta(clave: String, color: Color): Drawable {
        return drawables.getOrPut("compacta:$clave") {
            val p = Pixmap(28, 6, Pixmap.Format.RGBA8888)
            p.setColor(color)
            p.fillRectangle(0, 1, 28, 4)
            p.fillRectangle(2, 0, 24, 6)
            val textura = Texture(p)
            textura.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            p.dispose()
            texturas["compacta:$clave"] = textura
            TextureRegionDrawable(TextureRegion(textura))
        }
    }

    fun barra(max: Float, actual: Float, tipo: String): ProgressBar {
        val tono = when (tipo) {
            "salud" -> Color.valueOf("E63D54")
            "xp" -> Color.valueOf("EBC35A")
            else -> Color.valueOf("49D77E")
        }
        val style = ProgressBar.ProgressBarStyle(
            barraCompacta("fondo", Color.valueOf("102F44")), null
        ).apply { knobBefore = barraCompacta(tipo, tono) }
        return ProgressBar(0f, max.coerceAtLeast(1f), 1f, false, style).apply {
            value = actual.coerceIn(0f, max)
            setAnimateDuration(0.16f)
        }
    }

    fun icono(ruta: String): Image? {
        val archivo = Gdx.files.internal(ruta)
        if (!archivo.exists()) return null
        val texture = texturas.getOrPut("imagen:" + ruta) { Texture(archivo) }
        return Image(TextureRegionDrawable(TextureRegion(texture)))
    }

    /** El GameScreen llama a dispose al finalizar; las ventanas comparten estos recursos. */
    fun dispose() {
        texturas.values.forEach { it.dispose() }
        texturas.clear()
        drawables.clear()
    }
}
