package io.moviles.IPN_Tycoon

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import ktx.actors.onChange
import ktx.app.clearScreen
import ktx.assets.toInternalFile
import ktx.scene2d.*
import java.text.SimpleDateFormat
import java.util.*

class PartidasGuardadas(game: Main) : BaseScreen(game) {

    private var titleFont: BitmapFont? = null
    private var infoFont: BitmapFont? = null

    private val backgroundTexture: Texture by lazy {
        Texture("background.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    private val buttonTexture: Texture by lazy {
        val pixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.valueOf("3e3e54")); pixmap.fillRectangle(0, 0, 12, 12)
        pixmap.setColor(Color.valueOf("8cbd5c")); pixmap.fillRectangle(1, 1, 10, 10)
        pixmap.setColor(Color.valueOf("c8e6a1")); pixmap.fillRectangle(1, 1, 10, 2)
        pixmap.setColor(Color.valueOf("5b8c3f")); pixmap.fillRectangle(1, 9, 10, 2)
        Texture(pixmap).apply { setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
    }

    private var saveSlot1: GameSaveData? = null
    private var saveSlot2: GameSaveData? = null
    private var saveSlot3: GameSaveData? = null

    private fun generateFont(size: Int): BitmapFont {
        val fontFile = "font.ttf".toInternalFile()
        return if (fontFile.exists()) {
            val generator = FreeTypeFontGenerator(fontFile)
            val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size   = size
                color       = Color.WHITE
                borderWidth = 1.5f
                borderColor = Color.valueOf("3e3e54")
                minFilter   = Texture.TextureFilter.Nearest
                magFilter   = Texture.TextureFilter.Nearest
            }
            val font = generator.generateFont(param)
            generator.dispose()
            font
        } else BitmapFont().apply { data.setScale(size / 15f) }
    }

    override fun show() {
        super.show()
        titleFont = generateFont(16)
        infoFont  = generateFont(12)
        loadSlots()
    }

    private fun loadSlots() {
        game.saveManager.cargarSlots { s1, s2, s3 ->
            saveSlot1 = s1
            saveSlot2 = s2
            saveSlot3 = s3
            buildUI()
        }
    }

    private fun buildUI() {
        stage.clear()

        val drawable = NinePatchDrawable(NinePatch(buttonTexture, 4, 4, 4, 4))

        val btnStyle = TextButtonStyle().apply {
            font = titleFont
            up   = drawable
            over = drawable.tint(Color.valueOf("d1e8b2"))
            down = drawable.tint(Color.valueOf("a0a0a0"))
        }

        val borrarStyle = TextButtonStyle().apply {
            font = infoFont
            up   = drawable.tint(Color.valueOf("cc4444"))
            over = drawable.tint(Color.valueOf("ee6666"))
            down = drawable.tint(Color.valueOf("aa2222"))
        }

        val volverFont = generateFont(18) // Revertido a 18
        val volverStyle = TextButtonStyle().apply {
            font = volverFont
            up   = drawable
            over = drawable.tint(Color.valueOf("d1e8b2"))
            down = drawable.tint(Color.valueOf("a0a0a0"))
        }

        val infoStyle = Label.LabelStyle(infoFont, Color.WHITE)

        stage.actors {
            stack {
                setFillParent(true)

                image(backgroundTexture) {
                    setScaling(Scaling.fill)
                    setAlign(Align.center)
                }

                table {
                    setFillParent(true)
                    center()
                    pad(16f)

                    // ── Slot 1 ────────────────────────────────────────
                    addSlot(this, 1, saveSlot1, btnStyle, infoStyle, borrarStyle)
                    label("").cell(width = 14f)

                    // ── Slot 2 ────────────────────────────────────────
                    addSlot(this, 2, saveSlot2, btnStyle, infoStyle, borrarStyle)
                    label("").cell(width = 14f)

                    // ── Slot 3 ────────────────────────────────────────
                    addSlot(this, 3, saveSlot3, btnStyle, infoStyle, borrarStyle)

                    row()

                    textButton("← VOLVER") {
                        style = volverStyle
                        onChange { game.setScreen<SeleccionPartida>() }
                    }.cell(colspan = 5, padTop = 30f, width = 240f, height = 65f) // Revertido a 240x65
                }
            }
        }
    }

    private fun addSlot(
        table: KTableWidget,
        slotNum: Int,
        data: GameSaveData?,
        btnStyle: TextButtonStyle,
        infoStyle: Label.LabelStyle,
        borrarStyle: TextButtonStyle
    ) {
        val slotWidth  = 230f
        val slotHeight = 120f

        table.table {
            textButton(slotTitle(slotNum, data)) {
                style = btnStyle
                label.setWrap(true)
                label.setAlignment(Align.center)
                onChange { onSlotClicked(slotNum) }
            }.cell(width = slotWidth, height = slotHeight)

            row()

            label(slotSubtitle(slotNum, data)) {
                style = infoStyle
                setAlignment(Align.center)
                wrap = true
            }.cell(width = slotWidth, padTop = 5f)

            // Botón de eliminar si hay datos
            if (data != null) {
                row()
                textButton("ELIMINAR") {
                    style = borrarStyle
                    onChange {
                        game.saveManager.eliminarSlot(slotNum) {
                            loadSlots()
                        }
                    }
                }.cell(width = slotWidth * 0.7f, height = 30f, padTop = 10f)
            }
        }
    }

    private fun onSlotClicked(slotNum: Int) {
        val gameScreen = game.getScreen<GameScreen>()

        val currentData = when(slotNum) {
            1 -> saveSlot1
            2 -> saveSlot2
            3 -> saveSlot3
            else -> null
        }

        if (currentData != null) {
            game.saveManager.cargarPartida(slotNum) { ok ->
                if (ok) { gameScreen.modoCarga = true; game.setScreen<GameScreen>() }
            }
        } else {
            GameState.reset(GameState.DINERO_INICIAL_REAL); GameState.slotActual = slotNum
            PropiedadRepository.resetProgress()
            gameScreen.modoCarga = false
            game.setScreen<GameScreen>()
        }
    }

    private fun slotTitle(slotNum: Int, data: GameSaveData?): String = when {
        data != null -> data.nombreEscuela.ifBlank { "Partida $slotNum" }
        else         -> "+\nNUEVA PARTIDA"
    }

    private fun slotSubtitle(slotNum: Int, data: GameSaveData?): String = when {
        data == null -> "Slot $slotNum vacio"
        else -> {
            val fecha = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                .format(Date(data.fechaGuardado))
            "${data.nombreJugador}\n$${fmt(data.dinero)}\nCiclos: ${data.ciclosJugados}\n$fecha"
        }
    }

    private fun fmt(v: Long) = when {
        v >= 1_000_000L -> "${"%.1f".format(v / 1_000_000.0)}M"
        v >= 1_000L     -> "${"%.0f".format(v / 1_000.0)}K"
        else            -> v.toString()
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        super.dispose()
        backgroundTexture.dispose()
        buttonTexture.dispose()
        titleFont?.dispose()
        infoFont?.dispose()
    }
}
