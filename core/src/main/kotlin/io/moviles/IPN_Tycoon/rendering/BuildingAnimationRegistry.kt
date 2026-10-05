package io.moviles.IPN_Tycoon.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.assets.toInternalFile

class BuildingAnimationRegistry {

    private val textures = mutableListOf<Texture>()

    private val animationsDelegate = lazy {
        mapOf(
            "escom_hitbox" to createEscomAnimations()
        )
    }

    private val animationsByBuilding: Map<String, List<AnimatedElement>>
        by animationsDelegate

    private fun createEscomAnimations(): List<AnimatedElement> {
        val studentTextures = (1..4).map { frame ->
            Texture(
                "Mapa/animaciones/escom/estudiante_walk_%02d.png"
                    .format(frame)
                    .toInternalFile()
            ).also { textures.add(it) }
        }

        val studentAnimation = BuildingAnimation(
            frames = studentTextures
                .map { TextureRegion(it) }
                .toTypedArray(),
            frameDuration = 0.15f
        )

        return listOf(
            AnimatedElement(
                animation = studentAnimation,
                offsetX = 1400f,
                offsetY = 480f,
                width = 180f,
                height = 240f,
                movementX = 900f,
                movementY = -425f,
                movementDuration = 5f
            )
        )
    }

    fun getElements(buildingId: String): List<AnimatedElement> {
        return animationsByBuilding[buildingId] ?: emptyList()
    }

    fun dispose() {
        if (!animationsDelegate.isInitialized()) {
            return
        }

        textures.forEach { it.dispose() }
        textures.clear()
    }
}
