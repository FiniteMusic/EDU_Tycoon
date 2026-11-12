package io.moviles.IPN_Tycoon

/** Noventa retos temáticos, cinco por cada edificio. */
data class RetoTematico(val pregunta: String, val opciones: List<String>, val correcta: Int)

object CatalogoRetos {
    private val banco = mapOf(
        "escom_hitbox" to listOf(
            RetoTematico("¿Qué estructura repite instrucciones hasta una condición?", listOf("Compilador", "Bucle", "Variable"), 1),
            RetoTematico("¿Qué tipo de dato representa verdadero o falso?", listOf("Decimal", "Cadena", "Booleano"), 2),
            RetoTematico("¿Qué algoritmo busca en una lista ordenada dividiendo el rango?", listOf("Búsqueda binaria", "Búsqueda aleatoria", "Burbuja"), 0),
            RetoTematico("¿Qué significa HTTP?", listOf("Herramienta para imágenes", "Protocolo de transferencia de hipertexto", "Hardware de conexión local"), 1),
            RetoTematico("¿Qué estructura sigue el orden primero en entrar, primero en salir?", listOf("Pila", "Árbol", "Cola"), 2)
        ),
        "Direccion" to listOf(
            RetoTematico("¿Qué documento define objetivos y estrategias institucionales?", listOf("Plan estratégico", "Horario de comida", "Recibo de compra"), 0),
            RetoTematico("¿Qué indicador mide resultados respecto de objetivos?", listOf("ZIP", "PNG", "KPI"), 2),
            RetoTematico("¿Qué práctica mejora la rendición de cuentas?", listOf("Duplicar expedientes", "Ocultar registros", "Transparencia"), 2),
            RetoTematico("¿Quién coordina recursos y prioridades de una institución?", listOf("Administración", "Solo proveedores", "Solo visitantes"), 0),
            RetoTematico("¿Qué método compara costos y beneficios de un proyecto?", listOf("Análisis costo-beneficio", "Rotación de colores", "Inventario de plantas"), 0)
        ),
        "Mac_and_cheese" to listOf(
            RetoTematico("¿Qué medida básica previene contaminación de alimentos?", listOf("Lavado de manos", "Omitir refrigeración", "Guardar comida al sol"), 0),
            RetoTematico("¿Cuál es una fuente de proteína?", listOf("Huevo", "Azúcar", "Refresco"), 0),
            RetoTematico("¿Qué significa una cadena de frío?", listOf("Congelar utensilios siempre", "Servir sin higiene", "Mantener temperaturas seguras"), 2),
            RetoTematico("¿Qué alimento suele aportar fibra?", listOf("Bebida azucarada", "Verduras", "Dulces"), 1),
            RetoTematico("¿Qué información indica una etiqueta nutricional?", listOf("Nutrientes y porción", "Nombre del cajero", "Color del plato"), 0)
        ),
        "cafeteria" to listOf(
            RetoTematico("¿Qué opción ofrece un almuerzo equilibrado?", listOf("Verduras, proteína y cereal", "Solo refresco", "Solo dulces"), 0),
            RetoTematico("¿Qué nutriente contribuye a la hidratación?", listOf("Sal en exceso", "Agua", "Alcohol"), 1),
            RetoTematico("¿Qué práctica reduce desperdicios en restaurante?", listOf("Desechar sobrantes sin evaluar", "Comprar al azar", "Planificar porciones"), 2),
            RetoTematico("¿Qué debe hacerse antes de preparar alimentos?", listOf("Desinfectar superficies", "Reutilizar platos sucios", "Apagar refrigeradores"), 0),
            RetoTematico("¿Qué ayuda a la seguridad alimentaria?", listOf("Mezclar todos los utensilios", "Dejar carnes sin frío", "Separar crudos y cocidos"), 2)
        ),
        "auditorio" to listOf(
            RetoTematico("¿Qué mejora la claridad durante una conferencia?", listOf("Luces apagadas todo el tiempo", "Micrófono probado", "Eco sin control"), 1),
            RetoTematico("¿Qué caracteriza un debate académico?", listOf("Sin moderación", "Argumentos y evidencia", "Solo gritos"), 1),
            RetoTematico("¿Para qué sirve un guion técnico?", listOf("Coordinar audio, luces y escena", "Calcular matrículas", "Medir precipitación"), 0),
            RetoTematico("¿Qué recurso apoya accesibilidad en un evento?", listOf("Subtítulos", "Textos ilegibles", "Sonido distorsionado"), 0),
            RetoTematico("¿Qué mide el aforo?", listOf("Altura del escenario", "Capacidad de asistentes", "Precio del micrófono"), 1)
        ),
        "Arquitectura" to listOf(
            RetoTematico("¿Qué plano representa una vista horizontal de un edificio?", listOf("Planta", "Corte longitudinal", "Fachada"), 0),
            RetoTematico("¿Qué elemento transmite cargas hacia el suelo?", listOf("Ventana", "Pintura", "Cimentación"), 2),
            RetoTematico("¿Qué escala muestra 1 cm como 1 m real?", listOf("1:1", "1:100", "1:10 000"), 1),
            RetoTematico("¿Qué recurso reduce ganancia térmica solar?", listOf("Sombreado", "Más asfalto oscuro", "Quitar aislante"), 0),
            RetoTematico("¿Qué principio mejora el acceso universal?", listOf("Rampas accesibles", "Solo escaleras", "Puertas bloqueadas"), 0)
        ),
        "Biologicas" to listOf(
            RetoTematico("¿Cuál es la unidad básica de los seres vivos?", listOf("Célula", "Átomo sin organización", "Montaña"), 0),
            RetoTematico("¿Qué orgánulo realiza fotosíntesis?", listOf("Ribosoma", "Núcleo", "Cloroplasto"), 2),
            RetoTematico("¿Qué molécula guarda información hereditaria?", listOf("Agua", "ADN", "Almidón"), 1),
            RetoTematico("¿Qué relación describe un depredador?", listOf("No interactúa", "Consume otra especie", "Produce minerales"), 1),
            RetoTematico("¿Qué proceso transforma energía química en ATP?", listOf("Sedimentación", "Respiración celular", "Evaporación"), 1)
        ),
        "Bioquimica" to listOf(
            RetoTematico("¿Qué cataliza reacciones biológicas?", listOf("Grava", "Ladrillo", "Enzima"), 2),
            RetoTematico("¿Qué escala mide acidez?", listOf("Voltaje", "pH", "Frecuencia"), 1),
            RetoTematico("¿Qué macromolécula suele formar enzimas?", listOf("Vidrio", "Proteína", "Sal gema"), 1),
            RetoTematico("¿Qué unión forma cadenas de aminoácidos?", listOf("Enlace magnético", "Enlace peptídico", "Soldadura"), 1),
            RetoTematico("¿Qué ruta metabólica degrada glucosa?", listOf("Glucólisis", "Difusión térmica", "Fotosíntesis inversa"), 0)
        ),
        "Matematicas" to listOf(
            RetoTematico("¿Cuánto es 7 × 8?", listOf("56", "54", "64"), 0),
            RetoTematico("¿Cuál es el área de un cuadrado de lado 4?", listOf("16", "8", "12"), 0),
            RetoTematico("¿Qué valor tiene π aproximadamente?", listOf("3.14", "1.41", "2.71"), 0),
            RetoTematico("¿Cuál es la derivada de x²?", listOf("2x", "x³", "x"), 0),
            RetoTematico("¿Qué probabilidad hay de cara en una moneda justa?", listOf("1/4", "1/3", "1/2"), 2)
        ),
        "Edificio1" to listOf(
            RetoTematico("¿Qué órgano bombea la sangre?", listOf("Corazón", "Estómago", "Pulmón"), 0),
            RetoTematico("¿Qué significa RCP?", listOf("Reanimación cardiopulmonar", "Registro de compras", "Radiografía por computadora"), 0),
            RetoTematico("¿Qué vitamina se sintetiza con exposición solar?", listOf("Vitamina D", "Vitamina B12", "Vitamina C"), 0),
            RetoTematico("¿Qué aparato permite escuchar sonidos del corazón?", listOf("Estetoscopio", "Microscopio", "Termómetro"), 0),
            RetoTematico("¿Qué se mide con un esfigmomanómetro?", listOf("Agudeza visual", "Presión arterial", "Temperatura corporal"), 1)
        ),
        "Edificio2" to listOf(
            RetoTematico("¿Qué instrumento mide temperatura corporal?", listOf("Tensiómetro", "Balanza", "Termómetro"), 2),
            RetoTematico("¿Qué acción previene infecciones?", listOf("Lavado de manos", "Evitar limpieza", "Compartir agujas"), 0),
            RetoTematico("¿Qué signo suele indicar deshidratación?", listOf("Estatura", "Color de ojos", "Sed intensa"), 2),
            RetoTematico("¿Qué equipo protege frente a salpicaduras?", listOf("Gafas de seguridad", "Gorra decorativa", "Sandalias"), 0),
            RetoTematico("¿Cuál es el primer paso ante una emergencia?", listOf("Ignorar la situación", "Mover sin evaluar", "Evaluar seguridad y pedir ayuda"), 2)
        ),
        "Museo" to listOf(
            RetoTematico("¿Para qué sirve una cédula de museo?", listOf("Fijar precio del estacionamiento", "Contextualizar una pieza", "Indicar temperatura exterior"), 1),
            RetoTematico("¿Qué protege las colecciones frágiles?", listOf("Manipulación libre", "Control ambiental", "Sol directo"), 1),
            RetoTematico("¿Qué práctica apoya la conservación?", listOf("Mezclar piezas sin etiqueta", "Registro de inventario", "No documentar"), 1),
            RetoTematico("¿Qué es una exposición temporal?", listOf("Archivo cerrado", "Muestra de duración limitada", "Edificio perpetuo"), 1),
            RetoTematico("¿Qué recurso favorece la inclusión?", listOf("Textos diminutos", "Señalética accesible", "Acceso restringido sin motivo"), 1)
        ),
        "Turismo" to listOf(
            RetoTematico("¿Qué caracteriza al turismo sostenible?", listOf("Agotar recursos", "Excluir a residentes", "Cuidar ambiente y comunidad"), 2),
            RetoTematico("¿Qué documento suele necesitarse para viajar al extranjero?", listOf("Boleta de biblioteca", "Tarjeta del comedor", "Pasaporte"), 2),
            RetoTematico("¿Qué es un itinerario?", listOf("Etiqueta nutricional", "Plan de actividades y rutas", "Póliza de seguro escolar"), 1),
            RetoTematico("¿Qué práctica respeta el patrimonio cultural?", listOf("Seguir normas locales", "Rayar monumentos", "Retirar piezas"), 0),
            RetoTematico("¿Qué indicador compara habitaciones ocupadas con disponibles?", listOf("Voltaje", "Índice glucémico", "Tasa de ocupación"), 2)
        ),
        "palapas" to listOf(
            RetoTematico("¿Qué elemento protege del sol en una palapa?", listOf("Cable", "Cubierta", "Tubería"), 1),
            RetoTematico("¿Qué reduce impacto en áreas verdes?", listOf("Extraer plantas", "Depositar residuos correctamente", "Tirar basura"), 1),
            RetoTematico("¿Qué actividad fomenta convivencia?", listOf("Excluir compañeros", "Trabajo colaborativo", "Interrumpir todos"), 1),
            RetoTematico("¿Qué ayuda a conservar madera exterior?", listOf("Mantenimiento preventivo", "Ignorar humedad", "Mantenerla mojada"), 0),
            RetoTematico("¿Qué mejora la seguridad de una zona de descanso?", listOf("Iluminación y caminos libres", "Obstáculos sin señalizar", "Pisos resbalosos"), 0)
        ),
        "Biblioteca" to listOf(
            RetoTematico("¿Qué es una fuente primaria?", listOf("Documento original", "Resumen anónimo", "Publicidad sin fuente"), 0),
            RetoTematico("¿Qué sirve para localizar un libro?", listOf("Caja registradora", "Termostato", "Catálogo"), 2),
            RetoTematico("¿Qué es una cita bibliográfica?", listOf("Costo de colegiatura", "Referencia a una fuente", "Una multa de tránsito"), 1),
            RetoTematico("¿Qué ayuda a evaluar información confiable?", listOf("Cantidad de colores", "Solo el título llamativo", "Autoría y evidencia"), 2),
            RetoTematico("¿Qué se debe evitar en trabajos académicos?", listOf("Revisar datos", "Citar fuentes", "Plagio"), 2)
        ),
        "Arte_diseno_deporte" to listOf(
            RetoTematico("Adivina el personaje: pintó La noche estrellada.", listOf("Vincent van Gogh", "Marie Curie", "Isaac Newton"), 0),
            RetoTematico("¿Qué colores se combinan para obtener verde en pigmentos?", listOf("Negro y blanco", "Azul y amarillo", "Rojo y morado"), 1),
            RetoTematico("¿Qué principio organiza elementos en diseño?", listOf("Exceso de texto", "Jerarquía visual", "Ruido aleatorio"), 1),
            RetoTematico("¿Qué capacidad mejora con calentamiento deportivo?", listOf("Preparación muscular", "Fatiga inmediata", "Deshidratación"), 0),
            RetoTematico("¿Qué técnica usa puntos pequeños de color?", listOf("Puntillismo", "Grabado láser", "Escultura en mármol"), 0)
        ),
        "Dormitorios" to listOf(
            RetoTematico("¿Qué hábito favorece el descanso?", listOf("Pantallas toda la noche", "Horario de sueño constante", "Cafeína nocturna abundante"), 1),
            RetoTematico("¿Qué favorece una habitación saludable?", listOf("Basura acumulada", "Humedad permanente", "Ventilación adecuada"), 2),
            RetoTematico("¿Qué significa higiene del sueño?", listOf("Evitar toda actividad física", "Hábitos que mejoran el dormir", "Lavarse solo por la mañana"), 1),
            RetoTematico("¿Qué reduce riesgos de incendio?", listOf("Bloquear puertas", "Cables sobrecargados", "Salidas libres"), 2),
            RetoTematico("¿Qué recurso favorece convivencia en dormitorio?", listOf("Ruido sin límites", "Invadir espacios", "Normas compartidas"), 2)
        ),
        "Gastronomia" to listOf(
            RetoTematico("¿Qué técnica cocina un alimento sumergido en agua caliente?", listOf("Hervir", "Ahumar", "Congelar"), 0),
            RetoTematico("¿Qué función tiene la levadura en panificación?", listOf("Eliminar toda humedad", "Congelar el pan", "Fermentar la masa"), 2),
            RetoTematico("¿Qué se recomienda al manipular carne cruda?", listOf("No limpiar utensilios", "Mezclar con alimentos listos", "Tabla separada"), 2),
            RetoTematico("¿Qué sabor se asocia con glutamato?", listOf("Picante siempre", "Amargo solamente", "Umami"), 2),
            RetoTematico("¿Qué proceso espesa una salsa con almidón?", listOf("Gelatinización", "Sublimación", "Destilación"), 0)
        )
    )
    fun obtener(id: String, nivel: Int): RetoTematico? = banco[id]?.getOrNull(nivel - 1)
    fun cobertura(): Map<String, Int> = banco.mapValues { it.value.size }
}
