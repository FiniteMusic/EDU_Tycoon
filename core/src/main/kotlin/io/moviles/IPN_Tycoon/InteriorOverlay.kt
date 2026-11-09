package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Group
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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import ktx.scene2d.Scene2DSkin
import com.kotcrab.vis.ui.VisUI

/**
 * EDU_FIX_INTERIORES_ANCLAJE_V7.
 * El interior se revela sobre su edificio, en coordenadas del mapa. No hay
 * oscurecimiento de pantalla, redibujado del TMX ni ventanas flotantes.
 */
class InteriorOverlay(
    private val stage: Stage,
    private val propiedad: Propiedad,
    private val camera: OrthographicCamera,
    private val edificioCentroX: Float,
    private val edificioCentroY: Float,
    private val edificioAnchoMundo: Float,
    private val edificioAltoMundo: Float,
    private val rotacionExterior: Float,
    private val reflejoExteriorX: Boolean,
    private val reflejoExteriorY: Boolean,
    private val alAdministrar: (Propiedad) -> Unit,
    private val alCerrar: () -> Unit
) {
    val propiedadId: String get() = propiedad.id

    private var raiz: Group? = null
    private var panel: Group? = null
    private var imagen: Image? = null
    private var barra: Table? = null
    private var textura: Texture? = null
    private var cerrado = false
    private var errorApertura: String? = null
    private val esquinaInferior = Vector3()
    private val esquinaSuperior = Vector3()

    /**
     * No permitir que un recurso interior dañado cierre toda la partida.
     * La textura se genera previamente como vista previa optimizada (V5).
     */
    fun mostrar(): Boolean {
        return try {
            mostrarSeguro()
        } catch (fallo: Throwable) {
            val razon = "No pudo abrirse ${propiedad.nombre} N${propiedad.nivel}: " +
                (fallo.message ?: fallo.javaClass.simpleName)
            InteriorAssets.ultimoError = razon
            Gdx.app.error("EDU_INTERIOR", "V7 $razon", fallo)
            liberarRecursos()
            false
        }
    }

    private fun mostrarSeguro(): Boolean {
        if (cerrado || raiz != null) return false
        Gdx.app.log("EDU_INTERIOR", "V7 INICIO id=${propiedad.id} nivel=${propiedad.nivel}")
        val ruta = InteriorAssets.buscar(propiedad) ?: return false
        val archivo = Gdx.files.internal(ruta)
        if (!archivo.exists()) {
            val motivo = "La imagen del nivel ${propiedad.nivel} no está en el APK: $ruta"
            InteriorAssets.ultimoError = motivo
            Gdx.app.error("EDU_INTERIOR", motivo)
            return false
        }
        // La carpeta V5 contiene miniaturas <= 1536 px, no los PNG originales.
        // Evita picos de memoria RAM/GPU en el emulador Android.
        Gdx.app.log("EDU_INTERIOR", "V7 abriendo textura existente: $ruta")
        val recurso = Texture(archivo).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        textura = recurso

        val ajuste = InteriorOrientacion.obtener(
            propiedad.id, propiedad.nivel, rotacionExterior,
            reflejoExteriorX, reflejoExteriorY
        )
        val region = TextureRegion(recurso)
        if (ajuste.flipX || ajuste.flipY) region.flip(ajuste.flipX, ajuste.flipY)

        // NO capturar los toques de toda la pantalla: con V3 una zona
        // transparente podía bloquear la gestión si el panel salía del marco.
        // V7: dentro de Group().apply el nombre stage corresponde al Actor,
        // cuyo stage es null hasta ser agregado. Capturamos el Stage real.
        val viewportDeEscena = this.stage.viewport
        val root = Group().apply {
            setSize(viewportDeEscena.worldWidth, viewportDeEscena.worldHeight)
            touchable = Touchable.childrenOnly
        }
        val contenido = Group().apply {
            touchable = Touchable.childrenOnly
            setTransform(true)
        }
        val interior = Image(TextureRegionDrawable(region)).apply {
            // No deformar la ilustración ni alterar la perspectiva del PNG.
            setScaling(Scaling.fit)
            setAlign(Align.center)
            touchable = Touchable.disabled
        }
        contenido.addActor(interior)

        // El PNG debe verse aunque falle un estilo de botones del skin.
        // Los botones se crean como mejora no bloqueante, después de registrar
        // la imagen en la escena, de modo que no pueda ocultarse por un skin.
        root.addActor(contenido)
        raiz = root
        panel = contenido
        imagen = interior
        stage.addActor(root)
        root.toFront()
        try {
            val skin = try { VisUI.getSkin() } catch (_: Throwable) {
                Scene2DSkin.defaultSkin
            }
            val controles = Table().apply {
                pad(2f)
                touchable = Touchable.childrenOnly
            }
            controles.add(Label("INTERIOR N${propiedad.nivel}", skin)).padLeft(4f)
            val mejorar = TextButton("GESTIONAR / MEJORAR", skin)
            mejorar.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    cerrar()
                    alAdministrar(propiedad)
                }
            })
            controles.add(mejorar).expandX().fillX().padLeft(4f).padRight(3f)
            val volver = TextButton("VOLVER", skin)
            volver.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { cerrar() }
            })
            controles.add(volver).width(86f).padRight(3f)
            root.addActor(controles)
            barra = controles
        } catch (fallo: Throwable) {
            // La ilustración sigue visible: toque sobre ella para salir.
            Gdx.app.error("EDU_INTERIOR", "V6 controles no disponibles, imagen sí visible", fallo)
            interior.touchable = Touchable.enabled
            interior.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { cerrar() }
            })
        }
        actualizarPosicion()

        // Apertura desde la misma fachada; alpha del exterior viene del renderer.
        contenido.setScale(0.70f)
        contenido.color.a = 0f
        contenido.addAction(Actions.parallel(
            Actions.fadeIn(0.38f, Interpolation.fade),
            Actions.scaleTo(1f, 1f, 0.38f, Interpolation.pow2Out)
        ))
        // Imagen con la misma orientación geométrica que el exterior.
        interior.rotation = ajuste.rotacion
        Gdx.app.log(
            "EDU_INTERIOR",
            "V7 PNG EXACTO id=${propiedad.id} nivel=${propiedad.nivel} " +
                "ruta=$ruta rot=${ajuste.rotacion} flipX=${ajuste.flipX} flipY=${ajuste.flipY}"
        )
        return true
    }

    /**
     * V7: anclaje mundo -> pantalla -> Stage, sin limitar artificialmente
     * la posición del interior a los bordes de la pantalla. Eso provocaba
     * que la imagen se separara del exterior mientras la cámara se movía.
     *
     * La cámara es la del mundo. Stage tiene su propia cámara/viewport; por
     * ello primero proyectamos con el rectángulo real del viewport de Stage
     * y luego convertimos a unidades de la interfaz.
     */
    fun actualizarPosicion() {
        val contenido = panel ?: return
        val viewport = stage.viewport
        val screenWidth = viewport.screenWidth
        val screenHeight = viewport.screenHeight
        if (screenWidth <= 0 || screenHeight <= 0) return

        val stageWidth = viewport.worldWidth
        val stageHeight = viewport.worldHeight
        if (stageWidth <= 0f || stageHeight <= 0f) return
        raiz?.setSize(stageWidth, stageHeight)

        // Misma geometría centrada del sprite EXTERIOR y de su nivel.
        // edificioCentroY ya incluye media altura desde la base del edificio.
        val worldW = edificioAnchoMundo.coerceAtLeast(1f) * 0.98f
        val worldH = edificioAltoMundo.coerceAtLeast(1f) * 0.96f
        esquinaInferior.set(
            edificioCentroX - worldW * 0.5f,
            edificioCentroY - worldH * 0.5f,
            0f
        )
        esquinaSuperior.set(
            edificioCentroX + worldW * 0.5f,
            edificioCentroY + worldH * 0.5f,
            0f
        )
        val vx = viewport.screenX.toFloat()
        val vy = viewport.screenY.toFloat()
        val vw = screenWidth.toFloat()
        val vh = screenHeight.toFloat()
        camera.project(esquinaInferior, vx, vy, vw, vh)
        camera.project(esquinaSuperior, vx, vy, vw, vh)

        val xPorPixel = stageWidth / vw
        val yPorPixel = stageHeight / vh
        val centroX = ((esquinaInferior.x + esquinaSuperior.x) * 0.5f - vx) * xPorPixel
        val centroY = ((esquinaInferior.y + esquinaSuperior.y) * 0.5f - vy) * yPorPixel
        val anchoExterior = kotlin.math.abs(esquinaSuperior.x - esquinaInferior.x) * xPorPixel
        val altoExterior = kotlin.math.abs(esquinaSuperior.y - esquinaInferior.y) * yPorPixel
        if (!centroX.isFinite() || !centroY.isFinite() ||
            !anchoExterior.isFinite() || !altoExterior.isFinite()) return

        // Escala UNIFORME: las dimensiones visuales del edificio constituyen
        // una caja máxima; el PNG conserva su perspectiva y proporción real.
        val recurso = textura ?: return
        val textureW = recurso.width.coerceAtLeast(1).toFloat()
        val textureH = recurso.height.coerceAtLeast(1).toFloat()
        val factor = kotlin.math.min(anchoExterior / textureW, altoExterior / textureH)
        if (!factor.isFinite() || factor <= 0f) return
        val anchoInterior = textureW * factor
        val altoInterior = textureH * factor

        // No hacer coerceIn() del centro: un clamp desplaza la imagen respecto
        // del edificio al mover cámara o cambiar zoom. El centro siempre sigue
        // al punto real proyectado del mapa.
        contenido.setBounds(
            centroX - anchoInterior * 0.5f,
            centroY - altoInterior * 0.5f,
            anchoInterior,
            altoInterior
        )
        contenido.setOrigin(anchoInterior * 0.5f, altoInterior * 0.5f)
        imagen?.setBounds(0f, 0f, anchoInterior, altoInterior)
        imagen?.setOrigin(Align.center)

        // Barra de acciones independiente de la imagen para garantizar acceso.
        val anchoBarra = (stageWidth - 20f).coerceAtMost(455f).coerceAtLeast(180f)
        val altoBarra = 44f
        barra?.setBounds(
            (stageWidth - anchoBarra) * 0.5f,
            (stageHeight - altoBarra - 8f).coerceAtLeast(4f),
            anchoBarra,
            altoBarra
        )
        barra?.invalidate()
    }

    private fun liberarRecursos() {
        raiz?.remove()
        raiz = null
        panel = null
        imagen = null
        barra = null
        try { textura?.dispose() } catch (fallo: Throwable) {
            Gdx.app.error("EDU_INTERIOR", "Fallo liberando textura", fallo)
        }
        textura = null
    }

    fun cerrar() {
        if (cerrado) return
        cerrado = true
        liberarRecursos()
        alCerrar()
    }
}

/**
 * Garantiza que no se muestre el PNG de otro nivel. Primero intenta la ruta
 * EXACTA de Propiedad.interiorTexturePrefix; el manifiesto es respaldo y
 * debe terminar también en lvl<numero>.png. No existe fallback a lvl1.
 */
object InteriorAssets {
    private const val MANIFIESTO = "Mapa/interiores_manifest.txt"
    /** Motivo de la última ausencia/error, para mostrarlo al jugador. */
    var ultimoError: String? = null

    private val rutas: Map<String, String> by lazy {
        val archivo = Gdx.files.internal(MANIFIESTO)
        if (!archivo.exists()) return@lazy emptyMap()
        val resultado = mutableMapOf<String, String>()
        try {
            archivo.readString("UTF-8").lineSequence().forEach { linea ->
                val texto = linea.trim()
                if (texto.isEmpty() || texto.startsWith("#")) return@forEach
                val p = texto.split('|', limit = 3)
                if (p.size == 3) resultado["${p[0].trim()}|${p[1].trim()}"] =
                    p[2].trim().replace('\\', '/')
            }
        } catch (fallo: Exception) {
            Gdx.app.error("EDU_INTERIOR", "V6 manifiesto ilegible", fallo)
        }
        resultado
    }

    fun buscar(propiedad: Propiedad): String? {
        ultimoError = null
        if (!propiedad.comprada || propiedad.nivel < 1) return null
        val prefijo = propiedad.interiorTexturePrefix ?: return null // Palapas
        val nivel = propiedad.nivel
        val nombre = "${prefijo}lvl${nivel}.png"
        val candidatos = mutableListOf(
            "Mapa/Edificios/_InterioresOptimizados/$nombre",
            "Mapa/Interiores_Preview/$nombre"
        )
        rutas["${propiedad.id}|$nivel"]?.let { alternativa ->
            if (alternativa.endsWith("lvl$nivel.png", ignoreCase = true)) {
                candidatos.add("Mapa/Interiores_Preview/${alternativa.substringAfterLast('/')}")
            }
        }
        // La vista optimizada V6 se comprueba primero; V5 queda de respaldo.
        for (ruta in candidatos.distinct()) {
            if (Gdx.files.internal(ruta).exists()) {
                Gdx.app.log("EDU_INTERIOR", "V6 PNG encontrado: id=${propiedad.id} N$nivel -> $ruta")
                return ruta
            }
        }
        ultimoError = "No está empaquetada la imagen optimizada $nombre. " +
            "Reinstala el APK y verifica assets/Mapa/Edificios/_InterioresOptimizados."
        Gdx.app.error("EDU_INTERIOR", "V6 ${ultimoError ?: "recurso ausente"}")
        return null
    }
}

/**
 * Ajustes opcionales de orientación por edificio/nivel.
 * Archivo: assets/Mapa/interiores_orientacion.txt
 * Formato: id|nivel|grados|flipX|flipY
 * Usar 0/90/180/270 grados; flip horizontal invierte perspectiva lateral.
 * Una rotación de 90 grados NO genera una fachada frontal desde una lateral.
 */
object InteriorOrientacion {
    data class Ajuste(val rotacion: Float, val flipX: Boolean, val flipY: Boolean)
    private const val ARCHIVO = "Mapa/interiores_orientacion.txt"
    private val ajustes: Map<String, Ajuste> by lazy {
        val archivo = Gdx.files.internal(ARCHIVO)
        if (!archivo.exists()) return@lazy emptyMap()
        val resultado = mutableMapOf<String, Ajuste>()
        try {
            archivo.readString("UTF-8").lineSequence().forEach { linea ->
                val texto = linea.trim()
                if (texto.isEmpty() || texto.startsWith("#")) return@forEach
                val p = texto.split('|').map { it.trim() }
                if (p.size != 5) return@forEach
                val nivel = p[1].toIntOrNull() ?: return@forEach
                val grados = p[2].toFloatOrNull() ?: return@forEach
                val flipX = p[3].toBooleanStrictOrNull() ?: return@forEach
                val flipY = p[4].toBooleanStrictOrNull() ?: return@forEach
                resultado["${p[0]}|$nivel"] = Ajuste(grados, flipX, flipY)
            }
        } catch (e: Exception) {
            Gdx.app.error("EDU_INTERIOR", "No se pudo leer orientación", e)
        }
        resultado
    }

    fun obtener(
        id: String, nivel: Int, rotacionExterior: Float,
        reflejoExteriorX: Boolean, reflejoExteriorY: Boolean
    ): Ajuste = ajustes["$id|$nivel"]
        ?: Ajuste(rotacionExterior, reflejoExteriorX, reflejoExteriorY)
}
