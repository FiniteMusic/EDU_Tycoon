package io.moviles.IPN_Tycoon.rendering

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion

class BuildingAnimation(
    frames: Array<TextureRegion>,
    frameDuration: Float,
    playMode: Animation.PlayMode = Animation.PlayMode.LOOP
) {

    private val animation = Animation(frameDuration, *frames).apply {
        this.playMode = playMode
    }

    fun getFrame(stateTime: Float): TextureRegion {
        return animation.getKeyFrame(stateTime)
    }
}
