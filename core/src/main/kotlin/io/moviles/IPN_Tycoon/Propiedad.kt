package io.moviles.IPN_Tycoon

/**
 * Propiedad económica y visual de un edificio.
 *
 * texturePrefix: prefijo EXACTO del PNG exterior en assets/Mapa/Edificios/.
 *   GameScreen.kt forma: "${texturePrefix}lvl${nivel}.png"
 * interiorTexturePrefix: prefijo del PNG interior, reservado para futuras
 *   pantallas interiores (el GameScreen actual NO lo renderiza).
 * renderW/renderH: unidades del mundo de libGDX, no tamaño intrínseco del PNG.
 */
data class Propiedad(
    val id: String,
    val nombre: String,
    val precio: Long,
    val descripcion: String,
    val capacidad: Int,
    val baseAlumnos: Int,
    val mejoraMax: Int,
    val texturePrefix: String? = null,
    val renderW: Float = 3728f,
    val renderH: Float = 1968f,
    var nivel: Int = 0,
    var comprada: Boolean = false,
    val interiorTexturePrefix: String? = null
)

object PropiedadRepository {

    /** Multiplicador global actual, conservado del proyecto. */
    private const val ESCALA = 1.1f

    /**
     * Claves existentes conservadas para compatibilidad con hitboxes y guardados.
     * Las propiedades NUEVAS requieren objetos en las capas de Tiled:
     * Logica_Clics (rectángulos), Puntos_origen (puntos), Puntos_Nombres (puntos).
     */
    val propiedades: Map<String, Propiedad> = mapOf(
        // ── ESCOM / COMPUTACIÓN ──
        "escom_hitbox" to Propiedad(
            id = "escom_hitbox",
            nombre = "Escuela de Computación",
            precio = 300_000L,
            descripcion = "Escuela Superior de Cómputo",
            capacidad = 3200,
            baseAlumnos = 600,
            mejoraMax = 3,
            texturePrefix = "computo",
            renderW = 3728f * ESCALA,
            renderH = 1968f * ESCALA,
            interiorTexturePrefix = "computointerior"
        ),

        // ── DIRECCIÓN GENERAL ──
        "Direccion" to Propiedad(
            id = "Direccion",
            nombre = "Dirección General",
            precio = 5_000_000L,
            descripcion = "Oficinas administrativas del IPN",
            capacidad = 1000,
            baseAlumnos = 200,
            mejoraMax = 4,
            texturePrefix = "Direccion",
            renderW = 6112f * ESCALA,
            renderH = 5464f * ESCALA,
            interiorTexturePrefix = "Direccioninterior"
        ),

        // ── COMIDA — conservar ID antiguo ──
        "Mac_and_cheese" to Propiedad(
            id = "Mac_and_cheese",
            nombre = "Mac and Cheese",
            precio = 120_000L,
            descripcion = "Puesto de comida rápida",
            capacidad = 200,
            baseAlumnos = 50,
            mejoraMax = 3,
            texturePrefix = "comida",
            renderW = 2188f * ESCALA,
            renderH = 1180f * ESCALA,
            interiorTexturePrefix = "comidainterior"
        ),

        // ── RESTAURANTE — conservar ID de cafetería ──
        "cafeteria" to Propiedad(
            id = "cafeteria",
            nombre = "Cafetería",
            precio = 150_000L,
            descripcion = "Restaurante universitario",
            capacidad = 500,
            baseAlumnos = 100,
            mejoraMax = 3,
            texturePrefix = "Restaurante",
            renderW = 2188f * ESCALA,
            renderH = 1180f * ESCALA,
            interiorTexturePrefix = "Restauranteinterior"
        ),

        // ── AUDITORIO ──
        "auditorio" to Propiedad(
            id = "auditorio",
            nombre = "Auditorio Alejo Peralta",
            precio = 800_000L,
            descripcion = "Eventos y conferencias",
            capacidad = 2000,
            baseAlumnos = 400,
            mejoraMax = 3,
            texturePrefix = "auditorio",
            renderW = 5312f * ESCALA,
            renderH = 4688f * ESCALA,
            interiorTexturePrefix = "Auditoriointerior"
        ),

        // ── ARQUITECTURA ──
        "Arquitectura" to Propiedad(
            id = "Arquitectura",
            nombre = "Arquitectura",
            precio = 1_100_000L,
            descripcion = "Escuela de construcción y diseño",
            capacidad = 2500,
            baseAlumnos = 500,
            mejoraMax = 3,
            texturePrefix = "Arquitectura",
            renderW = 4000f * ESCALA,
            renderH = 2800f * ESCALA,
            interiorTexturePrefix = "Arquitecturainterior"
        ),

        // ── BIOLÓGICAS ──
        "Biologicas" to Propiedad(
            id = "Biologicas",
            nombre = "Biológicas",
            precio = 1_250_000L,
            descripcion = "Ciencias biológicas",
            capacidad = 2800,
            baseAlumnos = 550,
            mejoraMax = 3,
            texturePrefix = "Biologicas",
            renderW = 5280f * ESCALA,
            renderH = 3164f * ESCALA,
            interiorTexturePrefix = "biologicasinterior"
        ),

        // ── BIOQUÍMICA ──
        "Bioquimica" to Propiedad(
            id = "Bioquimica",
            nombre = "Bioquímica",
            precio = 1_250_000L,
            descripcion = "Ciencias bioquímicas",
            capacidad = 2800,
            baseAlumnos = 550,
            mejoraMax = 3,
            texturePrefix = "Bioquimica",
            renderW = 5132f * ESCALA,
            renderH = 3363f * ESCALA,
            interiorTexturePrefix = "Bioquimicainterior"
        ),

        // ── MATEMÁTICAS ──
        "Matematicas" to Propiedad(
            id = "Matematicas",
            nombre = "Matemáticas",
            precio = 1_000_000L,
            descripcion = "Física y Matemáticas",
            capacidad = 1500,
            baseAlumnos = 300,
            mejoraMax = 3,
            texturePrefix = "Matematicas",
            renderW = 4500f * ESCALA,
            renderH = 2500f * ESCALA,
            interiorTexturePrefix = "Matematicasinterior"
        ),

        // ── MEDICINA — conservar ID Edificio1 ──
        "Edificio1" to Propiedad(
            id = "Edificio1",
            nombre = "Escuela de Medicina",
            precio = 400_000L,
            descripcion = "Formación médica y ciencias de la salud",
            capacidad = 1200,
            baseAlumnos = 250,
            mejoraMax = 3,
            texturePrefix = "medicina",
            renderW = 4148f * ESCALA,
            renderH = 3528f * ESCALA,
            interiorTexturePrefix = "Medicinainterior"
        ),

        // ── CONSULTORIO — conservar ID Edificio2 ──
        "Edificio2" to Propiedad(
            id = "Edificio2",
            nombre = "Consultorio Universitario",
            precio = 400_000L,
            descripcion = "Atención y prácticas clínicas",
            capacidad = 1200,
            baseAlumnos = 250,
            mejoraMax = 3,
            texturePrefix = "Consultorio",
            renderW = 4144f * ESCALA,
            renderH = 3456f * ESCALA,
            interiorTexturePrefix = "Consultoriointerior"
        ),

        // ── MUSEO ──
        "Museo" to Propiedad(
            id = "Museo",
            nombre = "Museo Tezozómoc",
            precio = 600_000L,
            descripcion = "Ciencia y tecnología",
            capacidad = 800,
            baseAlumnos = 150,
            mejoraMax = 3,
            texturePrefix = "Museo",
            renderW = 6800f * ESCALA,
            renderH = 4200f * ESCALA,
            interiorTexturePrefix = "museointerior"
        ),

        // ── TURISMO — prefijo minúsculo en PNG nuevos ──
        "Turismo" to Propiedad(
            id = "Turismo",
            nombre = "Escuela de Turismo",
            precio = 900_000L,
            descripcion = "Formación en turismo y hospitalidad",
            capacidad = 2200,
            baseAlumnos = 450,
            mejoraMax = 3,
            texturePrefix = "turismo",
            renderW = 3000f * ESCALA,
            renderH = 2000f * ESCALA,
            interiorTexturePrefix = "turismointerior"
        ),

        // ── PALAPAS ──
        "palapas" to Propiedad(
            id = "palapas",
            nombre = "Palapas",
            precio = 350_000L,
            descripcion = "Área de descanso y convivencia",
            capacidad = 300,
            baseAlumnos = 60,
            mejoraMax = 2,
            texturePrefix = "palapas",
            renderW = 1997f * ESCALA,
            renderH = 1069f * ESCALA,
            interiorTexturePrefix = null
        ),

        // ── NUEVO: BIBLIOTECA — dimensiones y economía propuestas ──
        "Biblioteca" to Propiedad(
            id = "Biblioteca",
            nombre = "Biblioteca Central",
            precio = 1_500_000L,
            descripcion = "Estudio, investigación y adquisición de conocimiento",
            capacidad = 1800,
            baseAlumnos = 350,
            mejoraMax = 3,
            texturePrefix = "Biblioteca",
            renderW = 5200f * ESCALA,
            renderH = 3500f * ESCALA,
            interiorTexturePrefix = "Bibliotecainterior"
        ),

        // ── NUEVO: ARTE, DISEÑO Y DEPORTE — prefijo termina en _ ──
        "Arte_diseno_deporte" to Propiedad(
            id = "Arte_diseno_deporte",
            nombre = "Arte, Diseño y Deporte",
            precio = 1_400_000L,
            descripcion = "Actividades creativas, artísticas y deportivas",
            capacidad = 2200,
            baseAlumnos = 420,
            mejoraMax = 3,
            texturePrefix = "Arte_diseño_deporte_",
            renderW = 4500f * ESCALA,
            renderH = 3000f * ESCALA,
            interiorTexturePrefix = "ADDinterior"
        ),

        // ── NUEVO: DORMITORIOS — dimensiones y economía propuestas ──
        "Dormitorios" to Propiedad(
            id = "Dormitorios",
            nombre = "Dormitorios Universitarios",
            precio = 1_800_000L,
            descripcion = "Alojamiento y descanso de estudiantes",
            capacidad = 2400,
            baseAlumnos = 450,
            mejoraMax = 3,
            texturePrefix = "Dormitorios",
            renderW = 4600f * ESCALA,
            renderH = 3100f * ESCALA,
            interiorTexturePrefix = "Dormitoriosinterior"
        ),

        // ── NUEVO: GASTRONOMÍA — dimensiones y economía propuestas ──
        "Gastronomia" to Propiedad(
            id = "Gastronomia",
            nombre = "Escuela de Gastronomía",
            precio = 1_000_000L,
            descripcion = "Educación gastronómica y preparación de alimentos",
            capacidad = 1500,
            baseAlumnos = 320,
            mejoraMax = 3,
            texturePrefix = "Gastronomia",
            renderW = 3700f * ESCALA,
            renderH = 2500f * ESCALA,
            interiorTexturePrefix = "Gastronomiainterior"
        )
    )

    fun getPropiedad(id: String): Propiedad? =
        propiedades[id] ?: propiedades.values.find {
            it.id.equals(id, ignoreCase = true)
        }

    fun resetProgress() {
        propiedades.values.forEach {
            it.nivel = 0
            it.comprada = false
        }
    }
}
