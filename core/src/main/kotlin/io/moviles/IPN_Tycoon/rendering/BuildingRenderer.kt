package io.moviles.IPN_Tycoon.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import io.moviles.IPN_Tycoon.Propiedad

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

            batch.draw(
                frame,
                drawX + element.offsetX,
                drawY + element.offsetY,
                element.width,
                element.height
            )
        }
    }
}
