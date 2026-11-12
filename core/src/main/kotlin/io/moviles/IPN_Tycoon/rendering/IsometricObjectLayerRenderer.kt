package io.moviles.IPN_Tycoon.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.TextureMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

/**
 * Renderizador de capas de objetos que imita el draworder de Tiled.
 * - El mapa runtime proporciona _eduSortY (Y original del TMX, sin invertir)
 *   y _eduSortIndex para el orden visual estable.
 * - Se respetan el orden y la visibilidad de las capas originales.
 * - Los edificios comprados se intercalan en Edificios usando el mismo orden.
 * - La geometría y el orden se calculan una sola vez por carga/compra.
 * - No dibuja hitboxes ni puntos lógicos.
 */
class IsometricObjectLayerRenderer(private val map: TiledMap) {

    /** Datos del edificio comprado en las coordenadas de mundo actuales. */
    data class PurchasedSprite(
        val id: String,
        val texture: Texture,
        val worldX: Float,
        val worldY: Float,
        val width: Float,
        val height: Float,
        val rotation: Float,
        val flipX: Boolean,
        val flipY: Boolean,
        val sortY: Float,
        val sortIndex: Int
    )

    private data class DrawEntry(
        val region: TextureRegion,
        val x: Float,
        val y: Float,
        val originX: Float,
        val originY: Float,
        val width: Float,
        val height: Float,
        val scaleX: Float,
        val scaleY: Float,
        val rotation: Float,
        val sortY: Float,
        val sortIndex: Int,
        val left: Float,
        val bottom: Float,
        val right: Float,
        val top: Float,
        val propiedadId: String? = null
    )

    private val cachedLayers = HashMap<String, List<DrawEntry>>()
    private var purchasedSprites: List<DrawEntry> = emptyList()
    private var combinedBuildings: List<DrawEntry>? = null
    private var interiorFocusId: String? = null

    /** Atenúa solo el edificio seleccionado, manteniendo el orden de Tiled. */
    fun setInteriorFocus(propiedadId: String?) { interiorFocusId = propiedadId }

    var drawCount: Int = 0
        private set

    private fun numericProperty(obj: MapObject, name: String, fallback: Float): Float {
        val value = obj.properties.get(name)
        return when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun isPurchasable(obj: MapObject): Boolean {
        val flag = obj.properties.get("edificioComprable")
        if (flag == true || (flag is String && flag.equals("true", true))) return true
        return obj.properties.get("propiedadId") != null
    }

    private fun createEntry(
        region: TextureRegion,
        worldX: Float,
        worldY: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Float,
        sortY: Float,
        sortIndex: Int,
        propiedadId: String? = null
    ): DrawEntry {
        val x = worldX - originX
        val y = worldY - originY
        val angle = Math.toRadians(rotation.toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (xx in floatArrayOf(0f, width)) for (yy in floatArrayOf(0f, height)) {
            val dx = (xx - originX) * scaleX
            val dy = (yy - originY) * scaleY
            val px = worldX + dx * c - dy * s
            val py = worldY + dx * s + dy * c
            minX = min(minX, px)
            minY = min(minY, py)
            maxX = max(maxX, px)
            maxY = max(maxY, py)
        }
        return DrawEntry(region, x, y, originX, originY, width, height,
            scaleX, scaleY, rotation, sortY, sortIndex, minX, minY, maxX, maxY,
            propiedadId)
    }

    private fun prepareLayer(name: String): List<DrawEntry> {
        val layer = map.layers.get(name) ?: return emptyList()
        val entries = ArrayList<DrawEntry>()
        var index = 0
        for (obj in layer.objects) {
            val objectIndex = index++
            if (!obj.isVisible) continue
            if (name == "Edificios" && isPurchasable(obj)) continue
            val region: TextureRegion
            val x: Float
            val y: Float
            val sx: Float
            val sy: Float
            val rotation: Float
            val ox: Float
            val oy: Float
            when (obj) {
                is TiledMapTileMapObject -> {
                    region = obj.textureRegion ?: continue
                    x = obj.x; y = obj.y
                    sx = obj.scaleX; sy = obj.scaleY
                    rotation = -obj.rotation
                    ox = region.regionWidth * 0.5f
                    oy = 0f
                }
                is TextureMapObject -> {
                    region = obj.textureRegion ?: continue
                    x = obj.x; y = obj.y
                    sx = obj.scaleX; sy = obj.scaleY
                    rotation = -obj.rotation
                    ox = obj.originX; oy = obj.originY
                }
                else -> continue
            }
            val wx = x + y
            val wy = (y - x) * 0.5f
            // Valor serializado en TMX; el loader puede invertir la coordenada Y.
            val sortY = numericProperty(obj, "_eduSortY", -y)
            val sortIndex = numericProperty(obj, "_eduSortIndex", objectIndex.toFloat()).toInt()
            entries.add(createEntry(region, wx, wy, ox, oy,
                region.regionWidth.toFloat(), region.regionHeight.toFloat(),
                sx, sy, rotation, sortY, sortIndex))
        }
        return sortForLayer(name, entries)
    }

    private fun sortForLayer(name: String, list: List<DrawEntry>): List<DrawEntry> {
        val drawOrder = (map.layers.get(name)?.properties?.get("_eduDrawOrder") as? String)
            ?: "topdown"
        return if (drawOrder == "index") list.sortedBy { it.sortIndex }
        else list.sortedWith(compareBy<DrawEntry>({ it.sortY }, { it.sortIndex }))
    }

    /** Se ejecuta al cargar el escenario y después de comprar/mejorar un edificio. */
    fun setPurchasedSprites(sprites: List<PurchasedSprite>) {
        purchasedSprites = sprites.map { sprite ->
            val region = TextureRegion(sprite.texture)
            if (sprite.flipX || sprite.flipY) region.flip(sprite.flipX, sprite.flipY)
            val w = region.regionWidth.toFloat()
            val h = region.regionHeight.toFloat()
            createEntry(region, sprite.worldX, sprite.worldY,
                w * 0.5f, 0f, w, h,
                sprite.width / w, sprite.height / h,
                sprite.rotation, sprite.sortY, sprite.sortIndex, sprite.id)
        }
        combinedBuildings = null
    }

    private fun getLayerEntries(name: String): List<DrawEntry> {
        val static = cachedLayers.getOrPut(name) { prepareLayer(name) }
        if (name != "Edificios") return static
        val cached = combinedBuildings
        if (cached != null) return cached
        val combined = ArrayList<DrawEntry>(static.size + purchasedSprites.size)
        combined.addAll(static)
        combined.addAll(purchasedSprites)
        return sortForLayer(name, combined).also { combinedBuildings = it }
    }

    /** Recorre las capas en el orden que Tiled guardó, sin reordenarlas. */
    fun render(batch: Batch, camera: OrthographicCamera, caminantes: ((Batch) -> Unit)? = null) {
        var alumnosPintados = false // EDU_PASO5_PROFUNDIDAD
        val hw = camera.viewportWidth * camera.zoom * 0.5f
        val hh = camera.viewportHeight * camera.zoom * 0.5f
        val left = camera.position.x - hw
        val right = camera.position.x + hw
        val bottom = camera.position.y - hh
        val top = camera.position.y + hh
        drawCount = 0

        batch.projectionMatrix = camera.combined
        batch.begin()
        try {
            batch.color = Color.WHITE
            for (layer in map.layers) {
                if (!layer.isVisible || layer is TiledMapTileLayer) continue
                val name = layer.name ?: continue
                if (name == "Puntos_origen" || name == "Puntos_Nombres" ||
                    name == "Puntos_Render_Edificios") continue
                for (e in getLayerEntries(name)) {
                    if (e.right < left || e.left > right || e.top < bottom || e.bottom > top) continue
                    val focus = e.propiedadId != null && e.propiedadId == interiorFocusId
                    if (focus) batch.setColor(1f, 1f, 1f, 0.42f)
                    batch.draw(e.region, e.x, e.y, e.originX, e.originY,
                        e.width, e.height, e.scaleX, e.scaleY, e.rotation)
                    if (focus) batch.setColor(1f, 1f, 1f, 1f)
                    drawCount++
                }
                // Tiled mantiene Arboles_arbustos y Edificios por encima de Camino.
                if (name == "Camino" && !alumnosPintados) {
                    caminantes?.invoke(batch)
                    alumnosPintados = true
                }
            }
            if (!alumnosPintados) caminantes?.invoke(batch)
        } finally {
            batch.color = Color.WHITE
            batch.end()
        }
    }

    /**
     * Límites visuales del campus en coordenadas del renderizador. Se calculan
     * desde la geometría ya precalculada, SIN ordenar por fotograma ni cargar
     * nuevos PNG. También se incluyen los edificios bloqueados (sus anclajes
     * no contienen texturas, solo dimensiones y ubicación).
     */
    fun getVisualBounds(out: com.badlogic.gdx.math.Rectangle): Boolean {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        fun extend(x0: Float, y0: Float, x1: Float, y1: Float) {
            minX = kotlin.math.min(minX, x0)
            minY = kotlin.math.min(minY, y0)
            maxX = kotlin.math.max(maxX, x1)
            maxY = kotlin.math.max(maxY, y1)
        }
        for (name in arrayOf("Camino", "Edificios", "Arboles_arbustos", "Logica_Clics")) {
            if (map.layers.get(name) == null) continue
            for (e in getLayerEntries(name)) {
                extend(e.left, e.bottom, e.right, e.top)
            }
        }
        map.layers.get("Puntos_Render_Edificios")?.objects?.forEach { obj ->
            if (obj is com.badlogic.gdx.maps.objects.PointMapObject) {
                val x = obj.point.x + obj.point.y
                val y = (obj.point.y - obj.point.x) * 0.5f
                val w = (obj.properties.get("renderW") as? Number)?.toFloat() ?: 0f
                val h = (obj.properties.get("renderH") as? Number)?.toFloat() ?: 0f
                if (w > 0f && h > 0f) extend(x - w * 0.5f, y, x + w * 0.5f, y + h)
            }
        }
        if (!minX.isFinite() || !minY.isFinite() || !maxX.isFinite() || !maxY.isFinite())
            return false
        out.set(minX, minY, maxX - minX, maxY - minY)
        return true
    }

    /** Usar solo si se cambian objetos de Tiled en ejecución. */
    fun invalidate() {
        cachedLayers.clear()
        combinedBuildings = null
    }
}
