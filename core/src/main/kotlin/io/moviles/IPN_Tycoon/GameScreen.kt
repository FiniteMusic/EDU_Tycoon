package io.moviles.IPN_Tycoon

import io.moviles.IPN_Tycoon.rendering.IsometricObjectLayerRenderer
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.input.GestureDetector.GestureAdapter
import com.badlogic.gdx.maps.objects.PointMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import io.moviles.IPN_Tycoon.engine.EconomyEngine
import io.moviles.IPN_Tycoon.engine.EstudiantesEngine
import io.moviles.IPN_Tycoon.engine.EventEngine
import io.moviles.IPN_Tycoon.engine.EventoEfecto
import io.moviles.IPN_Tycoon.engine.GameCycleEngine
import io.moviles.IPN_Tycoon.engine.GameEvent
import ktx.actors.onChange
import ktx.app.clearScreen
import ktx.assets.toInternalFile
import ktx.scene2d.*

class GameScreen(game: Main) : BaseScreen(game, ExtendViewport(800f, 480f)) {

    // EDU_PASO4_MAIN_V1: recursos audiovisuales y progreso independientes del mapa V7/V3.
    private val audioAmbiente = AudioAmbiente()
    private val caminantesCampus = CaminantesCampus()
    private val animacionesCampus = AnimacionesCampus() // EDU_ANIMACIONES_CAMPUS_V4
    private var avisoMuerteVisible = false
    private var energyLabel: Label? = null
    private var healthLabel: Label? = null
    private var energyProgress: ProgressBar? = null
    private var healthProgress: ProgressBar? = null
    private var knowledgeProgress: ProgressBar? = null
    private var lastEnergy = -1
    private var lastHealth = -1
    private var lastXp = -1
    private var tiempoDiagnosticoFPS = 0f

    // El TMX de Android NO contiene las 250,000 celdas de Fondo_pasto.
    // Se dibuja un fondo verde texturizado con una sola llamada a GPU.
    private val pastoDetallado = false
    private var fondoPastoRapido: Texture? = null
    private var fondoPastoIntentado = false
    private var camaraCentradaEnCampus = false

    private val objectLayerRenderer by lazy { map?.let { IsometricObjectLayerRenderer(it) } }

    // ── Motor de ciclos ───────────────────────────────────────────────
    private val cycleEngine       = GameCycleEngine()
    private val economyEngine     = EconomyEngine()
    private val estudiantesEngine = EstudiantesEngine()
    private val eventEngine       = EventEngine { evento -> showEventToast(evento) }
    private var cycleTimer        = 0f
    private val cycleDuration     = 30f

    // ── Ajustes de Optimización ──────────────────────────────────────
    private val maxZoom                = 7.0f
    private val virtualHeight = 480f
    private val initialCameraX = -436f
    private val initialCameraY = 1360f
    private val maxZoomForLabels       = 4.0f
    private val maxZoomForFullBuildings = 5.8f

    // Offset vertical para edificios generales al encogerse (LOD)
    private val smallBuildingYOffset = mapOf(
        "Edificio1" to 35f,
        "Edificio2" to 35f,
        "edificio1" to 35f,
        "edificio2" to 35f
    )

    init {
        cycleEngine.addListener(economyEngine)
        cycleEngine.addListener(estudiantesEngine)
        cycleEngine.addListener(eventEngine)
    }

    // ── Mapa ──────────────────────────────────────────────────────────

    private val map: TiledMap? by lazy {
        val ruta = "Mapa/Mapa_Android_Optimizado.tmx"

        try {
            val archivo = Gdx.files.internal(ruta)

            Gdx.app.log(
                "MAP_DEBUG",
                "Existe el archivo TMX: ${archivo.exists()}"
            )

            val mapaCargado = TmxMapLoader().load(ruta)

            Gdx.app.log(
                "MAP_DEBUG",
                "MAPA CARGADO CORRECTAMENTE"
            )

            Gdx.app.log(
                "MAP_DEBUG",
                "Numero de capas: ${mapaCargado.layers.count}"
            )

            for (i in 0 until mapaCargado.layers.count) {
                val capa = mapaCargado.layers[i]

                Gdx.app.log(
                    "MAP_DEBUG",
                    "Capa: ${capa.name} - Visible: ${capa.isVisible}"
                )
            }

            mapaCargado

        } catch (e: Exception) {
            Gdx.app.error(
                "MAP_DEBUG",
                "ERROR AL CARGAR EL MAPA",
                e
            )
            null
        }
    }

    private val renderer: IsometricTiledMapRenderer? by lazy {
        map?.let { IsometricTiledMapRenderer(it) }
    }

    private val layerIndicesToRender: IntArray by lazy {
        val m = map ?: return@lazy intArrayOf()
        (0 until m.layers.count)
            .filter { m.layers[it] is com.badlogic.gdx.maps.tiled.TiledMapTileLayer }
            .toIntArray()
    }

    // ── Cámara ────────────────────────────────────────────────────────
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, 800f, 480f)
        zoom = 5f
        position.set(-436f, 1360f, 0f)
    }
    private var initialZoom = 5f
    private val targetCameraPos = Vector3(-436f, 1360f, 0f)

    // ── OPTIMIZACIÓN: Screen-space rect para culling 2D barato ────────
    // Reusable para no generar GC cada frame
    private val screenRect = Rectangle()
    private val tempVec2   = Vector2()

    /**
     * Actualiza screenRect con los límites del mundo visibles en pantalla.
     * Mucho más rápido que frustum.boundsInFrustum para sprites 2D isométricos.
     * Se llama una vez por frame en actualizarCamara().
     */
    private fun updateScreenRect() {
        val halfW = camera.viewportWidth  * camera.zoom * 0.5f
        val halfH = camera.viewportHeight * camera.zoom * 0.5f

        val margin = when {
            camera.zoom > 6f -> 60f
            camera.zoom > 4f -> 100f
            else             -> 140f
        } * camera.zoom

        screenRect.set(
            camera.position.x - halfW - margin,
            camera.position.y - halfH - margin,
            (halfW + margin) * 2f,
            (halfH + margin) * 2f
        )
    }

    /** Culling 2D: más rápido que frustum 3D para sprites isométricos */
    private fun isVisible(worldX: Float, worldY: Float, w: Float, h: Float): Boolean {
        return screenRect.overlaps(
            // Reutilizar un Rectangle temporal para evitar allocations
            tempRect.also { it.set(worldX - w * 0.5f, worldY - h * 0.5f, w, h) }
        )
    }
    private val tempRect = Rectangle()

    // ── Caché de texturas de edificios ────────────────────────────────
    private val buildingTextureCache = mutableMapOf<String, Texture?>()
    private val missingBuildingTextures = mutableSetOf<String>()

    // OPTIMIZACIÓN: Pre-resolvemos la textura en BuildingRenderInfo para
    // no llamar a getBuildingTexture() cada frame (evita hashmap lookup + lambda)
    // Índice de los archivos reales de Android assets: se crea una sola vez.
    // Android distingue mayúsculas/minúsculas, a diferencia de Windows.
    private fun clavePNG(valor: String): String =
        java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")

    private val indicesPNG: Map<String, com.badlogic.gdx.files.FileHandle> by lazy {
        val carpeta = Gdx.files.internal("Mapa/Edificios")
        val archivos = carpeta.list().filter {
            it.extension().equals("png", ignoreCase = true)
        }
        Gdx.app.log("TEXTURE", "PNG de edificios detectados en assets: ${archivos.size}")
        archivos.associateBy { clavePNG(it.nameWithoutExtension()) }
    }

    private fun getBuildingTexture(propiedad: Propiedad): Texture? {
        val prefix = propiedad.texturePrefix ?: return null
        val key = "${prefix}lvl${propiedad.nivel}"
        if (buildingTextureCache.containsKey(key)) return buildingTextureCache[key]

        val exacto = Gdx.files.internal("Mapa/Edificios/$key.png")
        val frontal = if (propiedad.id == "Biblioteca" && propiedad.nivel == 3)
            Gdx.files.internal("Mapa/Edificios/Bibliotecalvl3_frente.png") else null
        val archivo = if (frontal != null && frontal.exists()) frontal
            else if (exacto.exists()) exacto
            else indicesPNG[clavePNG(key)] ?: run {
                val base = Gdx.files.internal("Mapa/Edificios/${prefix}lvl1.png")
                if (propiedad.nivel > 1 && base.exists()) {
                    Gdx.app.error("EDU_TILED_NIVELES", "Falta fachada N${propiedad.nivel} " +
                        "para ${propiedad.id}: $key.png; provisionalmente se muestra N1")
                    base
                } else null
            }
        val tex = try {
            if (archivo != null && archivo.exists()) {
                Texture(archivo).apply {
                    setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
            } else null
        } catch (e: Exception) {
            Gdx.app.error("TEXTURE", "No se pudo abrir el PNG de ${propiedad.id}", e)
            null
        }
        if (tex == null && missingBuildingTextures.add(key)) {
            Gdx.app.error("TEXTURE", "FALTA el PNG para ${propiedad.id}: $key.png " +
                "(nivel ${propiedad.nivel}); comprueba assets/Mapa/Edificios")
        }
        buildingTextureCache[key] = tex
        return tex
    }

    private val worldFont: BitmapFont by lazy {
        val skinFont = Scene2DSkin.defaultSkin.getFont("default-font")
        val fontData = com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData(
            skinFont.data.fontFile,
            skinFont.data.flipped
        )
        BitmapFont(fontData, skinFont.regions, false).apply {
            data.setScale(4.5f)
        }
    }

    private val labelBgTexture: Texture by lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(0f, 0f, 0f, 0.6f))
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        tex.apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
    }

    // ── Diálogo ───────────────────────────────────────────────────────
    private var dialogoActor: DialogoActor? = null
    private var currentInfoWindow: BuildingInfoWindow? = null

    // EDU_DISENO_UNIFICADO_V3: interior integrado, seguimiento de cámara y
    // gestión disponible para edificios sin PNG de interior (p. ej. Palapas).
    private var interiorAbierto: InteriorOverlay? = null
    private var camaraAnteriorInterior: Vector3? = null
    private var zoomAnteriorInterior: Float? = null
    private var zoomObjetivoInterior: Float? = null

    private fun restaurarCamaraAnteriorInterior() {
        camaraAnteriorInterior?.let { targetCameraPos.set(it) }
        zoomAnteriorInterior?.let {
            zoomObjetivoInterior = it
            initialZoom = it
        }
        camaraAnteriorInterior = null
        zoomAnteriorInterior = null
    }

    // EDU_FIX_INTERIORES_VISIBLES_V6: diagnóstico visible, sin cierre ni
    // deshabilitar compras. Palapas nunca entra aquí desde la gestión.
    private fun avisarInteriorNoDisponible(mensaje: String) {
        try {
            val aviso = com.kotcrab.vis.ui.widget.VisWindow("Interior no disponible")
            aviso.addCloseButton()
            val texto = com.kotcrab.vis.ui.widget.VisLabel(mensaje)
            texto.setWrap(true)
            aviso.add(texto).width(360f).pad(18f)
            aviso.pack()
            aviso.centerWindow()
            stage.addActor(aviso)
            aviso.toFront()
        } catch (fallo: Throwable) {
            Gdx.app.error("EDU_INTERIOR", "V6 no pudo mostrar el aviso: $mensaje", fallo)
        }
    }

    // EDU_FIX_INTERIORES_ANCLAJE_V7: encuadre con margen para controles.
    private fun abrirInterior(propiedad: Propiedad) {
        if (!modoCarga || !propiedad.comprada || propiedad.nivel < 1) return
        val ruta = InteriorAssets.buscar(propiedad)
        if (ruta == null || !Gdx.files.internal(ruta).exists()) {
            Gdx.app.log("EDU_INTERIOR", "V4 sin PNG interior: id=${propiedad.id} nivel=${propiedad.nivel}")
            // No desactivar la mejora: solo avisar del PNG faltante.
            mostrarGestion(propiedad)
            if (propiedad.interiorTexturePrefix != null) {
                avisarInteriorNoDisponible(InteriorAssets.ultimoError ?: "Falta el PNG de nivel ${propiedad.nivel}.")
            }
            return
        }
        val entry = renderEntries.firstOrNull { it.propiedad.id == propiedad.id }
        if (entry == null) {
            mostrarGestion(propiedad)
            return
        }
        if (interiorAbierto != null) cerrarInterior()
        val geometria = geometriaEdificio(entry)
        val centroX = entry.bWorldX + geometria.desplazamientoX
        val centroY = entry.bWorldY + geometria.desplazamientoY + geometria.alto * 0.5f
        val nuevo = InteriorOverlay(
            stage = stage,
            propiedad = propiedad,
            camera = camera,
            edificioCentroX = centroX,
            edificioCentroY = centroY,
            edificioAnchoMundo = geometria.ancho,
            edificioAltoMundo = geometria.alto,
            rotacionExterior = geometria.rotacion ?: entry.bRotation,
            reflejoExteriorX = geometria.reflejoX ?: entry.bFlipX,
            reflejoExteriorY = geometria.reflejoY ?: entry.bFlipY,
            alReto = { mostrarMinijuego(it) },
            alAdministrar = { mostrarGestion(it) },
            alCerrar = {
                objectLayerRenderer?.setInteriorFocus(null)
                interiorAbierto = null
                restaurarCamaraAnteriorInterior()
                audioAmbiente.campus()
            }
        )
        // EDU_FIX_INTERIORES_CRASH_V5: si falla cualquier recurso, conservar
        // la pantalla de gestion, los niveles y la partida actual.
        val interiorVisible = try {
            nuevo.mostrar()
        } catch (fallo: Throwable) {
            Gdx.app.error("EDU_INTERIOR", "V5 fallo de apertura: ${propiedad.id} N${propiedad.nivel}", fallo)
            false
        }
        if (!interiorVisible) {
            Gdx.app.log("EDU_INTERIOR", "V6 apertura fallida; se conserva gestión")
            mostrarGestion(propiedad)
            avisarInteriorNoDisponible(InteriorAssets.ultimoError ?:
                "Falló la creación del interior. Consulta Logcat con filtro EDU_INTERIOR.")
            return
        }
        camaraAnteriorInterior = Vector3(targetCameraPos)
        zoomAnteriorInterior = camera.zoom
        targetCameraPos.set(centroX, centroY, 0f)
        zoomObjetivoInterior = kotlin.math.max(
            geometria.ancho / (camera.viewportWidth * 0.84f),
            geometria.alto / (camera.viewportHeight * 0.73f)
        ).coerceIn(1f, 14f)
        currentInfoWindow?.remove()
        currentInfoWindow = null
        interiorAbierto = nuevo
        objectLayerRenderer?.setInteriorFocus(propiedad.id)
        audioAmbiente.interior(propiedad)
    }

    private fun cerrarInterior(): Boolean {
        val actual = interiorAbierto ?: return false
        actual.cerrar() // Su callback restaura cámara y opacidad.
        return true
    }

    private fun mostrarGestion(propiedad: Propiedad) {
        currentInfoWindow?.remove()
        currentInfoWindow = BuildingInfoWindow(
            propiedad,
            onBuildingChanged = {
                hudDirty = true
                invalidateRenderEntry(propiedad.id)
                Gdx.app.log("GAME", "${propiedad.nombre} → nivel ${propiedad.nivel}")
                // Reabrir la ficha después de que BuildingInfoWindow retire
                // la anterior. El siguiente nivel vuelve a estar disponible.
                Gdx.app.postRunnable {
                    if (modoCarga && interiorAbierto == null) mostrarGestion(propiedad)
                }
            },
            onInteriorRequested = { abrirInterior(it) },
            onChallengeRequested = { mostrarMinijuego(it) },
            onRestRequested = { VentanaDescanso { hudDirty = true }.mostrarEn(stage) },
            onFoodRequested = {
                VentanaComida(it.id == "cafeteria") { hudDirty = true }.mostrarEn(stage)
            }
        )
        currentInfoWindow?.show(stage)
    }

    private val backgroundTexture: Texture by lazy {
        Texture("background.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }
    private var backgroundImage: Image? = null

    private fun mostrarMinijuego(propiedad: Propiedad) {
        MinijuegoWindow(propiedad) { hudDirty = true }.mostrarEn(stage)
    }

    private fun mostrarFinDePartida() {
        if (avisoMuerteVisible) return
        avisoMuerteVisible = true
        cerrarInterior()
        val skin = Scene2DSkin.defaultSkin
        val dialogo = com.kotcrab.vis.ui.widget.VisWindow("Fin de la partida")
        dialogo.isModal = true
        dialogo.add(Label("La salud del administrador llegó a 0. La partida se reiniciará.", skin))
            .width(380f).pad(16f).row()
        val reiniciar = com.badlogic.gdx.scenes.scene2d.ui.TextButton("REINICIAR", skin)
        reiniciar.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                dialogo.remove()
                avisoMuerteVisible = false
                val slot = GameState.slotActual
                val terminar = {
                    ProgresoAdministrador.borrar(slot)
                    GameState.reset(GameState.presupuestoNuevaPartida())
                    PropiedadRepository.resetProgress()
                    modoCarga = false
                    game.setScreen<SeleccionPartida>()
                }
                if (slot in 1..3) game.saveManager.eliminarSlot(slot) { terminar() }
                else terminar()
            }
        })
        dialogo.add(reiniciar).width(230f).pad(12f)
        dialogo.pack()
        dialogo.centerWindow()
        stage.addActor(dialogo)
        dialogo.toFront()
    }

    // ── Icono Menú ────────────────────────────────────────────────────
    private val menuIconTexture: Texture by lazy {
        Texture("menuicon.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────
    private var moneyLabel:       Label? = null
    private var alumnosLabel:     Label? = null
    private var knowledgeLabel:   Label? = null
    private var lastMoney:        Long = -1
    private var lastStudents:     Int = -1
    private var lastKnowledge:   Int = -1
    private var toastLine1:       Label? = null
    private var toastLine2:       Label? = null
    private var toastTable:       Table? = null
    private var eventTituloLabel: Label? = null
    private var eventEfectoLabel: Label? = null
    private var eventTable:       Table? = null

    // ── Estado ────────────────────────────────────────────────────────
    var modoCarga: Boolean = false
    private var tutorialHighlightPos: Vector3? = null
    private var tutorialTimer = 0f
    private val highlightTexture: Texture by lazy {
        val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(1f, 1f, 1f, 1f))
        pixmap.fillCircle(32, 32, 30)
        Texture(pixmap).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }.also { pixmap.dispose() }
    }

    // ── OPTIMIZACIÓN: Datos de renderizado pre-calculados ─────────────
    /**
     * Combina edificio + etiqueta en un solo objeto para:
     * 1. Iterar la lista UNA sola vez (antes eran 3 pasadas separadas)
     * 2. Evitar búsquedas repetidas por propiedad
     * 3. Almacenar la textura resuelta para no hacer hashmap lookup cada frame
     */
    private data class RenderEntry(
        val propiedad:   Propiedad,
        // Coordenadas del Edificio (Building)
        val bWorldX:     Float,
        val bWorldY:     Float,
        val bDrawX:      Float,   // Anclaje izquierdo del sprite
        val bWidth:      Float,   // Dimensiones visuales definidas en Tiled
        val bHeight:     Float,
        val bRotation:   Float,   // Rotación aplicada por el renderizador de objetos
        val bFlipX:      Boolean,
        val bFlipY:      Boolean,
        val bSortY:      Float,
        val bSortIndex:  Int,
        // Coordenadas de la Etiqueta (Label)
        val lWorldX:     Float,
        val lWorldY:     Float,
        val nameLayout:  GlyphLayout?,
        val nameBgW:     Float,
        val nameBgH:     Float,
        // Posición del texto centrado
        val nameTextX:   Float,
        val nameTextY:   Float,
        // Textura resuelta (se invalida al subir de nivel)
        var cachedTex:   Texture?
    )
    private val renderEntries = mutableListOf<RenderEntry>()

    // OPTIMIZACIÓN: Acumuladores pre-calculados para HUD — solo se recalculan
    // cuando cambia el estado de una propiedad, no en cada frame
    private var cachedTotalAlumnos:   Int = 0
    private var cachedTotalMaxLevel:  Int = 0
    private var cachedCurrentLevel:   Int = 0
    private var hudDirty = true   // Marca que hay que recalcular los acumuladores

    // ── Mapa auxiliar punto → propiedad ───────────────────────────────
    private val puntosAPropiedad = mapOf(
        "escom"          to "escom_hitbox",
        "escom_hitbox"   to "escom_hitbox",
        "ESCOM"          to "escom_hitbox",
        "Computacion"    to "escom_hitbox",
        "computacion"    to "escom_hitbox",
        "Direccion"      to "Direccion",
        "direccion"      to "Direccion",
        "Mac_and_cheese" to "Mac_and_cheese",
        "cafeteria"      to "cafeteria",
        "Cafeteria"      to "cafeteria",
        "auditorio"      to "auditorio",
        "Auditorio"      to "auditorio",
        "Arquitectura"   to "Arquitectura",
        "arquitectura"   to "Arquitectura",
        "Biologicas"     to "Biologicas",
        "biologicas"     to "Biologicas",
        "Bioquimica"     to "Bioquimica",
        "bioquimica"     to "Bioquimica",
        "Matematicas"    to "Matematicas",
        "matematicas"    to "Matematicas",
        "Edificio1"      to "Edificio1",
        "edificio1"      to "Edificio1",
        "Edificio2"      to "Edificio2",
        "edificio2"      to "Edificio2",
        "Museo"          to "Museo",
        "museo"          to "Museo",
        "Turismo"        to "Turismo",
        "turismo"        to "Turismo",
        "Palapas"        to "palapas",
        "palapas"        to "palapas",
        "Computo" to "escom_hitbox",
        "Restaurante" to "cafeteria",
        "Comida" to "Mac_and_cheese",
        "comida" to "Mac_and_cheese",
        "medicina" to "Edificio1",
        "Medicina" to "Edificio1",
        "Consultorio" to "Edificio2",
        "Biblioteca" to "Biblioteca",
        "Dormitorios" to "Dormitorios",
        "Gastronomia" to "Gastronomia",
        "Arte_Diseño_Deporte" to "Arte_diseno_deporte",
        "Arte_diseño_deporte" to "Arte_diseno_deporte",
        "arte_diseño_deporte" to "Arte_diseno_deporte",
        "Arte_diseno_deporte" to "Arte_diseno_deporte",
    )

    // ─────────────────────────────────────────────────────────────────
    override fun show() {
        super.show()
        cerrarInterior()
        // Sólo Android DEBUG: omitir introducción/tutorial y abrir el mapa.
        if (game.modoPruebasAndroid) {
            modoCarga = true
            tutorialHighlightPos = null
        }
        val skin   = Scene2DSkin.defaultSkin
        val fuente = skin.getFont("default-font")

        // No vaciar caché aquí: las texturas son propiedad de esta pantalla.

        if (dialogoActor == null) {
            dialogoActor = DialogoActor(fuente) { path ->
                TextureRegion(Texture(path.toInternalFile()))
            }
        }

        if (backgroundImage == null) {
            backgroundImage = Image(backgroundTexture).apply {
                setScaling(Scaling.fit)
                setAlign(Align.center)
            }
            backgroundImage?.setFillParent(true)
        }

        stage.clear()

        stage.addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    if (cerrarInterior()) return true
                    if (!modoCarga && dialogoActor?.isVisible == true) {
                        if (!(dialogoActor?.retroceder() ?: false)) game.setScreen<SeleccionPartida>()
                        return true
                    } else if (modoCarga) {
                        game.setScreen<SeleccionPartida>()
                        return true
                    }
                }
                return false
            }
        })

        if (!modoCarga) {
            backgroundImage?.let { stage.addActor(it) }
            dialogoActor?.let { actor ->
                stage.addActor(actor)
                actor.width  = stage.width
                actor.height = stage.height

                actor.alTerminarNombre = { nombre ->
                    GameState.nombreJugador = nombre
                    actor.variables["nombre"] = nombre
                }
                actor.alTerminarEscuela = { escuela ->
                    GameState.nombreEscuela = escuela
                    actor.variables["escuela"] = escuela
                }

                actor.mostrarConversacion(listOf(
                    Dialogo("?????",         "¡Hola! Soy el Ing. Lázaro Cárdenas.",                                       "sprite_saludando.png"),
                    Dialogo("Ing. Cárdenas", "Bienvenido a EDU-TYCOON. Aquí podrás crear tu propia institución educativa.", "sprite_apenado.png"),
                    Dialogo("Ing. Cárdenas", "¿Y por qué no? Llegar a construir un ¡¡IMPERIO EDUCATIVO!!",                 "sprite_explicando.png"),
                    Dialogo("Ing. Cárdenas", "Pero antes que nada, empecemos por lo básico....",                           "sprite_serio.png"),
                    Dialogo("Ing. Cárdenas", "¿Cuál es tu nombre?",                                                        "sprite_hablando.png", TipoDialogo.INPUT),
                    Dialogo("Ing. Cárdenas", "¡Un gusto conocerte {nombre}! Prepárate para el resto.",                     "sprite_saludando.png"),
                    Dialogo("Ing. Cárdenas", "Tener un buen nombre para tu institución lo es todo.",                       "sprite_serio.png"),
                    Dialogo("Ing. Cárdenas", "Así que dime, ¿qué nombre llevará?",                                         "sprite_hablando.png", TipoDialogo.INPUT),
                    Dialogo("Ing. Cárdenas", "¿{escuela}? Suena genial",                                                   "sprite_explicando.png")
                ))
            }

            stage.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (event?.isHandled == true) return
                    dialogoActor?.let { actor ->
                        if (actor.isVisible) {
                            actor.avanzar()
                        } else if (!modoCarga) {
                            modoCarga = true
                            backgroundImage?.remove()
                            stage.root.clearListeners()
                            reAgregarListenerBack()
                            configurarControlesMapa()
                            mostrarTutorial()
                        }
                    }
                }
            })
            Gdx.input.inputProcessor = stage
        } else {
            configurarControlesMapa()
        }

        Gdx.input.setCatchKey(Input.Keys.BACK, true)
    }

    private fun reAgregarListenerBack() {
        stage.addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    if (cerrarInterior()) return true
                    game.setScreen<SeleccionPartida>()
                    return true
                }
                return false
            }
        })
    }

    // ── Controles ────────────────────────────────────────────────────
    private fun configurarControlesMapa() {
        lastMoney      = -1
        lastStudents   = -1
        lastKnowledge = -1
        lastXp = -1; lastEnergy = -1; lastHealth = -1
        audioAmbiente.reanudar()
        cycleTimer     = 0f
        hudDirty       = true

        val multiplexer = InputMultiplexer()
        // Los controles de Scene2D tienen prioridad sobre los gestos del mapa.
        multiplexer.addProcessor(stage)

        val gestureDetector = GestureDetector(object : GestureAdapter() {

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                if (interiorAbierto != null) return true
                val m = map ?: return false
                val worldTouch = Vector3(x, y, 0f)
                camera.unproject(worldTouch)

                // Inversa exacta de wx=x+y, wy=(y-x)*0.5 usada en el render.
                val tiledX = worldTouch.x * 0.5f - worldTouch.y
                val tiledY = worldTouch.x * 0.5f + worldTouch.y

                // Al tocar el mapa, cerramos cualquier ficha que esté abierta.
                // Si tocamos una estructura, se abrirá la nueva abajo.
                currentInfoWindow?.remove()
                currentInfoWindow = null

                try {
                    // Seleccion por la posicion VISUAL de mejoras movidas en Tiled.
                    // Solo nivel >= 2; niveles 1 y desbloqueos mantienen las hitboxes.
                    val edificioMovido = renderEntries.asReversed().firstOrNull { entry ->
                        val propiedad = entry.propiedad
                        if (!propiedad.comprada || propiedad.nivel < 2) false
                        else {
                            val geo = geometriaEdificio(entry)
                            val centroX = entry.bWorldX + geo.desplazamientoX
                            val abajo = entry.bWorldY + geo.desplazamientoY
                            worldTouch.x >= centroX - geo.ancho * 0.38f &&
                            worldTouch.x <= centroX + geo.ancho * 0.38f &&
                            worldTouch.y >= abajo + geo.alto * 0.08f &&
                            worldTouch.y <= abajo + geo.alto * 0.75f
                        }
                    }
                    if (edificioMovido != null) {
                        mostrarGestion(edificioMovido.propiedad)
                        return true
                    }
                    val logicaLayer = m.layers["Logica_Clics"] ?: return false
                    for (obj in logicaLayer.objects) {
                        if (obj !is RectangleMapObject || !obj.isVisible) continue
                        if (obj.rectangle.contains(tiledX, tiledY)) {
                            val rawName   = obj.name ?: ""
                            val propId    = puntosAPropiedad[rawName] ?: rawName
                            val propiedad = PropiedadRepository.getPropiedad(propId) ?: continue

                            // EDU_FIX_INTERIORES_GESTION_V4:
                            // La gestión es la puerta de entrada universal.
                            // Así ninguna textura faltante bloquea el N2/N3.
                            mostrarGestion(propiedad)
                            return true
                        }
                    }
                    // Fallback táctil de Consultorios: la hitbox original
                    // cubre solo una parte del sprite según el TMX recibido.
                    // Se usa únicamente si ninguna hitbox tradicional coincidió.
                    val consultorio = renderEntries.firstOrNull {
                        it.propiedad.id == "Edificio2"
                    }
                    if (consultorio != null &&
                        worldTouch.x >= consultorio.bWorldX - consultorio.bWidth * 0.44f &&
                        worldTouch.x <= consultorio.bWorldX + consultorio.bWidth * 0.44f &&
                        worldTouch.y >= consultorio.bWorldY &&
                        worldTouch.y <= consultorio.bWorldY + consultorio.bHeight * 0.60f) {
                        val prop = consultorio.propiedad
                        // Mismo comportamiento para Consultorio.
                        mostrarGestion(prop)
                        Gdx.app.log("MAP_TOUCH", "Consultorio abierto desde área visual")
                        return true
                    }
                } catch (e: Exception) {
                    Gdx.app.error("MAP_TOUCH", "Error procesando toque", e)
                }
                return false
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                if (interiorAbierto != null) return true
                // Mover inmediatamente durante el arrastre: evita retraso acumulado.
                val unitsX = camera.viewportWidth * camera.zoom / Gdx.graphics.width.coerceAtLeast(1)
                val unitsY = camera.viewportHeight * camera.zoom / Gdx.graphics.height.coerceAtLeast(1)
                targetCameraPos.add(-deltaX * unitsX, deltaY * unitsY, 0f)
                limitarCamaraAlCampus()
                camera.position.set(targetCameraPos)
                return true
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                if (interiorAbierto != null) return true
                zoomObjetivoInterior = null
                val ratio = if (distance > 0) initialDistance / distance else 1f
                camera.zoom = (initialZoom * ratio).coerceIn(1f, maxZoom)
                limitarCamaraAlCampus()
                return true
            }

            override fun pinchStop() { initialZoom = camera.zoom }
        })

        multiplexer.addProcessor(gestureDetector)
        Gdx.input.inputProcessor = multiplexer

        prepararRenderEntries()
        caminantesCampus.configurar(map) // EDU_PASO5_CAMINOS
        animacionesCampus.configurar(map, renderEntries.associate { it.propiedad.id to Vector2(it.bWorldX, it.bWorldY) })
        audioAmbiente.campus()
        if (!camaraCentradaEnCampus) {
            val centro = getBuildingPos("Biblioteca") ?: getBuildingPos("escom_hitbox")
            if (centro != null) {
                targetCameraPos.set(centro)
                camera.position.set(centro)
                camera.update()
            }
            camaraCentradaEnCampus = true
        }
        setupHUD()
    }

    /**
     * Invalida la textura cacheada de un entry específico cuando sube de nivel.
     * Así no tenemos que regenerar toda la lista.
     */
    private fun invalidateRenderEntry(propId: String) {
        val entry = renderEntries.find { it.propiedad.id == propId }
        if (entry == null) {
            Gdx.app.error("EDU_BUILD", "COMPRA SIN REGISTRO DE RENDER: id=$propId")
        } else {
            entry.cachedTex = if (entry.propiedad.comprada) getBuildingTexture(entry.propiedad) else null
            if (entry.propiedad.comprada) {
                if (entry.cachedTex == null) {
                    Gdx.app.error("EDU_BUILD", "COMPRA SIN TEXTURA: id=$propId, nivel=${entry.propiedad.nivel}, prefix=${entry.propiedad.texturePrefix}")
                } else {
                    Gdx.app.log("EDU_BUILD", "COMPRA VISUAL: id=$propId, nivel=${entry.propiedad.nivel}, posicion=(${entry.bWorldX},${entry.bWorldY}), tamaño=(${entry.bWidth},${entry.bHeight})")
                }
            }
        }
        sincronizarEdificiosComprados()
    }

    /** Inserta los comprados en la capa Edificios y en su orden real de Tiled. */
    /** Intercala los comprados en Edificios, respetando la profundidad de Tiled.
     * Medicina debe dibujarse DETRÁS de Consultorios, incluso si Tiled les dio
     * coordenadas Y de orden opuesto (no mover las posiciones visuales).
     */
    // EDU_PHASE1_SCALING: ajustes exclusivamente visuales; no modifican la economía.
    // Ajusta estos valores tras comprobar la separación con los caminos.
    // El nivel 1 siempre conserva las dimensiones originales de Tiled.
    // EDU_DISENO_UNIFICADO_V3: dimensiones independientes X/Y para evitar
    // deformar las texturas de niveles superiores y respetar la fachada.
    // El nivel 1 mantiene exactamente la geometría de Tiled.
    private data class GeometriaEdificio(
        val ancho: Float,
        val alto: Float,
        val desplazamientoX: Float = 0f,
        val desplazamientoY: Float = 0f,
        val rotacion: Float? = null,
        val reflejoX: Boolean? = null,
        val reflejoY: Boolean? = null
    )

    private fun factorEscalaVisual(id: String, nivel: Int): Float {
        if (nivel <= 1) return 1f
        return when (id) {
            "Biblioteca" -> when (nivel) {
                2 -> 0.87f
                3 -> 0.78f
                else -> 0.74f
            }
            else -> when (nivel) {
                2 -> 0.94f
                3 -> 0.88f
                else -> 0.84f
            }
        }
    }

    // EDU_EDITOR_NIVELES_TILED_V1 / EDU_GEOMETRIA_RELATIVA_V3: datos pequeños por nivel.
    // Las capas EDU_NIVEL_* nunca se incluyen en Mapa_Android_Optimizado.tmx.
    private data class NivelTiled(
        val deltaMundoX: Float, val deltaMundoY: Float,
        val ancho: Float, val alto: Float,
        val rotacion: Float, val flipX: Boolean, val flipY: Boolean
    )

    private val posicionesNivelesTiled: Map<String, NivelTiled> by lazy {
        val fichero = Gdx.files.internal("Mapa/ajustes_niveles_runtime.txt")
        if (!fichero.exists() || !fichero.readString("UTF-8").startsWith("# EDU_GEOMETRIA_RELATIVA_V3")) {
            Gdx.app.log("EDU_TILED_NIVELES", "Sin ajustes V3 relativos; geometría V7 segura")
            emptyMap()
        } else {
            val datos = mutableMapOf<String, NivelTiled>()
            try {
                fichero.readString("UTF-8").lineSequence().forEach { linea ->
                    val l = linea.trim()
                    if (l.isBlank() || l.startsWith("#")) return@forEach
                    val p = l.split('|')
                    if (p.size != 9) return@forEach
                    val nivel = p[1].toIntOrNull() ?: return@forEach
                    val x = p[2].toFloatOrNull() ?: return@forEach
                    val y = p[3].toFloatOrNull() ?: return@forEach
                    val ancho = p[4].toFloatOrNull() ?: return@forEach
                    val alto = p[5].toFloatOrNull() ?: return@forEach
                    val rotacion = p[6].toFloatOrNull() ?: return@forEach
                    if (nivel < 2 || !x.isFinite() || !y.isFinite() ||
                        !ancho.isFinite() || !alto.isFinite() || !rotacion.isFinite() ||
                        ancho <= 0f || alto <= 0f) return@forEach
                    datos["${p[0]}|$nivel"] = NivelTiled(
                        x, y, ancho, alto, rotacion,
                        p[7].equals("true", ignoreCase = true),
                        p[8].equals("true", ignoreCase = true)
                    )
                }
            } catch (e: Exception) {
                Gdx.app.error("EDU_TILED_NIVELES", "Datos de Tiled ilegibles; conservar V7", e)
                datos.clear()
            }
            Gdx.app.log("EDU_TILED_NIVELES", "Ajustes visuales de nivel: ${datos.size}")
            datos
        }
    }

    private fun geometriaEdificio(entry: RenderEntry): GeometriaEdificio {
        val id = entry.propiedad.id
        val nivel = entry.propiedad.nivel
        if (nivel > 1) {
            // El editor visual tiene prioridad; si no hay ajuste conserva V7.
            posicionesNivelesTiled["$id|$nivel"]?.let { ajuste ->
                return GeometriaEdificio(
                    ajuste.ancho, ajuste.alto,
                    // El dato V3 es un DESPLAZAMIENTO respecto al nivel 1.
                    // No restar de nuevo las coordenadas absolutas del anclaje.
                    desplazamientoX = ajuste.deltaMundoX,
                    desplazamientoY = ajuste.deltaMundoY,
                    rotacion = ajuste.rotacion,
                    reflejoX = ajuste.flipX,
                    reflejoY = ajuste.flipY
                )
            }
        }
        if (nivel <= 1) return GeometriaEdificio(entry.bWidth, entry.bHeight)

        val escala = factorEscalaVisual(id, nivel)
        // Dirección N3: se aleja del mobiliario del borde izquierdo.
        // Biblioteca N3: el hueco de fachada es más alto y menos ancho.
        val limiteAncho = when {
            id == "Direccion" && nivel >= 3 -> entry.bWidth * 0.72f
            id == "Biblioteca" && nivel == 3 -> entry.bWidth * 0.70f
            else -> entry.bWidth * escala
        }
        val limiteAlto = when {
            id == "Direccion" && nivel >= 3 -> entry.bHeight * 0.82f
            id == "Biblioteca" && nivel == 3 -> entry.bHeight * 1.04f
            else -> entry.bHeight * escala
        }
        val desplazamiento = if (id == "Direccion" && nivel >= 3)
            entry.bWidth * 0.065f else 0f

        // No alterar las proporciones ya aprobadas de los otros 16 edificios.
        if (id != "Direccion" && id != "Biblioteca") {
            return GeometriaEdificio(limiteAncho, limiteAlto, desplazamiento)
        }
        val textura = entry.cachedTex
        if (textura == null || textura.width <= 0 || textura.height <= 0) {
            return GeometriaEdificio(limiteAncho, limiteAlto, desplazamiento)
        }
        // Ajuste de aspecto sin estirar: altura y anchura reales del PNG del
        // nivel actual, no la caja fija del nivel 1.
        val aspecto = textura.width.toFloat() / textura.height.toFloat()
        val ancho = kotlin.math.min(limiteAncho, limiteAlto * aspecto)
        val alto = ancho / aspecto
        return GeometriaEdificio(ancho, alto, desplazamiento)
    }

    private fun sincronizarEdificiosComprados() {
        val medicina = renderEntries.firstOrNull { it.propiedad.id == "Edificio1" }
        val consultorio = renderEntries.firstOrNull { it.propiedad.id == "Edificio2" }
        val menorOrdenY = if (medicina != null && consultorio != null)
            kotlin.math.min(medicina.bSortY, consultorio.bSortY) else null
        val mayorOrdenY = if (medicina != null && consultorio != null)
            kotlin.math.max(medicina.bSortY, consultorio.bSortY) else null
        val menorIndice = if (medicina != null && consultorio != null)
            kotlin.math.min(medicina.bSortIndex, consultorio.bSortIndex) else null
        val mayorIndice = if (medicina != null && consultorio != null)
            kotlin.math.max(medicina.bSortIndex, consultorio.bSortIndex) else null

        // Gastronomía debe quedar DETRÁS del Restaurante cuando ambos existen.
        // Se fuerzan tanto la coordenada de profundidad como el índice para
        // respetar los modos topdown e index del TMX.
        val gastronomia = renderEntries.firstOrNull { it.propiedad.id == "Gastronomia" }
        val restaurante = renderEntries.firstOrNull { it.propiedad.id == "cafeteria" }
        val gastroAntesY = if (gastronomia != null && restaurante != null)
            kotlin.math.min(gastronomia.bSortY, restaurante.bSortY) - 1f else null
        val restauranteDespuesY = if (gastronomia != null && restaurante != null)
            kotlin.math.max(gastronomia.bSortY, restaurante.bSortY) + 1f else null
        val gastroAntesIndice = if (gastronomia != null && restaurante != null)
            kotlin.math.min(gastronomia.bSortIndex, restaurante.bSortIndex) - 1 else null
        val restauranteDespuesIndice = if (gastronomia != null && restaurante != null)
            kotlin.math.max(gastronomia.bSortIndex, restaurante.bSortIndex) + 1 else null

        val purchased = ArrayList<IsometricObjectLayerRenderer.PurchasedSprite>()
        for (entry in renderEntries) {
            if (!entry.propiedad.comprada) continue
            val texture = entry.cachedTex ?: continue
            val sortY = entry.bSortY // EDU_TILED_ORDEN_V4
            val sortIndex = entry.bSortIndex // EDU_TILED_ORDEN_V4
            // Ajustar exclusivamente tamaño visual de mejoras, conservando centro inferior.
            val geometria = geometriaEdificio(entry)
            purchased.add(IsometricObjectLayerRenderer.PurchasedSprite(
                id = entry.propiedad.id,
                texture = texture,
                worldX = entry.bWorldX + geometria.desplazamientoX,
                worldY = entry.bWorldY + geometria.desplazamientoY,
                width = geometria.ancho,
                height = geometria.alto,
                rotation = geometria.rotacion ?: entry.bRotation,
                flipX = geometria.reflejoX ?: entry.bFlipX,
                flipY = geometria.reflejoY ?: entry.bFlipY,
                sortY = sortY,
                sortIndex = sortIndex
            ))
        }
        objectLayerRenderer?.setPurchasedSprites(purchased)
        Gdx.app.log("EDU_BUILD", "Sprites comprados en escena: ${purchased.size}")
    }

    /**
     * OPTIMIZACIÓN PRINCIPAL: Combina la preparación de edificios y etiquetas
     * en una sola pasada. Pre-calcula todo lo necesario para el render loop.
     * Solo se llama al cargar el mapa, no cada frame.
     */
    private fun prepararRenderEntries() {
        renderEntries.clear()
        val m = map ?: return

        fun idCanonico(nombre: String?): String {
            val raw = nombre ?: ""
            return puntosAPropiedad[raw] ?: raw
        }

        val origenPorId = HashMap<String, PointMapObject>()
        m.layers.get("Puntos_origen")?.objects
            ?.filterIsInstance<PointMapObject>()?.forEach { obj ->
                origenPorId[idCanonico(obj.name)] = obj
            }

        val anclajePorId = HashMap<String, PointMapObject>()
        m.layers.get("Puntos_Render_Edificios")?.objects
            ?.filterIsInstance<PointMapObject>()?.forEach { obj ->
                anclajePorId[idCanonico(obj.name)] = obj
            }

        val nombrePorId = HashMap<String, PointMapObject>()
        m.layers.get("Puntos_Nombres")?.objects
            ?.filterIsInstance<PointMapObject>()?.forEach { obj ->
                nombrePorId[idCanonico(obj.name)] = obj
            }

        var registrados = 0
        var sinAnclaje = 0
        for (propiedad in PropiedadRepository.propiedades.values) {
            val id = propiedad.id
            val anchor = anclajePorId[id]
            val origen = origenPorId[id]
            val etiqueta = nombrePorId[id]
            val posicion = anchor?.point ?: origen?.point ?: etiqueta?.point

            if (posicion == null) {
                Gdx.app.error("EDU_BUILD", "SIN POSICIÓN: id=$id. Revisa Puntos_Render_Edificios/Puntos_origen/Puntos_Nombres")
                continue
            }
            if (anchor == null) {
                sinAnclaje++
                Gdx.app.error("EDU_BUILD", "SIN ANCLAJE VISUAL: id=$id. Se emplea punto de origen o etiqueta; regenerar TMX optimizado para posición exacta")
            }

            val wx = posicion.x + posicion.y
            val wy = (posicion.y - posicion.x) * 0.5f
            val w = ((anchor?.properties?.get("renderW") as? Number)?.toFloat()
                ?: propiedad.renderW).coerceAtLeast(1f)
            val h = ((anchor?.properties?.get("renderH") as? Number)?.toFloat()
                ?: propiedad.renderH).coerceAtLeast(1f)
            val rot = (anchor?.properties?.get("renderRotation") as? Number)?.toFloat() ?: 0f
            val flipX = anchor?.properties?.get("renderFlipX") == true
            val flipY = anchor?.properties?.get("renderFlipY") == true
            val sortY = (anchor?.properties?.get("renderSortY") as? Number)?.toFloat() ?: 0f
            val sortIndex = (anchor?.properties?.get("renderSortIndex") as? Number)?.toInt() ?: 0

            val nombrePoint = etiqueta?.point ?: posicion
            val labelX = nombrePoint.x + nombrePoint.y
            val labelY = (nombrePoint.y - nombrePoint.x) * 0.5f
            val layout = GlyphLayout(worldFont, "Edificio de ${propiedad.nombre}")
            val bgW = layout.width + 80f
            val bgH = layout.height + 40f

            renderEntries.add(RenderEntry(
                propiedad = propiedad,
                bWorldX = wx,
                bWorldY = wy,
                bDrawX = wx - w * 0.5f,
                bWidth = w,
                bHeight = h,
                bRotation = rot,
                bFlipX = flipX,
                bFlipY = flipY,
                bSortY = sortY,
                bSortIndex = sortIndex,
                lWorldX = labelX,
                lWorldY = labelY,
                nameLayout = layout,
                nameBgW = bgW,
                nameBgH = bgH,
                nameTextX = labelX - layout.width * 0.5f,
                nameTextY = labelY + layout.height * 0.5f,
                cachedTex = if (propiedad.comprada) getBuildingTexture(propiedad) else null
            ))
            registrados++
            Gdx.app.log("EDU_BUILD", "REGISTRO id=$id, comprada=${propiedad.comprada}, anclaje=${anchor != null}, origen=${origen != null}, posicion=($wx,$wy)")
        }
        Gdx.app.log("EDU_BUILD", "RESUMEN: ${registrados}/${PropiedadRepository.propiedades.size} propiedades, anclajes ausentes=$sinAnclaje")
        sincronizarEdificiosComprados()
        recalcularHUDCache()
    }

    /** Recalcula los totales para el HUD. Se llama solo cuando cambia el estado. */
    private fun recalcularHUDCache() {
        val props = PropiedadRepository.propiedades.values
        cachedTotalAlumnos  = props.filter { it.comprada }.sumOf { it.baseAlumnos * it.nivel }
        cachedTotalMaxLevel = props.sumOf { it.mejoraMax }
        cachedCurrentLevel  = props.filter { it.comprada }.sumOf { it.nivel }
        hudDirty = false
    }

    // ── HUD ───────────────────────────────────────────────────────────
    private fun setupHUD() {
        val skin          = Scene2DSkin.defaultSkin
        val font          = skin.getFont("default-font")
        val goldStyle     = Label.LabelStyle(font, Color.GOLD)
        val cyanStyle     = Label.LabelStyle(font, Color.CYAN)
        val whiteDrawable = skin.newDrawable("white", Color(0f, 0f, 0f, 0.45f))
        val toastBg       = skin.newDrawable("white", Color(0f, 0f, 0f, 0.72f))

        moneyLabel   = Label(formatMoney(GameState.dinero), goldStyle).apply { setFontScale(1.1f) }
        alumnosLabel = Label(formatAlumnos(GameState.alumnosTotales), cyanStyle).apply { setFontScale(1.1f) }

        toastLine1 = Label("", Label.LabelStyle(font, Color.GREEN))
        toastLine2 = Label("", Label.LabelStyle(font, Color.CYAN))

        val sw = stage.width
        val sh = stage.height

        toastTable = Table().apply {
            background = toastBg
            pad(8f, 16f, 8f, 16f)
            defaults().left().padBottom(2f)
            add(toastLine1).row()
            add(toastLine2)
            pack()
            color.a = 0f
            setPosition((sw - width) / 2f, sh * 0.08f)
        }
        stage.addActor(toastTable)

        eventTituloLabel = Label("", Label.LabelStyle(font, Color.WHITE))
        eventEfectoLabel = Label("", Label.LabelStyle(font, Color.WHITE))
        val eventBg = skin.newDrawable("white", Color(0.1f, 0.05f, 0.2f, 0.85f))
        eventTable = Table().apply {
            background = eventBg
            pad(10f, 18f, 10f, 18f)
            defaults().left().padBottom(3f)
            add(eventTituloLabel).row()
            add(eventEfectoLabel)
            pack()
            color.a = 0f
            setPosition((sw - width) / 2f, sh * 0.22f)
        }
        stage.addActor(eventTable)

        // EDU_PASO5_HUD: iconos, capsulas, barras reales y diseño compacto.
        knowledgeLabel = Label("N1 · 0/250 XP", goldStyle)
        energyLabel = Label("20/20", cyanStyle)
        healthLabel = Label("100/100", goldStyle)
        val hud5 = HUDTematico.crear(
            moneyLabel!!, alumnosLabel!!, knowledgeLabel!!, energyLabel!!, healthLabel!!
        ) {
            PauseMenuWindow(game) {
                modoCarga = false
                game.setScreen<SeleccionPartida>()
            }.show(stage)
        }
        energyProgress = hud5.energia
        healthProgress = hud5.salud
        knowledgeProgress = hud5.xp
        stage.addActor(hud5.raiz)
    }

    // ── Render ────────────────────────────────────────────────────────
    override fun render(delta: Float) {
        clearScreen(0.34f, 0.51f, 0.22f, 1f)

        if (modoCarga) {
            // El campus permanece visible: el interior se superpone sin fondo negro.
            actualizarCamara(delta)
            renderizarMundo(delta)
            if (Gdx.graphics.frameId == 60L) {
                Gdx.app.log(
                    "MAP_DEBUG",
                    "Renderizador: ${renderer != null}"
                )

                Gdx.app.log(
                    "MAP_DEBUG",
                    "Camara: ${camera.position} Zoom: ${camera.zoom}"
                )
            }
            actualizarCicloDeJuego(delta)
            actualizarIndicadoresHUD()
            if (ProgresoAdministrador.avanzarTiempo(delta)) mostrarFinDePartida()
        }
        audioAmbiente.actualizar(delta)

        if (modoCarga) {
            tiempoDiagnosticoFPS += delta
            if (tiempoDiagnosticoFPS >= 3f) {
                com.badlogic.gdx.Gdx.app.log("PERFORMANCE", "FPS: ${com.badlogic.gdx.Gdx.graphics.framesPerSecond}")
                tiempoDiagnosticoFPS = 0f
            }
        }
        interiorAbierto?.actualizarPosicion()
        stage.act(delta)
        stage.draw()
    }

    private val limitesVisualesCampus = Rectangle()
    private var limitesVisualesCalculados = false

    private fun limitarCamaraAlCampus() {
        if (!limitesVisualesCalculados) {
            limitesVisualesCalculados =
                objectLayerRenderer?.getVisualBounds(limitesVisualesCampus) == true
            if (!limitesVisualesCalculados) return
        }

        val halfW = camera.viewportWidth * camera.zoom * 0.5f
        val halfH = camera.viewportHeight * camera.zoom * 0.5f
        // Pequeño espacio para apreciar bardas y fachadas completas.
        val margenExterior = 225f
        val minX = limitesVisualesCampus.x + halfW - margenExterior
        val maxX = limitesVisualesCampus.x + limitesVisualesCampus.width - halfW + margenExterior
        val minY = limitesVisualesCampus.y + halfH - margenExterior
        val maxY = limitesVisualesCampus.y + limitesVisualesCampus.height - halfH + margenExterior

        targetCameraPos.x = if (minX <= maxX)
            targetCameraPos.x.coerceIn(minX, maxX)
        else limitesVisualesCampus.x + limitesVisualesCampus.width * 0.5f

        targetCameraPos.y = if (minY <= maxY)
            targetCameraPos.y.coerceIn(minY, maxY)
        else limitesVisualesCampus.y + limitesVisualesCampus.height * 0.5f
    }

    private fun actualizarCamara(delta: Float) {
        if (interiorAbierto == null && zoomObjetivoInterior == null) limitarCamaraAlCampus()
        val alpha = 1f - kotlin.math.exp(-10f * delta.coerceIn(0f, 0.05f))
        camera.position.lerp(targetCameraPos, alpha)
        zoomObjetivoInterior?.let { objetivo ->
            camera.zoom += (objetivo - camera.zoom) * alpha
            if (kotlin.math.abs(camera.zoom - objetivo) < 0.005f) {
                camera.zoom = objetivo
                zoomObjetivoInterior = null
            }
        }
        camera.update()
        renderer?.setView(camera)
        updateScreenRect()
    }


    /**
     * Césped liviano con la textura REAL de grasss.png, sin el contorno
     * transparente en rombos. Obtiene únicamente la zona central opaca de
     * la imagen isométrica y la convierte en una textura repetible.
     * La textura se crea una vez, no por fotograma.
     */
    private fun dibujarPastoRapido(batch: com.badlogic.gdx.graphics.g2d.Batch) {
        if (!fondoPastoIntentado) {
            fondoPastoIntentado = true
            var fuente: Pixmap? = null
            var textura: Pixmap? = null
            try {
                val src = Pixmap(Gdx.files.internal("Mapa/calles/grasss.png"))
                fuente = src
                // El centro del rombo es opaco. No copiar esquinas transparentes.
                val sw = (src.width / 3).coerceAtLeast(2)
                val sh = (src.height / 3).coerceAtLeast(2)
                val sx = (src.width - sw) / 2
                val sy = (src.height - sh) / 2
                val dst = Pixmap(128, 128, Pixmap.Format.RGBA8888)
                textura = dst
                val seed = src.getPixel(src.width / 2, src.height / 2)
                for (yy in 0 until 128) {
                    // Reflejar bordes: primero y último píxel coinciden.
                    val fy = if (yy < 64) yy else 127 - yy
                    val srcY = sy + fy * (sh - 1) / 63
                    for (xx in 0 until 128) {
                        val fx = if (xx < 64) xx else 127 - xx
                        val srcX = sx + fx * (sw - 1) / 63
                        val rgba = src.getPixel(srcX, srcY)
                        val color = if ((rgba and 255) < 32) seed else rgba
                        dst.drawPixel(xx, yy, (color and -256) or 255)
                    }
                }
                fondoPastoRapido = Texture(dst).apply {
                    setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                    setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
                Gdx.app.log("MAP_GRASS", "Patrón real de grasss.png sin rombos: 128x128")
            } catch (e: Exception) {
                Gdx.app.error("MAP_GRASS", "No fue posible preparar grasss.png", e)
            } finally {
                textura?.dispose()
                fuente?.dispose()
            }
        }
        val tex = fondoPastoRapido ?: return
        val halfW = camera.viewportWidth * camera.zoom * 0.5f
        val halfH = camera.viewportHeight * camera.zoom * 0.5f
        val left = camera.position.x - halfW - 64f
        val bottom = camera.position.y - halfH - 64f
        val w = halfW * 2f + 128f
        val h = halfH * 2f + 128f
        batch.projectionMatrix = camera.combined
        batch.begin()
        try {
            batch.color = Color.WHITE
            val period = 128f
            batch.draw(tex, left, bottom, w, h,
                left / period, bottom / period,
                (left + w) / period, (bottom + h) / period)
        } finally {
            batch.end()
        }
    }

    private fun renderizarMundo(delta: Float) {
        val r = renderer ?: return
        val zoom = camera.zoom
        // La base de césped se mantiene ligera. Las teselas NUEVAS (pisos,
        // caminos decorativos) sobreviven en el Fondo_pasto del runtime.
        if (!pastoDetallado) dibujarPastoRapido(r.batch)
        if (layerIndicesToRender.isNotEmpty()) r.render(layerIndicesToRender)
        // Los objetos de Tiled se renderizan por separado: el renderer isometrico
        // no procesa TiledMapTileMapObject de las capas de objetos.
        // Capas y objetos en el mismo orden que Tiled; edificios comprados
        // intercalados con el resto de la capa Edificios.
        objectLayerRenderer?.render(r.batch, camera) { batch ->
            caminantesCampus.dibujarEnBatch(batch, camera, delta)
        } // EDU_PASO5_PROFUNDIDAD
        // Tráfico (asfalto exterior), aves y helicópteros POR ENCIMA de arbustos.
        animacionesCampus.actualizar(delta)
        r.batch.begin()
        try { animacionesCampus.dibujar(r.batch, camera) }
        finally { r.batch.end() }

        r.batch.begin()
        try {
            r.batch.color = Color.WHITE

            val drawLabels    = zoom <= maxZoomForLabels
            val useSmallScale = zoom > maxZoomForFullBuildings

            // Los edificios comprados ya se dibujaron en su posición correcta
            // dentro de Edificios; no repetirlos encima de árboles/caminos.

            // ====================== ETIQUETAS (solo si cerca) ======================
            if (drawLabels) {
                // PASO B: Fondos de etiquetas (no comprados)
                r.batch.color = Color.WHITE
                for (entry in renderEntries) {
                    if (entry.propiedad.comprada) continue
                    // Culling 2D usando coordenadas de la ETIQUETA
                    if (!isVisible(entry.lWorldX, entry.lWorldY, entry.nameBgW * 0.9f, entry.nameBgH * 0.9f)) continue
                    r.batch.draw(
                        labelBgTexture,
                        entry.lWorldX - (entry.nameBgW / 2f),
                        entry.lWorldY - (entry.nameBgH / 2f),
                        entry.nameBgW,
                        entry.nameBgH
                    )
                }

                // PASO C: Textos (la fuente mantiene el color internamente)
                worldFont.color = Color.GOLD
                for (entry in renderEntries) {
                    if (entry.propiedad.comprada) continue
                    val layout = entry.nameLayout ?: continue
                    // Culling 2D usando coordenadas de la ETIQUETA
                    if (!isVisible(entry.lWorldX, entry.lWorldY, entry.nameBgW, entry.nameBgH)) continue
                    worldFont.draw(r.batch, layout, entry.nameTextX, entry.nameTextY)
                }
            }

            // Tutorial highlight
            tutorialHighlightPos?.let { pos ->
                tutorialTimer += delta
                val pulse = 1f + 0.3f * MathUtils.sin(tutorialTimer * 10f)
                val size  = 380f * pulse
                r.batch.color = Color(1f, 0.9f, 0f, 0.45f)
                r.batch.draw(highlightTexture, pos.x - size / 2f, pos.y - size / 4f, size, size / 2f)
                r.batch.color = Color.WHITE
            }

        } catch (e: Exception) {
            Gdx.app.error("RENDER", "Error en renderizado de mundo: ${e.message}")
        }
        r.batch.color = Color.WHITE
        r.batch.end()
    }

    private fun actualizarCicloDeJuego(delta: Float) {
        cycleTimer += delta
        while (cycleTimer >= cycleDuration) {
            cycleTimer -= cycleDuration
            cycleEngine.advanceCycle()
            hudDirty = true   // El ciclo cambia dinero/alumnos → invalidar caché HUD
            // Paso 4: el dinero solo procede de completar retos.
        }
    }

    /**
     * OPTIMIZACIÓN: Ya no hace sumOf en cada frame.
     * Solo recalcula si hudDirty = true (al comprar/mejorar/fin de ciclo).
     */
    private fun actualizarIndicadoresHUD() {
        if (hudDirty) recalcularHUDCache()

        if (GameState.dinero != lastMoney) {
            lastMoney = GameState.dinero
            moneyLabel?.setText(formatMoney(lastMoney))
        }

        if (cachedTotalAlumnos != lastStudents) {
            lastStudents = cachedTotalAlumnos
            alumnosLabel?.setText(formatAlumnos(lastStudents))
        }

        if (lastXp != ProgresoAdministrador.experiencia) {
            lastXp = ProgresoAdministrador.experiencia
            lastKnowledge = ProgresoAdministrador.nivelConocimiento
            knowledgeLabel?.setText("N${ProgresoAdministrador.nivelConocimiento} · ${ProgresoAdministrador.xpNivel}/${ProgresoAdministrador.xpSiguienteNivel} XP")
            knowledgeProgress?.setRange(0f, ProgresoAdministrador.xpSiguienteNivel.toFloat())
            knowledgeProgress?.value = ProgresoAdministrador.xpNivel.toFloat()
        }
        if (lastEnergy != ProgresoAdministrador.energia) {
            lastEnergy = ProgresoAdministrador.energia
            energyLabel?.setText("${lastEnergy}/20")
            energyProgress?.value = lastEnergy.toFloat()
            energyLabel?.color = if (lastEnergy <= 4) Color.ORANGE else Color.CYAN
        }
        if (lastHealth != ProgresoAdministrador.salud) {
            lastHealth = ProgresoAdministrador.salud
            healthLabel?.setText("${lastHealth}/100")
            healthProgress?.value = lastHealth.toFloat()
            healthLabel?.color = if (lastHealth <= 25) Color.RED else Color.GREEN
        }
    }

    // ── Resize / Dispose ──────────────────────────────────────────────
    override fun hide() {
        audioAmbiente.suspender()
        cerrarInterior()
        super.hide()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        // Altura virtual fija: misma escala visual en dispositivos distintos.
        camera.viewportHeight = virtualHeight
        camera.viewportWidth = virtualHeight * width.coerceAtLeast(1).toFloat() / height.coerceAtLeast(1).toFloat()
        camera.update()
        interiorAbierto?.actualizarPosicion()
    }

    override fun dispose() {
        cerrarInterior()
        super.dispose()
        backgroundTexture.dispose()
        menuIconTexture.dispose()
        highlightTexture.dispose()
        worldFont.dispose()
        labelBgTexture.dispose()
        buildingTextureCache.values.forEach { it?.dispose() }
        buildingTextureCache.clear()
        fondoPastoRapido?.dispose()
        renderer?.dispose()
        map?.dispose()
        audioAmbiente.dispose()
        caminantesCampus.dispose()
        animacionesCampus.dispose()
        UIEstiloPaso5.dispose() // EDU_PASO5_RECURSOS
    }

    // ── Tutorial ──────────────────────────────────────────────────────
    private fun getBuildingPos(id: String): Vector3? {
        return renderEntries.find { it.propiedad.id == id }
            ?.let { Vector3(it.bWorldX, it.bWorldY, 0f) }
    }

    private fun mostrarTutorial() {
        val posEscom     = getBuildingPos("escom_hitbox")
        val posDireccion = getBuildingPos("Direccion")

        dialogoActor?.let { actor ->
            actor.isVisible = true
            actor.mostrarConversacion(listOf(
                Dialogo("Ing. Cárdenas", "¡Bienvenido, Director! Vamos a darte un recorrido rápido por las herramientas de gestión.", "sprite_saludando.png") {
                    getBuildingPos("Biblioteca")?.let { targetCameraPos.set(it) }
                    camera.zoom = 6f
                    tutorialHighlightPos = null
                },
                Dialogo("Ing. Cárdenas", "Mira arriba a la izquierda. Ese es tu Presupuesto. Úsalo sabiamente para expandir el campus.", "sprite_explicando.png"),
                Dialogo("Ing. Cárdenas", "A su lado verás el número de Alumnos. Entre más y mejores edificios tengas, más estudiantes atraerás.", "sprite_hablando.png"),
                Dialogo("Ing. Cárdenas", "El Conocimiento crece al superar retos. Cuida tu energía y salud para continuar.", "sprite_serio.png"),
                Dialogo("Ing. Cárdenas", "Para crecer, simplemente toca cualquier edificio o terreno en el mapa.", "sprite_hablando.png") {
                    posEscom?.let {
                        targetCameraPos.set(it.x, it.y, 0f)
                        initialZoom = 3f; camera.zoom = 3f
                        tutorialHighlightPos = it
                    }
                },
                Dialogo("Ing. Cárdenas", "Al tocar un lugar como la ESCOM, se abrirá una ventana para comprarlo o subirlo de nivel.", "sprite_explicando.png"),
                Dialogo("Ing. Cárdenas", "Para mejorar edificios necesitas conocimiento obtenido en sus retos temáticos.", "sprite_hablando.png") {
                    posDireccion?.let {
                        targetCameraPos.set(it.x, it.y, 0f)
                        initialZoom = 4f; camera.zoom = 4f
                        tutorialHighlightPos = it
                    }
                },
                Dialogo("Ing. Cárdenas", "No olvides explorar todo el campus arrastrando el dedo y haciendo zoom.", "sprite_saludando.png") {
                    getBuildingPos("Biblioteca")?.let { targetCameraPos.set(it) }
                    initialZoom = 5f; camera.zoom = 5f
                    tutorialHighlightPos = null
                },
                Dialogo("Ing. Cárdenas", "¡Ahora sí, pon la técnica al servicio de la patria! ¡¡HUÉLUM!!", "sprite_saludando.png")
            ))
        }
    }

    // ── Toasts ────────────────────────────────────────────────────────
    private fun showCycleToast(result: EconomyEngine.CycleResult) {
        val toast = toastTable ?: return
        toastLine1?.setText("Ingresos:  +${formatMoney(result.ingresos)}")
        toastLine2?.setText("Alumnos:    ${formatAlumnos(GameState.alumnosTotales)}")
        toast.clearActions()
        toast.color.a = 0f
        toast.invalidateHierarchy(); toast.pack()
        toast.setPosition((stage.width - toast.width) / 2f, stage.height * 0.08f)
        toast.addAction(Actions.sequence(
            Actions.fadeIn(0.25f), Actions.delay(3.5f), Actions.fadeOut(0.4f)
        ))
    }

    private fun showEventToast(evento: GameEvent) {
        val toast   = eventTable ?: return
        val esGasto = evento.efecto is EventoEfecto.Gasto
        val cantidad = when (val e = evento.efecto) {
            is EventoEfecto.Gasto   -> e.cantidad
            is EventoEfecto.Ingreso -> e.cantidad
        }
        val signo = if (esGasto) "-" else "+"
        eventTituloLabel?.setText(evento.titulo)
        eventTituloLabel?.setColor(if (esGasto) Color.RED else Color.GREEN)
        eventEfectoLabel?.setText("${signo}\$${formatMoney(cantidad)}  —  ${evento.descripcion}")
        eventEfectoLabel?.setColor(if (esGasto) Color.RED else Color.GREEN)
        toast.clearActions()
        toast.color.a = 0f
        toast.invalidateHierarchy(); toast.pack()
        toast.setPosition((stage.width - toast.width) / 2f, stage.height * 0.22f)
        toast.addAction(Actions.sequence(
            Actions.fadeIn(0.25f), Actions.delay(5f), Actions.fadeOut(0.4f)
        ))
    }

    private fun formatMoney(v: Long) = when {
        v >= 1_000_000L -> "${"%.2f".format(v / 1_000_000.0)}M"
        v >= 1_000L     -> "${"%.1f".format(v / 1_000.0)}K"
        else            -> v.toString()
    }

    private fun formatAlumnos(v: Int) = when {
        v >= 1_000 -> "${"%.1f".format(v / 1_000.0)}K"
        else       -> v.toString()
    }
}
