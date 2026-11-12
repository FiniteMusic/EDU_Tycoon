package io.moviles.IPN_Tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * HOTFIX_PASO5_V3_NPC
 * Rutas fiables: dibujar polilíneas en la capa Tiled "Rutas_Alumnos".
 * Compatibilidad: sin esa capa se usan SOLO los pisos peatonales de "Camino",
 * no asfalto, bardas, jardineras ni mobiliario. Las rutas de Tiled tienen prioridad.
 */
class CaminantesCampus {
    private data class Alumno(
        var nodo: Int,
        var siguiente: Int,
        var avance: Float,
        val fila: Int,
        val fase: Float,
        val velocidad: Float,
        var tramos: Int = 0
    )

    private val texturaCache = lazy {
        Texture(Gdx.files.internal("sprites/personajes_caminar.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }
    private val textura: Texture by texturaCache
    private val fotogramas by lazy {
        Array(4) { fila -> Array(4) { frame ->
            TextureRegion(textura, frame * 32, fila * 48, 32, 48)
        } }
    }
    private val puntos = ArrayList<Vector2>()
    private val vecinos = ArrayList<MutableSet<Int>>()
    private val alumnos = ArrayList<Alumno>()
    private val rutasGuardadas = ArrayList<List<Int>>()
    private var reloj = 0f

    private fun nuevoNodo(x: Float, y: Float): Int {
        val indice = puntos.size
        puntos.add(Vector2(x, y))
        vecinos.add(linkedSetOf())
        return indice
    }

    private fun unir(a: Int, b: Int) {
        if (a == b) return
        vecinos[a].add(b)
        vecinos[b].add(a)
    }

    /**
     * La geometría se genera desde Mapa_General.tmx, pero se empaqueta como
     * lista de puntos en coordenadas mundo para NO modificar los TMX.
     * Es preferible a la heurística de centros de losetas que situaba alumnos
     * sobre bardas o entre las fachadas.
     */
    private fun usarRutasGuardadas(): Boolean {
        val archivo = Gdx.files.internal("Mapa/rutas_alumnos.txt")
        if (!archivo.exists()) return false
        try {
            for (linea in archivo.readString("UTF-8").lineSequence()) {
                val limpia = linea.trim()
                if (limpia.isBlank() || limpia.startsWith("#")) continue
                val indices = ArrayList<Int>()
                for (par in limpia.split(';')) {
                    val v = par.split(',')
                    if (v.size != 2) continue
                    val x = v[0].toFloatOrNull() ?: continue
                    val y = v[1].toFloatOrNull() ?: continue
                    if (x.isFinite() && y.isFinite()) indices.add(nuevoNodo(x, y))
                }
                if (indices.size < 2) continue
                for (i in 0 until indices.size - 1) unir(indices[i], indices[i + 1])
                rutasGuardadas.add(indices)
            }
        } catch (e: Exception) {
            Gdx.app.error("EDU_NPC", "Rutas guardadas inválidas", e)
            return false
        }
        return rutasGuardadas.isNotEmpty()
    }

    /** Las polilíneas se unen exclusivamente por sus puntos consecutivos. */
    private fun usarRutasExplicitas(mapa: TiledMap?): Boolean {
        val capa = mapa?.layers?.get("Rutas_Alumnos") ?: return false
        for (obj in capa.objects) {
            val linea = (obj as? PolylineMapObject)?.polyline ?: continue
            val vertices = linea.transformedVertices
            if (vertices.size < 4) continue
            var anterior = -1
            for (i in 0 until vertices.size / 2) {
                val tx = vertices[i * 2]
                val ty = vertices[i * 2 + 1]
                val wx = tx + ty
                val wy = (ty - tx) * 0.5f
                if (!wx.isFinite() || !wy.isFinite()) continue
                val actual = nuevoNodo(wx, wy)
                if (anterior >= 0) unir(anterior, actual)
                anterior = actual
            }
        }
        return vecinos.any { it.isNotEmpty() }
    }

    /** IDs globales del primer tileset calles.tsx (firstgid=1). */
    private val suelosPeatonales = setOf(111, 143, 155) // solo pavimento interior
    private val objetosValla = setOf(149, 151) // vallasd y vallasd1

    private fun world(obj: TiledMapTileMapObject): Vector2 = Vector2(
        obj.x + obj.y,
        (obj.y - obj.x) * 0.5f
    )

    /**
     * Fallback: extrae centros de superficies caminables de "Camino".
     * Los anclajes de asfalto (GID 144), bardas (149/151) y mobiliario
     * nunca entran a la red, evitando el error visual mostrado.
     */
    private fun usarPavimento(mapa: TiledMap?) {
        val objetos = mapa?.layers?.get("Camino")?.objects ?: return
        val vallas = objetos.asSequence()
            .filterIsInstance<TiledMapTileMapObject>()
            .filter { it.tile.id in objetosValla }
            .map { world(it) }
            .toList()

        for (obj in objetos) {
            val piso = obj as? TiledMapTileMapObject ?: continue
            if (piso.tile.id !in suelosPeatonales) continue
            val posicion = world(piso)
            val alto = abs(piso.scaleY) * piso.textureRegion.regionHeight
            // El anclaje de un objeto gráfico suele estar en el borde inferior;
            // desplazar la pisada hacia DENTRO del pavimento, no sobre la barda.
            posicion.y += alto * 0.25f
            if (vallas.any { it.dst2(posicion) < 240f * 240f }) continue
            if (posicion.x.isFinite() && posicion.y.isFinite()) {
                nuevoNodo(posicion.x, posicion.y)
            }
        }

        if (puntos.size < 2) return
        // Puentes cortos entre losas; no unir caminos opuestos atravesando edificios.
        for (i in puntos.indices) {
            val proximos = puntos.indices.asSequence()
                .filter { it != i }
                .map { it to puntos[i].dst2(puntos[it]) }
                .filter { (_, d) -> d in (105f * 105f)..(790f * 790f) }
                .sortedBy { it.second }
                .take(4)
            for ((j, _) in proximos) {
                val centro = Vector2(
                    (puntos[i].x + puntos[j].x) * 0.5f,
                    (puntos[i].y + puntos[j].y) * 0.5f
                )
                if (vallas.none { it.dst2(centro) < 210f * 210f }) unir(i, j)
            }
        }
    }

    fun configurar(mapa: TiledMap?) {
        puntos.clear(); vecinos.clear(); alumnos.clear(); rutasGuardadas.clear(); reloj = 0f
        val manual = usarRutasGuardadas() || usarRutasExplicitas(mapa)
        if (!manual) {
            puntos.clear(); vecinos.clear(); rutasGuardadas.clear()
            usarPavimento(mapa)
        }
        val elegibles = puntos.indices.filter { vecinos[it].isNotEmpty() }
        if (elegibles.isEmpty()) {
            Gdx.app.log("EDU_NPC", "Sin rutas peatonales; verifica Camino o Rutas_Alumnos")
            return
        }

        // Muestreo espacial: repartir alumnos y no concentrarlos en una puerta.
        val objetivo = if (rutasGuardadas.isNotEmpty())
            (rutasGuardadas.size * 4).coerceIn(20, 40)
        else (elegibles.size / 4).coerceIn(14, 44)
        val orden = elegibles.shuffled(Random(1293))
        val inicios = ArrayList<Int>()
        if (rutasGuardadas.isNotEmpty()) {
            // Primero repartir alumnos entre corredores, antes de llenar rutas.
            for ((i, ruta) in rutasGuardadas.withIndex()) {
                val disponibles = ruta.filter { vecinos[it].isNotEmpty() }
                for (j in 0 until minOf(4, disponibles.size)) {
                    val nodo = disponibles[(j * disponibles.size / 4 + i) % disponibles.size]
                    if (nodo !in inicios) inicios.add(nodo)
                }
            }
        }
        for (minimo in floatArrayOf(1050f, 720f, 440f, 240f)) {
            val minimo2 = minimo * minimo
            for (nodo in orden) {
                if (inicios.size >= objetivo) break
                if (inicios.all { puntos[it].dst2(puntos[nodo]) > minimo2 }) inicios.add(nodo)
            }
            if (inicios.size >= objetivo) break
        }
        inicios.take(objetivo).forEachIndexed { i, nodo ->
            val salidas = vecinos[nodo].toList()
            if (salidas.isNotEmpty()) {
                alumnos.add(Alumno(nodo, salidas[i % salidas.size],
                    (i % 9) / 10f, i % 4, i * 0.19f, 59f + (i % 8) * 4f))
            }
        }
        fotogramas.size
        Gdx.app.log("EDU_NPC", "${alumnos.size} alumnos dispersos; " +
            "${puntos.size} nodos, rutas explícitas=$manual")
    }

    private fun siguienteNodo(actual: Int, anterior: Int, tramos: Int): Int {
        val opciones = vecinos[actual].filter { it != anterior }
        if (opciones.isEmpty()) return anterior
        val entrada = puntos[actual].cpy().sub(puntos[anterior]).nor()
        val alineados = opciones.sortedByDescending { destino ->
            val salida = puntos[destino].cpy().sub(puntos[actual]).nor()
            entrada.dot(salida)
        }
        // Varios tramos en la misma dirección antes de girar intencionalmente.
        return if (tramos % 11 == 0 && alineados.size > 1) alineados[1]
               else alineados.first()
    }

    /** Batch proporcionado por IsometricObjectLayerRenderer: NO llamar begin/end. */
    fun dibujarEnBatch(batch: Batch, camera: OrthographicCamera, delta: Float) {
        if (alumnos.isEmpty()) return
        val dt = delta.coerceIn(0f, 0.15f)
        reloj += dt
        val halfW = camera.viewportWidth * camera.zoom * 0.54f
        val halfH = camera.viewportHeight * camera.zoom * 0.54f
        val colorAnterior = batch.color.cpy()
        batch.setColor(1f, 1f, 1f, 1f)
        try {
            for (a in alumnos) {
                var inicio = puntos[a.nodo]
                var fin = puntos[a.siguiente]
                val longitud = sqrt(inicio.dst2(fin)).coerceAtLeast(1f)
                a.avance += dt * a.velocidad / longitud
                if (a.avance >= 1f) {
                    val previo = a.nodo
                    a.nodo = a.siguiente
                    a.tramos++
                    a.siguiente = siguienteNodo(a.nodo, previo, a.tramos)
                    a.avance = (a.avance - 1f).coerceIn(0f, 0.60f)
                    inicio = puntos[a.nodo]
                    fin = puntos[a.siguiente]
                }
                val x = MathUtils.lerp(inicio.x, fin.x, a.avance)
                val y = MathUtils.lerp(inicio.y, fin.y, a.avance)
                if (abs(x - camera.position.x) > halfW + 180f ||
                    abs(y - camera.position.y) > halfH + 245f) continue
                val frame = ((reloj * 7f + a.fase).toInt() % 4 + 4) % 4
                // Un 18% más grandes que Paso 5, con la pisada en el camino.
                batch.draw(fotogramas[a.fila][frame], x - 71f, y - 11f, 142f, 213f)
            }
        } finally { batch.color = colorAnterior }
    }

    fun dispose() { if (texturaCache.isInitialized()) textura.dispose() }
}
