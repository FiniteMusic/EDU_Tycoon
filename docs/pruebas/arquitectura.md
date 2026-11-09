# Arquitectura y estructura de EDU-TYCOON

## 2.1 Estructura del repositorio

EDU-TYCOON es un proyecto desarrollado en Kotlin utilizando el framework
libGDX y una estructura modular administrada mediante Gradle.

El archivo `settings.gradle` define los dos módulos principales que conforman
actualmente el proyecto:

- `core`
- `android`

La estructura general del repositorio es la siguiente:

EDU_Tycoon/
├── android/
├── assets/
├── core/
├── gradle/
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
└── settings.gradle


### Módulo `core`

El módulo `core` concentra la mayor parte de la lógica y funcionamiento del
videojuego.

El código principal se encuentra en:

`core/src/main/kotlin/io/moviles/IPN_Tycoon/`

Dentro de este paquete se encuentran archivos como:

- `Main.kt`: clase principal del juego dentro del módulo `core`.
- `GameScreen.kt`: contiene gran parte del funcionamiento de la pantalla
  principal de la partida, incluyendo la interacción y representación del mapa.
- `GameState.kt`: administra información relacionada con el estado de la
  partida.
- `Propiedad.kt`: contiene información y comportamiento asociado con las
  propiedades del juego.
- `BuildingInfoWindow.kt`: administra la ventana de información de los
  edificios.
- `Bienvenida.kt`: implementa la pantalla de bienvenida.
- `SeleccionPartida.kt`: implementa la selección de partidas.
- `PartidasGuardadas.kt`: maneja la interfaz relacionada con las partidas
  guardadas.
- `GameSaveManager.kt`: contiene operaciones relacionadas con el guardado de
  las partidas.
- `PauseMenuWindow.kt`: implementa el menú de pausa.
- `BaseScreen.kt`: proporciona funcionalidad base utilizada por las pantallas
  del juego.

También existen los paquetes `data` y `engine`, utilizados para organizar
componentes relacionados con el manejo de datos y el funcionamiento interno
del juego.

El módulo contiene además pruebas unitarias en:

`core/src/test/kotlin/`


### Módulo `android`

El módulo `android` contiene la implementación y configuración necesaria para
ejecutar EDU-TYCOON como una aplicación Android.

Entre sus archivos principales se encuentran:

- `android/build.gradle`
- `android/AndroidManifest.xml`
- `android/src/main/`
- `android/res/`

Este módulo depende directamente de `core` mediante:


implementation project(':core')


## 2.2 Arquitectura del proyecto

EDU-TYCOON utiliza una arquitectura modular en la que la lógica principal del
juego se encuentra en `core`, mientras que el módulo `android` proporciona el
punto de entrada de la aplicación y los componentes que dependen directamente
de Android.

De manera general, el flujo de la aplicación puede representarse así:

```text
Dispositivo Android
        │
        ▼
AndroidLauncher
        │
        ├── Inicializa Room
        │       │
        │       ▼
        │   AppDatabase
        │       │
        │       ▼
        │   EscuelaDao
        │       │
        │       ▼
        │   EscuelaRepository
        │       │
        │       ▼
        │   AndroidGameSaveManager
        │
        ▼
      Main
        │
        ├── Bienvenida
        ├── SeleccionPartida
        ├── PartidasGuardadas
        └── GameScreen
                │
                ├── GameState
                ├── PropiedadRepository
                ├── BuildingInfoWindow
                ├── GameCycleEngine
                ├── EconomyEngine
                └── assets/
```

### Punto de entrada en Android

El inicio de la aplicación ocurre en:

`android/src/main/kotlin/io/moviles/IPN_Tycoon/android/AndroidLauncher.kt`

`AndroidLauncher` extiende `AndroidApplication`, clase proporcionada por el
backend Android de libGDX.

Durante `onCreate()` obtiene una instancia de la base de datos, crea un
`EscuelaRepository` y posteriormente un `AndroidGameSaveManager`.

Finalmente inicializa el juego mediante:

```kotlin
initialize(Main(saveManager), AndroidApplicationConfiguration().apply {
    useImmersiveMode = true
})
```

Esto permite que el módulo Android proporcione al módulo `core` una
implementación concreta del sistema de guardado sin introducir dependencias
directas de Android en la interfaz `GameSaveManager`.


### Inicialización del juego

La clase:

`core/src/main/kotlin/io/moviles/IPN_Tycoon/Main.kt`

extiende `KtxGame<KtxScreen>`.

Al ejecutarse su método `create()` se inicializan componentes de KTX y VisUI y
se registran las principales pantallas:

```kotlin
addScreen(Bienvenida(this))
addScreen(SeleccionPartida(this))
addScreen(PartidasGuardadas(this))
addScreen(GameScreen(this))

setScreen<Bienvenida>()
```

Por lo tanto, `Bienvenida` constituye la primera pantalla mostrada después de
inicializar el juego.


### Capa de presentación e interacción

Las pantallas y ventanas del juego se encuentran principalmente dentro del
módulo `core`.

Entre ellas se encuentran:

- `Bienvenida.kt`
- `SeleccionPartida.kt`
- `PartidasGuardadas.kt`
- `GameScreen.kt`
- `BuildingInfoWindow.kt`
- `PauseMenuWindow.kt`

`GameScreen` concentra una parte importante de la interacción durante la
partida, incluyendo el mapa, cámara, selección de edificios y representación
de elementos del campus.

`BuildingInfoWindow` presenta información de una propiedad y permite realizar
operaciones de compra o mejora.


### Estado y modelo del juego

`GameState.kt` mantiene el estado global de la partida.

Entre sus datos se encuentran:

- dinero disponible;
- número total de alumnos;
- ciclos jugados;
- jugador y escuela;
- slot de guardado actual;
- estado de la música.

También proporciona operaciones sobre la economía:

```kotlin
fun puedeComprar(costo: Long) = dinero >= costo

fun gastar(cantidad: Long): Boolean {
    if (dinero < cantidad) return false
    dinero -= cantidad
    return true
}
```

Las propiedades disponibles se representan mediante la clase `Propiedad` y se
registran en `PropiedadRepository`.

Cada propiedad puede contener información como nombre, precio, capacidad,
nivel, estado de compra y recurso gráfico asociado.


### Motores de funcionamiento

El paquete:

`core/src/main/kotlin/io/moviles/IPN_Tycoon/engine/`

contiene componentes encargados de distintos aspectos del funcionamiento de
la partida.

Entre ellos se encuentran:

- `GameCycleEngine`
- `EconomyEngine`
- `EstudiantesEngine`
- `EventEngine`

`GameCycleEngine` coordina el avance de los ciclos y notifica a los sistemas
registrados siguiendo un orden de resolución.

Por ejemplo, `EconomyEngine` calcula los ingresos generados por los edificios
comprados y posteriormente los acredita al estado de la partida mediante
`GameState`.


### Persistencia de datos

El guardado de partidas utiliza Room y se distribuye entre los módulos `core`
y `android`.

En `core` se encuentran:

- `GameSaveManager`
- `EscuelaEntity`
- `EscuelaDao`
- `EscuelaRepository`

`GameSaveManager` funciona como una interfaz independiente de Android para las
operaciones de guardado, carga y eliminación.

En el módulo `android`, `AndroidGameSaveManager` implementa dicha interfaz y
utiliza `EscuelaRepository` para acceder a Room.

El flujo general de persistencia es:

```text
Pantallas del juego
       │
       ▼
GameSaveManager
       │
       ▼
AndroidGameSaveManager
       │
       ▼
EscuelaRepository
       │
       ▼
EscuelaDao
       │
       ▼
Room / AppDatabase
       │
       ▼
ipn_tycoon_db
```

La información se almacena localmente en el dispositivo. En la arquitectura
revisada no se identificó un servidor externo necesario para guardar las
partidas.


### Recursos gráficos y mapa

Los recursos utilizados por libGDX se encuentran en `assets/`.

`GameScreen` carga y representa el mapa y utiliza los datos definidos en
`PropiedadRepository` para relacionar las propiedades con sus recursos
gráficos.

Los edificios pueden definir un `texturePrefix`. A partir de este valor y del
nivel de la propiedad se determina el recurso correspondiente.

Esta relación es especialmente relevante para la propuesta del equipo, ya que
las modificaciones de mapa, edificios y animaciones deberán integrarse sin
alterar las capas que actualmente contienen información necesaria para la
interacción del juego.


## 2.3 Recorrido de una funcionalidad existente

Para demostrar el flujo entre la interacción del usuario, la lógica y la
interfaz se analizó la compra o mejora de un edificio.

### Paso 1. El jugador selecciona un edificio

Durante la exploración del mapa, `GameScreen` identifica la propiedad
seleccionada y permite mostrar su información.

La propiedad correspondiente se obtiene de los elementos definidos en
`PropiedadRepository`.


### Paso 2. Se muestra la información

La información se presenta mediante:

`BuildingInfoWindow.kt`

La ventana recibe una instancia de `Propiedad` y determina si el jugador puede
comprarla, mejorarla o si ya alcanzó su nivel máximo.

Si la propiedad todavía no ha sido comprada, el costo corresponde a:

```kotlin
costo = data.precio
```

Si ya fue adquirida pero admite otra mejora, el costo se obtiene mediante:

```kotlin
costo = GameState.costoMejora(data)
```


### Paso 3. Se valida el dinero disponible

Cuando el jugador presiona el botón correspondiente, la aplicación consulta:

```kotlin
if (!GameState.puedeComprar(costo)) {
    setText("¡Saldo insuficiente!")
    color = Color.RED
    isDisabled = true
    return@onChange
}
```

Si el jugador no cuenta con el dinero requerido, la operación se detiene y el
estado de la propiedad no se modifica.


### Paso 4. Se realiza la operación

Si existe dinero suficiente se ejecuta:

```kotlin
GameState.gastar(costo)
```

Después se modifica la propiedad.

Para una compra nueva:

```kotlin
data.comprada = true
data.nivel = 1
```

Para una mejora:

```kotlin
data.nivel++
```


### Paso 5. Se actualiza la pantalla

Después de modificar la propiedad se ejecuta:

```kotlin
onBuildingChanged()
```

La ventana se cierra y la pantalla puede actualizar la representación del
edificio y los valores correspondientes al nuevo estado.


### Archivos involucrados

Los principales archivos que participan en este recorrido son:

- `GameScreen.kt`: interacción con el mapa y actualización visual.
- `BuildingInfoWindow.kt`: interfaz y ejecución de la acción de compra/mejora.
- `GameState.kt`: validación y modificación del dinero.
- `Propiedad.kt`: definición del estado y características de las propiedades.

Por ejemplo, para modificar la regla que determina si una compra puede
realizarse sería necesario revisar principalmente `GameState.kt` y
`BuildingInfoWindow.kt`.

Para modificar la forma en que el resultado de la compra aparece en el mapa,
el principal archivo a revisar sería `GameScreen.kt`, junto con los recursos
correspondientes en `assets/`.