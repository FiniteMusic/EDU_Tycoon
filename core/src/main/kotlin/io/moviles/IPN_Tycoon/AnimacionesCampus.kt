package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs

/**
 * EDU_ANIMACIONES_CAMPUS_V4. Solo animación visual; sin colisiones ni audio.
 * - Automóviles: avenida EXTERIOR identificada por asfalto, nunca por Camino peatonal.
 * - Aves: vuelan sobre el campus, no ocupan rutas de alumnos.
 * - Helicópteros: visita alternada a Dirección General y Medicina.
 * No crea ni modifica ningún objeto Tiled; solo consume la geometría existente.
 */
class AnimacionesCampus {
    private val cacheCoches = lazy { Texture(Gdx.files.internal("sprites/coches_campus.png")) }
    private val texturaCoches by cacheCoches
    private val cacheAves = lazy { Texture(Gdx.files.internal("sprites/aves_campus.png")) }
    private val texturaAves by cacheAves
    private val cacheHelis = lazy { Texture(Gdx.files.internal("sprites/helicopteros_campus.png")) }
    private val texturaHelis by cacheHelis
    private val coches by lazy { Array(5) { TextureRegion(texturaCoches, it * 72, 0, 72, 40) } }
    private val aves by lazy { Array(4) { TextureRegion(texturaAves, it * 32, 0, 32, 24) } }
    private val helis by lazy { Array(4) { TextureRegion(texturaHelis, it * 96, 0, 96, 60) } }
    private var tiempo = 0f
    private var calleA: Pair<Vector2,Vector2>? = null
    private var calleB: Pair<Vector2,Vector2>? = null
    private var minCampus = 0f
    private var maxCampus = 18000f
    private var centroCampusY = 0f
    private var direccion: Vector2? = null
    private var medicina: Vector2? = null

    fun configurar(mapa: TiledMap?, ubicaciones: Map<String,Vector2>) {
        tiempo = 0f
        direccion = ubicaciones["Direccion"]?.cpy()
        medicina = ubicaciones["Edificio1"]?.cpy()
        if (ubicaciones.isNotEmpty()) {
            minCampus = ubicaciones.values.minOf { it.x }
            maxCampus = ubicaciones.values.maxOf { it.x }
            centroCampusY = ubicaciones.values.map { it.y }.average().toFloat()
        }
        val asfalto = mapa?.layers?.get("Camino")?.objects
            ?.filterIsInstance<TiledMapTileMapObject>()
            ?.filter { it.tile.id == 144 && it.x in 550f..15300f && abs(it.y) <= 650f }
            ?: emptyList()
        // La avenida perimetral tiene dos carriles y está junto al borde sur.
        val lane1 = asfalto.filter { it.y >= 0f }.sortedBy { it.x }
        val lane2 = asfalto.filter { it.y < 0f }.sortedBy { it.x }
        fun punto(obj: TiledMapTileMapObject): Vector2 =
            Vector2(obj.x + obj.y, (obj.y - obj.x) * 0.5f)
        fun extremos(lista:List<TiledMapTileMapObject>):Pair<Vector2,Vector2>? {
            if (lista.size<7) return null
            return punto(lista.first()) to punto(lista.last())
        }
        calleA = extremos(lane1)
        calleB = extremos(lane2) ?: calleA?.let { (a,b) ->
            // Dos carriles separados; desplazamiento perpendicular en mundo.
            Vector2(a.x-40f,a.y-90f) to Vector2(b.x-40f,b.y-90f)
        }
        // Prioridad: avenida compilada directamente de Mapa_General.tmx, para
        // no depender del flip Y del TmxMapLoader de cada versión de libGDX.
        val archivo = Gdx.files.internal("Mapa/avenida_autos.txt")
        if (archivo.exists()) {
            try {
                val lineas = archivo.readString("UTF-8").lineSequence()
                    .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
                val leidas = lineas.mapNotNull { linea ->
                    val puntos = linea.split(';').mapNotNull { par ->
                        val v = par.split(',')
                        if (v.size != 2) null else {
                            val x = v[0].toFloatOrNull()
                            val y = v[1].toFloatOrNull()
                            if (x != null && y != null && x.isFinite() && y.isFinite())
                                Vector2(x,y) else null
                        }
                    }
                    if (puntos.size == 2 && puntos[0].dst(puntos[1]) > 1000f)
                        puntos[0] to puntos[1] else null
                }
                if (leidas.size >= 2) {
                    calleA = leidas[0]
                    calleB = leidas[1]
                }
            } catch (e: Exception) {
                Gdx.app.error("EDU_VEHICULOS", "Avenida exterior ilegible", e)
            }
        }
        Gdx.app.log("EDU_VEHICULOS", "Pistas asfalto exterior=${asfalto.size}, carriles=${(if(calleA != null)1 else 0)+(if(calleB != null)1 else 0)}")
    }

    /** Llamar una única vez por fotograma, incluso si la cámara no muestra la avenida. */
    fun actualizar(delta:Float) { tiempo += delta.coerceIn(0f,0.20f) }

    private fun visible(camera:OrthographicCamera,x:Float,y:Float,margen:Float):Boolean {
        val hw=camera.viewportWidth*camera.zoom*0.55f
        val hh=camera.viewportHeight*camera.zoom*0.55f
        return abs(x-camera.position.x) <= hw+margen && abs(y-camera.position.y) <= hh+margen
    }

    /** Requiere Batch.begin() activado por GameScreen; no modifica el estado del Batch. */
    fun dibujar(batch:Batch,camera:OrthographicCamera) {
        val anterior=batch.color.cpy()
        batch.setColor(1f,1f,1f,1f)
        try {
            // Tráfico. Ocho automóviles, velocidades distintas, sin alumnos sobre avenida.
            for (i in 0 until 8) {
                val extremos = if (i%2==0) calleA else calleB
                val (a,b)=extremos ?: continue
                val sentido=i%2==0
                val total=if(sentido) a.dst(b) else b.dst(a)
                val u=((tiempo*(65f+6f*(i%3))/total.coerceAtLeast(1f) + i/8f)%1f)
                val origen=if(sentido) a else b
                val destino=if(sentido) b else a
                val x=MathUtils.lerp(origen.x,destino.x,u)
                val y=MathUtils.lerp(origen.y,destino.y,u)
                if (!visible(camera,x,y,260f)) continue
                batch.draw(coches[i%coches.size],x-85f,y-24f,170f,94f)
            }

            // Aves pequeñas, sin sonido. Se dibujan por encima de todas las capas.
            val amplitud=(maxCampus-minCampus).coerceAtLeast(4500f)
            for (i in 0 until 7) {
                val u=((tiempo*(24f+i*2f)/amplitud+i/7f)%1f)
                val x=minCampus+amplitud*u
                val y=centroCampusY+550f+(i%4)*410f+85f*MathUtils.sin(tiempo*.7f+i)
                if (!visible(camera,x,y,150f)) continue
                val frame=((tiempo*5f+i).toInt()%4+4)%4
                batch.draw(aves[frame],x-45f,y-24f,90f,68f)
            }

            // Helicópteros alternados. 0-25 llegada, 25-42 descenso,
            // 42-58 estancia, 58-75 ascenso, 75-100 salida, resto pausa.
            for ((i,anclaje) in listOf(direccion,medicina).withIndex()) {
                val roof=anclaje ?: continue
                val fase=(tiempo+i*78f)%156f
                if (fase>=105f)continue
                val x:Float
                val elevacion:Float
                when {
                    fase<25f -> {
                        val u=fase/25f
                        x=roof.x+MathUtils.lerp(-3300f,0f,u)
                        elevacion=2600f
                    }
                    fase<42f -> {
                        x=roof.x
                        elevacion=MathUtils.lerp(2600f,1400f,(fase-25f)/17f)
                    }
                    fase<58f -> {
                        x=roof.x
                        elevacion=1400f+16f*MathUtils.sin(fase*1.5f)
                    }
                    fase<75f -> {
                        x=roof.x
                        elevacion=MathUtils.lerp(1400f,2600f,(fase-58f)/17f)
                    }
                    else -> {
                        x=roof.x+MathUtils.lerp(0f,3200f,(fase-75f)/30f)
                        elevacion=2600f
                    }
                }
                val y=roof.y+elevacion
                if (!visible(camera,x,y,340f)) continue
                val frame=((tiempo*12f).toInt()%4+4)%4
                batch.draw(helis[frame],x-165f,y-77f,330f,206f)
            }
        } finally { batch.color=anterior }
    }

    fun dispose() {
        if (cacheCoches.isInitialized()) texturaCoches.dispose()
        if (cacheAves.isInitialized()) texturaAves.dispose()
        if (cacheHelis.isInitialized()) texturaHelis.dispose()
    }
}
