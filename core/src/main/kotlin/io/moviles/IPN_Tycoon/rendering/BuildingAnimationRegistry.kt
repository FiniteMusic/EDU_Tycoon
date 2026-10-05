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

            // Nivel 1
            AnimatedElement(
                animation = studentAnimation,
                offsetX = 1400f,
                offsetY = 480f,
                width = 180f,
                height = 240f,
                movementX = 600f,
                movementY = -300f,
                movementDuration = 5f
            ),

            // Nivel 2
            AnimatedElement(
                animation = studentAnimation,
                offsetX = 1300f,
                offsetY = 410f,
                width = 180f,
                height = 240f,
                movementX = 650f,
                movementY = -300f,
                movementDuration = 6f
            ),

            // Nivel 3
            AnimatedElement(
                animation = studentAnimation,
                offsetX = 1750f,
                offsetY = 300f,
                width = 180f,
                height = 240f,
                movementX = 600f,
                movementY = -280f,
                movementDuration = 4.5f
            )
        )
    }

    fun getElements(
        buildingId: String,
        level: Int
    ): List<AnimatedElement> {

        val elements = animationsByBuilding[buildingId] ?: return emptyList()

        return elements.take(
            level.coerceIn(0, elements.size)
        )
    }

    fun dispose() {
        if (!animationsDelegate.isInitialized()) {
            return
        }

        textures.forEach { it.dispose() }
        textures.clear()
    }
}
