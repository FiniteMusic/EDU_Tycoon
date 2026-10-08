package io.moviles.IPN_Tycoon

/**
 * @param texturePrefix  Prefijo del archivo en Mapa/Edificios/.
 *                       Ruta generada: "${texturePrefix}lvl${nivel}.png"
 *                       null = sin sprite todavía.
 * @param renderW/H      Tamaño de render en unidades de mundo.
 *                       Tomados del layer Edificios del TMX × ESCALA.
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
    var comprada: Boolean = false
)

object PropiedadRepository {

    /** Multiplicador global de tamaño de sprite en el mapa. */
    private const val ESCALA = 1.1f

    val propiedades = mapOf(

        // ── ESCOM ─────────────────────────────────────────────────────
        // TMX: 3728 × 1968
        "escom_hitbox" to Propiedad(
            id            = "escom_hitbox",
            nombre        = "Escuela de Computación",
            precio        = 300_000L,
            descripcion   = "Escuela Superior de Cómputo",
            capacidad     = 3200,
            baseAlumnos   = 600,
            mejoraMax     = 2,
            texturePrefix = "escom",
            renderW       = 3728f * ESCALA,
            renderH       = 1968f * ESCALA
        ),

        // ── DIRECCIÓN GENERAL ─────────────────────────────────────────
        // TMX: 6112 × 5464
        "Direccion" to Propiedad(
            id            = "Direccion",
            nombre        = "Dirección General",
            precio        = 5_000_000L,
            descripcion   = "Oficinas administrativas del IPN",
            capacidad     = 1000,
            baseAlumnos   = 200,
            mejoraMax     = 4,
            texturePrefix = "Direccion",
            renderW       = 6112f * ESCALA,
            renderH       = 5464f * ESCALA
        ),

        // ── MAC AND CHEESE ────────────────────────────────────────────
        "Mac_and_cheese" to Propiedad(
            id            = "Mac_and_cheese",
            nombre        = "Burgers & Fries",
            precio        = 120_000L,
            descripcion   = "Restaurante de comida rápida",
            capacidad     = 200,
            baseAlumnos   = 50,
            mejoraMax     = 2,
            texturePrefix = "Mac_and_cheese",
            renderW       = 2188f * ESCALA,
            renderH       = 1180f * ESCALA
        ),

        // ── CAFETERÍA ─────────────────────────────────────────────────
        // TMX: 2188 × 1180
        "cafeteria" to Propiedad(
            id            = "cafeteria",
            nombre        = "Cafetería",
            precio        = 150_000L,
            descripcion   = "Lugar para comer rico",
            capacidad     = 500,
            baseAlumnos   = 100,
            mejoraMax     = 2,
            texturePrefix = "cafeteria",
            renderW       = 2188f * ESCALA,
            renderH       = 1180f * ESCALA
        ),

        // ── AUDITORIO ─────────────────────────────────────────────────
        // TMX: 5312 × 4688
        "auditorio" to Propiedad(
            id            = "auditorio",
            nombre        = "Auditorio Alejo Peralta",
            precio        = 800_000L,
            descripcion   = "Eventos y conferencias",
            capacidad     = 2000,
            baseAlumnos   = 400,
            mejoraMax     = 2,
            texturePrefix = "auditorio",
            renderW       = 5312f * ESCALA,
            renderH       = 4688f * ESCALA
        ),

        // ── ESIA ARQUITECTURA ─────────────────────────────────────────
        // Sin textura todavía — tamaño estimado
        "Arquitectura" to Propiedad(
            id            = "Arquitectura",
            nombre        = "Arquitectura",
            precio        = 1_100_000L,
            descripcion   = "Escuela de construcción y diseño",
            capacidad     = 2500,
            baseAlumnos   = 500,
            mejoraMax     = 3,
            texturePrefix = "Arquitectura",
            renderW       = 4000f * ESCALA,
            renderH       = 2800f * ESCALA
        ),

        // ── ENCB BIOLÓGICAS ───────────────────────────────────────────
        // TMX: 5280 × 3164
        "Biologicas" to Propiedad(
            id            = "Biologicas",
            nombre        = "Biológicas",
            precio        = 1_250_000L,
            descripcion   = "Ciencias biológicas",
            capacidad     = 2800,
            baseAlumnos   = 550,
            mejoraMax     = 3,
            texturePrefix = "Biologicas",
            renderW       = 5280f * ESCALA,
            renderH       = 3164f * ESCALA
        ),

        // ── ENCB BIOQUÍMICA ───────────────────────────────────────────
        // TMX: 5132 × 3363
        "Bioquimica" to Propiedad(
            id            = "Bioquimica",
            nombre        = "Bioquímica",
            precio        = 1_250_000L,
            descripcion   = "Ciencias bioquímicas",
            capacidad     = 2800,
            baseAlumnos   = 550,
            mejoraMax     = 3,
            texturePrefix = "Bioquimica",
            renderW       = 5132f * ESCALA,
            renderH       = 3363f * ESCALA
        ),

        // ── ESFM MATEMÁTICAS ──────────────────────────────────────────
        // Sin textura todavía — tamaño estimado
        "Matematicas" to Propiedad(
            id            = "Matematicas",
            nombre        = "Matemáticas",
            precio        = 1_000_000L,
            descripcion   = "Física y Matemáticas",
            capacidad     = 1500,
            baseAlumnos   = 300,
            mejoraMax     = 3,
            texturePrefix = "Matematicas",
            renderW       = 4500f * ESCALA,
            renderH       = 2500f * ESCALA
        ),

        // ── EDIFICIO 1 ────────────────────────────────────────────────
        // TMX: 4148 × 3528
        "Edificio1" to Propiedad(
            id            = "Edificio1",
            nombre        = "Edificio 1",
            precio        = 400_000L,
            descripcion   = "Salones de clases generales",
            capacidad     = 1200,
            baseAlumnos   = 250,
            mejoraMax     = 1,
            texturePrefix = "Edificio1",
            renderW       = 4148f * ESCALA,
            renderH       = 3528f * ESCALA
        ),

        // ── EDIFICIO 2 ────────────────────────────────────────────────
        // TMX: 4144 × 3456
        "Edificio2" to Propiedad(
            id            = "Edificio2",
            nombre        = "Edificio 2",
            precio        = 400_000L,
            descripcion   = "Laboratorios de computación",
            capacidad     = 1200,
            baseAlumnos   = 250,
            mejoraMax     = 1,
            texturePrefix = "Edificio2",
            renderW       = 4144f * ESCALA,
            renderH       = 3456f * ESCALA
        ),

        // ── MUSEO TEZOZÓMOC ───────────────────────────────────────────
        // Sin textura todavía — tamaño estimado
        "Museo" to Propiedad(
            id            = "Museo",
            nombre        = "Museo Tezozómoc",
            precio        = 600_000L,
            descripcion   = "Ciencia y tecnología",
            capacidad     = 800,
            baseAlumnos   = 150,
            mejoraMax     = 3,
            texturePrefix = "Museo",
            renderW       = 6800f * ESCALA,
            renderH       = 4200f * ESCALA
        ),

        // ── EST TURISMO ───────────────────────────────────────────────
        // Sin textura todavía — tamaño estimado
        "Turismo" to Propiedad(
            id            = "Turismo",
            nombre        = "Turismo",
            precio        = 900_000L,
            descripcion   = "Escuela de Turismo",
            capacidad     = 2200,
            baseAlumnos   = 450,
            mejoraMax     = 3,
            texturePrefix = "Turismo",
            renderW       = 3000f * ESCALA,
            renderH       = 2000f * ESCALA
        ),

        // ── PALAPAS ───────────────────────────────────────────────────
        // TMX: 1997 × 1069
        "palapas" to Propiedad(
            id            = "palapas",
            nombre        = "Palapas",
            precio        = 350_000L,
            descripcion   = "Área de descanso y convivencia",
            capacidad     = 300,
            baseAlumnos   = 60,
            mejoraMax     = 2,
            texturePrefix = "palapas",
            renderW       = 1997f * ESCALA,
            renderH       = 1069f * ESCALA
        )
    )

    fun getPropiedad(id: String): Propiedad? =
        propiedades[id] ?: propiedades.values.find { it.id.equals(id, ignoreCase = true) }

    fun resetProgress() {
        propiedades.values.forEach { it.nivel = 0; it.comprada = false }
    }
}
