package io.moviles.IPN_Tycoon.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.moviles.IPN_Tycoon.Propiedad
import ktx.assets.toInternalFile

class BuildingRenderer {

    fun render(
        batch: Batch,
        texture: Texture,
        propiedad: Propiedad,
        drawX: Float,
        drawY: Float,
        stateTime: Float,
        animatedElements: List<AnimatedElement> = emptyList()
    ) {
        // Base estática del edificio
        batch.draw(
            texture,
            drawX,
            drawY,
            propiedad.renderW,
            propiedad.renderH
        )

        // Elementos animados superpuestos
        animatedElements.forEach { element ->
            val frame = element.animation.getFrame(stateTime)

            val duration = element.movementDuration.coerceAtLeast(0.01f)
            val cyclePosition = (stateTime % (duration * 2f)) / duration

            val progress = if (cyclePosition <= 1f) {
                cyclePosition
            } else {
                2f - cyclePosition
            }

            val currentX = element.offsetX + element.movementX * progress
            val currentY = element.offsetY + element.movementY * progress

            batch.draw(
                frame,
                drawX + currentX,
                drawY + currentY,
                element.width,
                element.height
            )
        }
    }


}
