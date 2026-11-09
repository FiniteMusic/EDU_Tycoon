package io.moviles.IPN_Tycoon

import com.kotcrab.vis.ui.VisUI
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.scene2d.Scene2DSkin

class Main(val saveManager: GameSaveManager) : KtxGame<KtxScreen>() {
    override fun create() {
        KtxAsync.initiate()
        VisUI.load()
        Scene2DSkin.defaultSkin = VisUI.getSkin()

        addScreen(Bienvenida(this))
        addScreen(SeleccionPartida(this))
        addScreen(PartidasGuardadas(this))
        addScreen(GameScreen(this))

        setScreen<Bienvenida>()
    }

    override fun dispose() {
        super.dispose()
        VisUI.dispose()
    }
}
