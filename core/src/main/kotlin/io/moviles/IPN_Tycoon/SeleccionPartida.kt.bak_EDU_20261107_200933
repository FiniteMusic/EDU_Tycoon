package io.moviles.IPN_Tycoon

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import ktx.actors.onChange
import ktx.app.clearScreen
import ktx.assets.toInternalFile
import ktx.scene2d.*

class SeleccionPartida(game: Main) : BaseScreen(game) {

    private var pixelFont: BitmapFont? = null

    private val backgroundTexture: Texture by lazy {
        Texture("back_seleccion.png".toInternalFile()).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    private val buttonTexture: Texture by lazy {
        val pixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.valueOf("3e3e54"))
        pixmap.fillRectangle(0, 0, 12, 12)
        pixmap.setColor(Color.valueOf("8cbd5c"))
        pixmap.fillRectangle(1, 1, 10, 10)
        pixmap.setColor(Color.valueOf("c8e6a1"))
        pixmap.fillRectangle(1, 1, 10, 2)
        pixmap.setColor(Color.valueOf("5b8c3f"))
        pixmap.fillRectangle(1, 9, 10, 2)
        Texture(pixmap).apply { setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
    }

    private fun generatePixelFont(size: Int): BitmapFont {
        val fontFile = "font.ttf".toInternalFile()
        return if (fontFile.exists()) {
            val generator = FreeTypeFontGenerator(fontFile)
            val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = size
                color = Color.WHITE
                borderWidth = 2f
                borderColor = Color.valueOf("3e3e54")
                minFilter = Texture.TextureFilter.Nearest
                magFilter = Texture.TextureFilter.Nearest
            }
            val font = generator.generateFont(parameter)
            generator.dispose()
            font
        } else {
            BitmapFont().apply { data.setScale(size / 15f) }
        }
    }

    override fun show() {
        super.show()
        pixelFont = generatePixelFont(24)

        val drawable = NinePatchDrawable(NinePatch(buttonTexture, 4, 4, 4, 4))
        val customButtonStyle = TextButtonStyle().apply {
            font = pixelFont
            up   = drawable
            over = drawable.tint(Color.valueOf("d1e8b2"))
            down = drawable.tint(Color.valueOf("a0a0a0"))
        }

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

                    table {
                        textButton("NUEVA PARTIDA") {
                            style = customButtonStyle
                            onChange {
                                GameState.reset(GameState.DINERO_INICIAL_REAL)
                                PropiedadRepository.resetProgress()
                                val screen = game.getScreen<GameScreen>()
                                screen.modoCarga = false
                                game.setScreen<GameScreen>()
                            }
                        }.cell(width = 450f, height = 90f, padBottom = 25f)

                        row()

                        // ← ÚNICO CAMBIO: va a PartidasGuardadas en vez de GameScreen
                        textButton("CARGAR PARTIDA") {
                            style = customButtonStyle
                            onChange {
                                game.setScreen<PartidasGuardadas>()
                            }
                        }.cell(width = 450f, height = 90f)
                    }.cell(padTop = 120f)
                }
            }
        }
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        super.dispose()
        buttonTexture.dispose()
        backgroundTexture.dispose()
        pixelFont?.dispose()
    }
}
