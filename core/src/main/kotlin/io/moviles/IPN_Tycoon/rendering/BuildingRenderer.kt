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
        drawY: Float
    ) {
        batch.draw(
            texture,
            drawX,
            drawY,
            propiedad.renderW,
            propiedad.renderH
        )
    }
}
