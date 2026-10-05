package io.moviles.IPN_Tycoon

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
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
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
import io.moviles.IPN_Tycoon.rendering.BuildingRenderer
import io.moviles.IPN_Tycoon.rendering.BuildingAnimationRegistry

class GameScreen(game: Main) : BaseScreen(game) {

    // ── Motor de ciclos ───────────────────────────────────────────────
    private val cycleEngine       = GameCycleEngine()
    private val economyEngine     = EconomyEngine()
    private val estudiantesEngine = EstudiantesEngine()
    private val eventEngine       = EventEngine { evento -> showEventToast(evento) }
    private var cycleTimer        = 0f
    private val cycleDuration     = 30f

    // ── Ajustes de Optimización ──────────────────────────────────────
    private val maxZoom                = 7.0f
    private val maxZoomForLabels       = 4.0f

    init {
        cycleEngine.addListener(economyEngine)
        cycleEngine.addListener(estudiantesEngine)
        cycleEngine.addListener(eventEngine)
    }

    // ── Mapa ──────────────────────────────────────────────────────────
    private val map: TiledMap? by lazy {
        try {
            TmxMapLoader().load("Mapa/Mapa_General.tmx")
        } catch (e: Exception) {
            Gdx.app.error("MAP_ERROR", "Error cargando mapa: ${e.message}")
            null
        }
    }
    private val renderer: IsometricTiledMapRenderer? by lazy {
        map?.let { IsometricTiledMapRenderer(it) }
    }

    private val layerIndicesToRender: IntArray by lazy {
        val m = map ?: return@lazy intArrayOf()
        (0 until m.layers.count)
            .filter { m.layers[it].name != "Edificios" }
            .toIntArray()
    }

    private val buildingRenderer = BuildingRenderer()
    private val buildingAnimations = BuildingAnimationRegistry()



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
    private var animationStateTime = 0f
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

    // OPTIMIZACIÓN: Pre-resolvemos la textura en BuildingRenderInfo para
    // no llamar a getBuildingTexture() cada frame (evita hashmap lookup + lambda)
    private fun getBuildingTexture(propiedad: Propiedad): Texture? {
        val prefix = propiedad.texturePrefix ?: return null
        val key    = "${prefix}lvl${propiedad.nivel}"
        // getOrPut con valor ya computado evita la lambda en cache hit
        val cached = buildingTextureCache[key]
        if (cached != null) return cached
        val file = "Mapa/Edificios/$key.png".toInternalFile()
        val tex = if (file.exists()) Texture(file) else null
        if (tex == null) Gdx.app.error("TEXTURE", "No encontrado: $key.png")
        buildingTextureCache[key] = tex
        return tex
    }

    private val worldFont: BitmapFont by lazy {
        val skinFont = Scene2DSkin.defaultSkin.getFont("default-font")
        skinFont.data.setScale(1.0f)
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
    private val backgroundTexture: Texture by lazy {
        Texture("background.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }
    private var backgroundImage: Image? = null

    // ── Icono Menú ────────────────────────────────────────────────────
    private val menuIconTexture: Texture by lazy {
        Texture("menuicon.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────
    private var moneyLabel:       Label? = null
    private var alumnosLabel:     Label? = null
    private var reputationLabel:  Label? = null
    private var lastMoney:        Long = -1
    private var lastStudents:     Int = -1
    private var lastReputation:   Int = -1
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
        Texture(pixmap).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
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
        val bDrawX:      Float,   // bWorldX - renderW/2
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
        "palapas"        to "palapas"
    )

    // ─────────────────────────────────────────────────────────────────
    override fun show() {
        super.show()
        val skin   = Scene2DSkin.defaultSkin
        val fuente = skin.getFont("default-font")

        buildingTextureCache.clear()

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
        lastReputation = -1
        cycleTimer     = 0f
        hudDirty       = true

        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(stage)

        val gestureDetector = GestureDetector(object : GestureAdapter() {

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                val m = map ?: return false
                val worldTouch = Vector3(x, y, 0f)
                camera.unproject(worldTouch)

                val tileWidth  = 64f
                val tileHeight = 32f
                val tiledX = (worldTouch.x / (tileWidth  / 2f) - worldTouch.y / (tileHeight / 2f)) / 2f * tileHeight
                val tiledY = (worldTouch.y / (tileHeight / 2f) + worldTouch.x / (tileWidth  / 2f)) / 2f * tileHeight

                // Al tocar el mapa, cerramos cualquier ficha que esté abierta.
                // Si tocamos una estructura, se abrirá la nueva abajo.
                currentInfoWindow?.remove()
                currentInfoWindow = null

                try {
                    val logicaLayer = m.layers["Logica_Clics"] ?: return false
                    logicaLayer.objects.filterIsInstance<RectangleMapObject>().forEach { obj ->
                        if (obj.rectangle.contains(tiledX, tiledY)) {
                            val rawName   = obj.name ?: ""
                            val propId    = puntosAPropiedad[rawName] ?: rawName
                            val propiedad = PropiedadRepository.getPropiedad(propId) ?: return false

                            currentInfoWindow = BuildingInfoWindow(propiedad) {
                                // Invalidar HUD y refrescar la textura cacheada del entry
                                hudDirty = true
                                invalidateRenderEntry(propiedad.id)
                                Gdx.app.log("GAME", "${propiedad.nombre} → nivel ${propiedad.nivel}")
                            }
                            currentInfoWindow?.show(stage)
                            return true
                        }
                    }
                } catch (_: Exception) {}
                return false
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                targetCameraPos.add(-deltaX * camera.zoom, deltaY * camera.zoom, 0f)
                return true
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                val ratio = if (distance > 0) initialDistance / distance else 1f
                camera.zoom = (initialZoom * ratio).coerceIn(1f, maxZoom)
                return true
            }

            override fun pinchStop() { initialZoom = camera.zoom }
        })

        multiplexer.addProcessor(gestureDetector)
        Gdx.input.inputProcessor = multiplexer

        prepararRenderEntries()
        setupHUD()
    }

    /**
     * Invalida la textura cacheada de un entry específico cuando sube de nivel.
     * Así no tenemos que regenerar toda la lista.
     */
    private fun invalidateRenderEntry(propId: String) {
        renderEntries.find { it.propiedad.id == propId }?.let { entry ->
            entry.cachedTex = getBuildingTexture(entry.propiedad)
        }
    }

    /**
     * OPTIMIZACIÓN PRINCIPAL: Combina la preparación de edificios y etiquetas
     * en una sola pasada. Pre-calcula todo lo necesario para el render loop.
     * Solo se llama al cargar el mapa, no cada frame.
     */
    private fun prepararRenderEntries() {
        renderEntries.clear()

        val edificiosLayer = map?.layers?.get("Puntos_origen") ?: return
        val nombresLayer   = map?.layers?.get("Puntos_Nombres")

        // Construir mapa de posiciones de etiquetas indexado por nombre
        val nombresPorId = mutableMapOf<String, Pair<Float, Float>>()
        nombresLayer?.objects?.filterIsInstance<PointMapObject>()?.forEach { obj ->
            val rawName = obj.name ?: ""
            val propId  = puntosAPropiedad[rawName] ?: rawName
            val wx = obj.point.x + obj.point.y
            val wy = (obj.point.y - obj.point.x) * 0.5f
            nombresPorId[propId] = Pair(wx, wy)
        }

        // Una sola pasada para edificios — también une info de etiqueta si existe
        edificiosLayer.objects.filterIsInstance<PointMapObject>().forEach { obj ->
            val rawName   = obj.name ?: ""
            val propId    = puntosAPropiedad[rawName] ?: rawName
            val propiedad = PropiedadRepository.getPropiedad(propId) ?: return@forEach

            val buildingWorldX = obj.point.x + obj.point.y
            val buildingWorldY = (obj.point.y - obj.point.x) * 0.5f

            // Datos de la etiqueta flotante (si hay punto de nombre asociado)
            val (labelWorldX, labelWorldY) = nombresPorId[propId] ?: Pair(buildingWorldX, buildingWorldY)
            val texto   = "Edificio de ${propiedad.nombre}"
            val layout  = GlyphLayout(worldFont, texto)
            val padX    = 40f; val padY = 20f
            val bgW     = layout.width  + padX * 2f
            val bgH     = layout.height + padY * 2f

            renderEntries.add(RenderEntry(
                propiedad  = propiedad,
                bWorldX    = buildingWorldX,
                bWorldY    = buildingWorldY,
                bDrawX     = buildingWorldX - (propiedad.renderW / 2f),
                lWorldX    = labelWorldX,
                lWorldY    = labelWorldY,
                nameLayout = layout,
                nameBgW    = bgW,
                nameBgH    = bgH,
                nameTextX  = labelWorldX - (layout.width  / 2f),
                nameTextY  = labelWorldY + (layout.height / 2f),
                cachedTex  = getBuildingTexture(propiedad)
            ))
        }

        // Precalcular acumuladores para el HUD
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

        val hudTable = Table().apply {
            setFillParent(true)
            top()
            add(Table().apply {
                background = whiteDrawable
                pad(8f)
                add(Label("$ ", goldStyle))
                add(moneyLabel).padRight(20f)
                add(Label("Alumnos: ", cyanStyle))
                add(alumnosLabel).padRight(20f)
                add(Label("Reputación: ", goldStyle)).padRight(5f)
                reputationLabel = Label("0%", goldStyle).apply { setFontScale(1.1f) }
                add(reputationLabel)
            }).left().expandX().pad(10f)

            add(scene2d.image(menuIconTexture) {
                setScaling(Scaling.fit)
                setAlign(Align.center)
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        PauseMenuWindow(game) {
                            modoCarga = false
                            game.setScreen<SeleccionPartida>()
                        }.show(stage)
                    }
                })
            }).right().pad(10f).size(80f)
        }
        stage.addActor(hudTable)
    }

    // ── Render ────────────────────────────────────────────────────────
    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)

        if (modoCarga) {
            animationStateTime += delta
            actualizarCamara()      // También llama updateScreenRect()
            renderizarMundo(delta)
            actualizarCicloDeJuego(delta)
            actualizarIndicadoresHUD()
        }

        stage.act(delta)
        stage.draw()
    }

    private fun actualizarCamara() {
        camera.position.lerp(targetCameraPos, 0.2f)
        camera.update()
        renderer?.setView(camera)
        // OPTIMIZACIÓN: Actualizar el rect de culling 2D una vez por frame
        updateScreenRect()
    }

    private fun renderizarMundo(delta: Float) {
        val r = renderer ?: return
        val zoom = camera.zoom
        r.render(layerIndicesToRender)

        r.batch.begin()
        try {
            r.batch.color = Color.WHITE

            val drawLabels    = zoom <= maxZoomForLabels


            // ====================== EDIFICIOS COMPRADOS ======================
            for (entry in renderEntries) {
                if (!entry.propiedad.comprada) continue

                val p   = entry.propiedad
                val tex = entry.cachedTex ?: continue

                // Culling 2D usando coordenadas del EDIFICIO
                if (!isVisible(entry.bWorldX, entry.bWorldY, p.renderW, p.renderH)) continue

                val animatedElements = buildingAnimations.getElements(
                    buildingId = p.id,
                    level = p.nivel
                )

                buildingRenderer.render(
                    batch = r.batch,
                    texture = tex,
                    propiedad = p,
                    drawX = entry.bDrawX,
                    drawY = entry.bWorldY,
                    stateTime = animationStateTime,
                    animatedElements = animatedElements
                )
            }

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
            economyEngine.lastResult?.let { showCycleToast(it) }
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

        val reputation = if (cachedTotalMaxLevel > 0) {
            ((cachedCurrentLevel.toFloat() / cachedTotalMaxLevel) * 100).toInt().coerceIn(0, 100)
        } else 0

        if (reputation != lastReputation) {
            lastReputation = reputation
            reputationLabel?.setText("$lastReputation%")
            reputationLabel?.color = when {
                reputation >= 80 -> Color.GOLD
                reputation >= 50 -> Color.YELLOW
                else             -> Color.WHITE
            }
        }
    }

    // ── Resize / Dispose ──────────────────────────────────────────────
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.viewportWidth  = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        super.dispose()
        backgroundTexture.dispose()
        menuIconTexture.dispose()
        highlightTexture.dispose()
        worldFont.dispose()
        labelBgTexture.dispose()
        buildingTextureCache.values.forEach { it?.dispose() }
        buildingTextureCache.clear()
        map?.dispose()
        buildingAnimations.dispose()
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
                    targetCameraPos.set(-436f, 1360f, 0f)
                    camera.zoom = 6f
                    tutorialHighlightPos = null
                },
                Dialogo("Ing. Cárdenas", "Mira arriba a la izquierda. Ese es tu Presupuesto. Úsalo sabiamente para expandir el campus.", "sprite_explicando.png"),
                Dialogo("Ing. Cárdenas", "A su lado verás el número de Alumnos. Entre más y mejores edificios tengas, más estudiantes atraerás.", "sprite_hablando.png"),
                Dialogo("Ing. Cárdenas", "La Reputación indica el progreso global de tu institución. ¡Tu meta es alcanzar el 100%!", "sprite_serio.png"),
                Dialogo("Ing. Cárdenas", "Para crecer, simplemente toca cualquier edificio o terreno en el mapa.", "sprite_hablando.png") {
                    posEscom?.let {
                        targetCameraPos.set(it.x, it.y, 0f)
                        initialZoom = 3f; camera.zoom = 3f
                        tutorialHighlightPos = it
                    }
                },
                Dialogo("Ing. Cárdenas", "Al tocar un lugar como la ESCOM, se abrirá una ventana para comprarlo o subirlo de nivel.", "sprite_explicando.png"),
                Dialogo("Ing. Cárdenas", "Subir de nivel un edificio aumenta su capacidad de alumnos y la reputación de la escuela.", "sprite_hablando.png") {
                    posDireccion?.let {
                        targetCameraPos.set(it.x, it.y, 0f)
                        initialZoom = 4f; camera.zoom = 4f
                        tutorialHighlightPos = it
                    }
                },
                Dialogo("Ing. Cárdenas", "No olvides explorar todo el campus arrastrando el dedo y haciendo zoom.", "sprite_saludando.png") {
                    targetCameraPos.set(-436f, 1360f, 0f)
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
