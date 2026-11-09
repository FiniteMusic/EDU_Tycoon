package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import ktx.scene2d.Scene2DSkin

/**
 * Interiores sobre el campus, sin fondo negro. La animación pertenece solo
 * al panel de imagen; la transparencia del edificio la controla el renderer.
 * No mantiene imágenes cargadas después de cerrar el panel.
 */
class InteriorOverlay(
    private val stage: Stage,
    private val propiedad: Propiedad,
    private val alAdministrar: (Propiedad) -> Unit,
    private val alCerrar: () -> Unit
) {
    val propiedadId: String get() = propiedad.id
    private var raiz: Table? = null
    private var textura: Texture? = null
    private var cerrado = false

    /** Devuelve false sin mostrar nada si no hay interior (p. ej., Palapas). */
    fun mostrar(): Boolean {
        if (cerrado || raiz != null) return false
        val ruta = InteriorAssets.buscar(propiedad) ?: return false
        val archivo = Gdx.files.internal(ruta)
        if (!archivo.exists()) {
            Gdx.app.error("EDU_INTERIOR", "PNG del interior no encontrado: $ruta")
            return false
        }
        val cargada = try {
            Texture(archivo).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        } catch (e: Exception) {
            Gdx.app.error("EDU_INTERIOR", "No se pudo cargar $ruta", e)
            return false
        }
        textura = cargada
        val skin = Scene2DSkin.defaultSkin
        val root = Table().apply {
            setFillParent(true)
            touchable = Touchable.enabled
            // Ningún background: el campus sigue visible alrededor y debajo.
            addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float,
                                       pointer: Int, button: Int): Boolean = true
            })
        }
        val cabecera = Table().apply {
            background = skin.newDrawable("white", Color(0.07f, 0.11f, 0.15f, 0.86f))
            pad(7f)
        }
        cabecera.add(Label("${propiedad.nombre} — interior nivel ${propiedad.nivel}", skin))
            .expandX().left().padLeft(8f)
        val gestionar = TextButton("GESTIONAR / MEJORAR", skin)
        gestionar.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                cerrar()
                alAdministrar(propiedad)
            }
        })
        cabecera.add(gestionar).width(175f).height(42f).padRight(7f)
        val salir = TextButton("VOLVER", skin)
        salir.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { cerrar() }
        })
        cabecera.add(salir).width(100f).height(42f)

        val panel = Table().apply {
            touchable = Touchable.childrenOnly
            setTransform(true)
        }
        panel.add(cabecera).growX().height(57f).row()
        panel.add(Image(cargada).apply {
            setScaling(Scaling.fit)
            setAlign(Align.center)
        }).grow().padTop(6f)

        // El panel deja visible el mapa en el perímetro y no usa un rectángulo negro.
        val ancho = stage.viewport.worldWidth
        val alto = stage.viewport.worldHeight
        root.add(panel).width(ancho * 0.80f).height(alto * 0.82f)
        raiz = root
        stage.addActor(root)
        root.toFront()

        panel.setOrigin(Align.center)
        panel.setScale(0.82f)
        panel.color.a = 0f
        panel.addAction(Actions.parallel(
            Actions.fadeIn(0.28f, Interpolation.fade),
            Actions.scaleTo(1f, 1f, 0.28f, Interpolation.pow2Out)
        ))
        Gdx.app.log("EDU_INTERIOR", "ABIERTO ${propiedad.id}, nivel=${propiedad.nivel}")
        return true
    }

    fun cerrar() {
        if (cerrado) return
        cerrado = true
        raiz?.remove()
        raiz = null
        textura?.dispose()
        textura = null
        alCerrar()
    }
}

/** Mantiene las rutas y prefijos de interiores del paso 2. */
object InteriorAssets {
    private const val MANIFIESTO = "Mapa/interiores_manifest.txt"
    private val rutas: Map<String, String> by lazy {
        val archivo = Gdx.files.internal(MANIFIESTO)
        if (!archivo.exists()) return@lazy emptyMap()
        val resultado = mutableMapOf<String, String>()
        try {
            archivo.readString("UTF-8").lineSequence().forEach { linea ->
                val limpia = linea.trim()
                if (limpia.isBlank() || limpia.startsWith("#")) return@forEach
                val partes = limpia.split('|', limit = 3)
                if (partes.size == 3) {
                    resultado["${partes[0].trim()}|${partes[1].trim()}"] =
                        partes[2].trim().replace('\\', '/')
                }
            }
        } catch (e: Exception) {
            Gdx.app.error("EDU_INTERIOR", "No se pudo leer el manifiesto", e)
        }
        resultado
    }
    fun buscar(propiedad: Propiedad): String? {
        if (!propiedad.comprada || propiedad.nivel < 1) return null
        rutas["${propiedad.id}|${propiedad.nivel}"]?.let { return it }
        val prefijo = propiedad.interiorTexturePrefix ?: return null
        return "Mapa/Edificios/${prefijo}lvl${propiedad.nivel}.png"
    }
}
