package io.moviles.IPN_Tycoon.android

import io.moviles.IPN_Tycoon.GameState
import io.moviles.IPN_Tycoon.BuildConfig
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import io.moviles.IPN_Tycoon.Main
import io.moviles.IPN_Tycoon.android.database.AndroidGameSaveManager
import io.moviles.IPN_Tycoon.android.database.DatabaseProvider
import io.moviles.IPN_Tycoon.data.repositories.EscuelaRepository

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DEBUG: 50 millones. RELEASE: presupuesto económico original.
        GameState.presupuestoInicialPruebasAndroid =
            if (BuildConfig.DEBUG) 50_000_000L else null

        val db          = DatabaseProvider.getDatabase(this)
        val repository  = EscuelaRepository(db.escuelaDao())
        val saveManager = AndroidGameSaveManager(repository)

        initialize(Main(saveManager, modoPruebasAndroid = BuildConfig.DEBUG), AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        })
    }
}
