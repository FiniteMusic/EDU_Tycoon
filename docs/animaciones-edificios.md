# Sistema de animaciones de edificios

## 1. Objetivo

Se implementó un sistema de renderizado y animación para los edificios de EDU-TYCOON con el objetivo de incorporar elementos visuales dinámicos sin modificar la lógica principal del juego.

La primera implementación se realizó sobre la ESCOM mediante estudiantes animados que se desplazan alrededor del edificio. Esta implementación funciona como prototipo para reutilizar el mismo sistema posteriormente en otros edificios.

El sistema permite mantener separados:

- El estado y la lógica de cada propiedad.
- El renderizado de los edificios.
- Las animaciones asociadas a cada edificio.
- Los recursos gráficos utilizados por las animaciones.

---

## 2. Componentes principales

La implementación se divide principalmente en los siguientes componentes:

### `BuildingRenderer`

Responsable de dibujar el edificio y los elementos animados asociados a él.

Recibe:

- La textura del edificio.
- La propiedad correspondiente.
- La posición de renderizado.
- El tiempo acumulado de animación.
- La lista de elementos animados.

De esta forma, `GameScreen` ya no contiene directamente toda la lógica necesaria para dibujar los edificios.

### `BuildingAnimation`

Encapsula una animación formada por varios `TextureRegion`.

Permite definir:

- Los frames utilizados.
- La duración de cada frame.
- El modo de reproducción de la animación.

El frame correspondiente se obtiene utilizando el tiempo acumulado de ejecución.

### `AnimatedElement`

Representa un elemento animado asociado a un edificio.

Cada elemento contiene los parámetros necesarios para determinar su tamaño, posición y movimiento.

### `BuildingAnimationRegistry`

Centraliza el registro de las animaciones disponibles para cada edificio.

Su función es asociar un edificio y su nivel con los elementos animados que deben mostrarse. También administra los recursos gráficos utilizados por estas animaciones para que puedan liberarse correctamente al cerrar la pantalla.

---

## 3. Parámetros reutilizables

Los elementos animados utilizan los siguientes parámetros:

| Parámetro | Descripción |
| --- | --- |
| `offsetX` | Posición horizontal inicial del elemento respecto al edificio. |
| `offsetY` | Posición vertical inicial del elemento respecto al edificio. |
| `width` | Ancho con el que se dibuja el elemento. |
| `height` | Alto con el que se dibuja el elemento. |
| `movementX` | Distancia horizontal recorrida durante el movimiento. |
| `movementY` | Distancia vertical recorrida durante el movimiento. |
| `movementDuration` | Tiempo que tarda el elemento en completar un trayecto en una dirección. |
| `frameDuration` | Tiempo durante el cual permanece visible cada frame de la animación. |
| `frames` | Conjunto de imágenes que forman la animación. |

Las posiciones se calculan respecto a la posición del edificio. Esto permite que las animaciones permanezcan visualmente asociadas a la estructura aunque la cámara se desplace o cambie el nivel de zoom.

---

## 4. Movimiento de los elementos

El movimiento se calcula a partir del tiempo acumulado de animación.

Cada elemento parte de:

`offsetX` y `offsetY`

y se desplaza progresivamente utilizando:

`movementX` y `movementY`.

Al finalizar el trayecto, el elemento realiza el recorrido en sentido contrario.

Durante el regreso también se invierte horizontalmente el frame utilizado, de manera que el personaje mantenga una orientación coherente con la dirección de desplazamiento.

Este comportamiento permite crear recorridos continuos sin tener que definir manualmente una animación independiente para cada dirección.

---

## 5. Integración con el nivel del edificio

Las animaciones pueden depender del nivel actual de una propiedad.

El registro obtiene los elementos mediante:

`buildingId` y `level`.

Para el prototipo de ESCOM se utiliza el nivel del edificio como nivel de actividad:

| Nivel | Elementos visibles |
| --- | --- |
| 0 | Sin estudiantes animados. |
| 1 | Un estudiante animado. |
| 2 | Dos estudiantes animados. |

De esta manera, la mejora de un edificio también puede producir un cambio visual en la actividad que ocurre a su alrededor.

El sistema puede extenderse posteriormente con más tipos de elementos, por ejemplo:

- estudiantes;
- puertas;
- luces;
- banderas;
- vegetación;
- humo;
- vehículos u otros objetos ambientales.

---

## 6. Integración con el renderizado

Las animaciones reutilizan el sistema de culling existente.

Antes de dibujar un edificio se comprueba si se encuentra dentro del área visible de la cámara. Si el edificio no es visible, tampoco se procesan sus elementos animados.

El flujo simplificado es:

1. Obtener la propiedad.
2. Verificar que esté comprada.
3. Obtener su textura.
4. Comprobar si está dentro del área visible.
5. Consultar sus elementos animados según edificio y nivel.
6. Dibujar el edificio.
7. Dibujar sus elementos animados.

Esto evita procesar innecesariamente animaciones pertenecientes a edificios que se encuentran fuera de pantalla.

---

## 7. Recursos gráficos

Los frames utilizados por las animaciones se almacenan dentro de los recursos del proyecto.

Para el prototipo de ESCOM se utiliza la estructura:

`assets/Mapa/animaciones/escom/`

Los recursos son cargados por el registro de animaciones y reutilizados mientras la pantalla permanece activa.

Cuando `GameScreen` es liberado, también se liberan los recursos administrados por `BuildingAnimationRegistry`.

Esto evita mantener texturas de animaciones innecesariamente en memoria después de abandonar la pantalla.

---

## 8. Cómo agregar una animación a otro edificio

Para incorporar animaciones a una nueva propiedad se debe:

1. Agregar los frames correspondientes dentro de `assets/Mapa/animaciones/`.
2. Crear la animación a partir de esos frames.
3. Registrar uno o más `AnimatedElement` asociados al ID de la propiedad.
4. Ajustar `offsetX` y `offsetY` para colocar correctamente el elemento.
5. Definir `movementX`, `movementY` y `movementDuration` si el elemento debe desplazarse.
6. Probar el comportamiento con diferentes niveles de zoom.
7. Verificar que el elemento permanezca correctamente asociado al edificio.

Los valores de posición y movimiento deben ajustarse individualmente debido a que cada edificio posee dimensiones y puntos de origen diferentes.

---

## 9. Prototipo implementado

La ESCOM se utilizó como primer edificio para validar el sistema.

El prototipo permite comprobar:

- Animación mediante múltiples frames.
- Movimiento continuo.
- Cambio de orientación durante el recorrido de regreso.
- Posicionamiento relativo al edificio.
- Diferentes niveles de actividad.
- Funcionamiento durante desplazamiento de cámara y zoom.
- Reutilización del culling existente.
- Administración y liberación de recursos.

Con esta implementación se establece una base reutilizable para incorporar posteriormente actividad visual a otros edificios sin concentrar nuevamente toda la lógica dentro de `GameScreen`.