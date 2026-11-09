# Registro de pruebas — EDU-TYCOON

## Objetivo

El presente documento registra las pruebas realizadas sobre la versión base de
EDU-TYCOON utilizada por el equipo.

El objetivo es comprobar el funcionamiento general de la aplicación antes de
incorporar las modificaciones propuestas, documentando tanto los
comportamientos correctos como los posibles defectos encontrados.

Las pruebas se realizan directamente sobre la aplicación en ejecución y los
resultados se acompañan de evidencia visual cuando corresponde.


## Entorno de pruebas

| Elemento | Información |
|---|---|
| Aplicación | EDU-TYCOON |
| Rama | `main` |
| Plataforma | Android |
| Dispositivo / emulador | Pixel 10a — Emulador de Android Studio |
| Versión de Android | Android 17.0 ("CinnamonBun") |
| API | 37.2-ext |
| Arquitectura | x86_64 |
| Fecha de ejecución | 4 de octubre de 2026 |


---

# Prueba 1 — Flujo principal de una partida

## Objetivo

Comprobar que el jugador puede realizar el flujo principal de EDU-TYCOON,
desde el inicio de una partida hasta la interacción con una propiedad dentro
del mapa.

## Precondiciones

- La aplicación se encuentra instalada y puede iniciarse.
- Existe un slot disponible para iniciar o cargar una partida.

## Pasos

1. Iniciar EDU-TYCOON.
2. Acceder a una partida nueva o existente.
3. Esperar a que se muestre el mapa principal.
4. Desplazarse por diferentes zonas del mapa.
5. Seleccionar un edificio o propiedad.
6. Consultar la información mostrada.
7. Si el saldo disponible lo permite, comprar o mejorar la propiedad.
8. Cerrar la ventana de información.
9. Observar los cambios producidos en el saldo, estado de la propiedad y mapa.
10. Continuar desplazándose por el mapa para comprobar que la partida continúa
    funcionando.

## Resultado esperado

El usuario debe poder acceder al mapa, desplazarse, seleccionar una propiedad,
consultar su información y realizar una operación válida.

Cuando se realice una compra o mejora, el saldo y el estado de la propiedad
deben actualizarse y el resultado debe reflejarse en el juego.

## Resultado real

La aplicación permitió iniciar una partida, completar la introducción y acceder
al mapa principal. Fue posible desplazarse por el mapa, seleccionar propiedades
y consultar su información.

También fue posible comprar y mejorar propiedades. El saldo, la cantidad de
alumnos y la reputación se actualizaron después de realizar las operaciones.

Durante la prueba se detectó un problema con la propiedad **Mac and Cheese**.
La compra se realizó correctamente y posteriormente la ventana de gestión
mostró la propiedad como adquirida y disponible para mejorar. Sin embargo, su
representación gráfica no apareció en el mapa.

## Estado

`FALLIDA`


## Evidencia

[▶ Ver video de evidencia — Flujo principal](evidencia/pruebas/prueba-01-flujo-principal.mp4)

**Video 1.** Compra de Mac and Cheese. La operación se realiza correctamente,
pero el recurso gráfico correspondiente no aparece posteriormente en el mapa.

**Figura 1. Evidencia de la prueba del flujo principal.**

## Observaciones

El flujo principal del juego puede completarse y las operaciones realizadas
modifican correctamente los valores de la partida. Sin embargo, se detectó un
defecto de representación gráfica en Mac and Cheese: el estado de la propiedad
se actualiza después de la compra, pero el edificio correspondiente no se
muestra visualmente en el mapa.

El defecto es reproducible y puede registrarse como un issue independiente.


---

# Prueba 2 — Operación con saldo insuficiente

## Objetivo

Comprobar el comportamiento del juego cuando se intenta comprar o mejorar una
propiedad sin disponer del dinero necesario.

## Precondiciones

- Existe una partida activa.
- El jugador tiene acceso al mapa.
- El saldo disponible es menor que el costo de alguna propiedad o mejora.

## Pasos

1. Iniciar o cargar una partida.
2. Acceder al mapa principal.
3. Identificar una propiedad cuyo costo sea mayor al saldo disponible.
4. Seleccionar la propiedad.
5. Revisar el costo mostrado.
6. Intentar realizar la compra o mejora.
7. Observar el mensaje mostrado por la aplicación.
8. Revisar nuevamente el saldo disponible.
9. Comprobar el estado de la propiedad.

## Resultado esperado

La aplicación debe impedir la operación cuando el saldo disponible sea menor
al costo.

El dinero del jugador no debe disminuir y la propiedad debe conservar el
estado que tenía antes del intento.

La interfaz debe informar al usuario que no cuenta con saldo suficiente.

## Resultado real

La aplicación impidió realizar una operación cuando el costo de la propiedad
era mayor que el saldo disponible.

Al intentar efectuar la compra, la operación no se completó y la interfaz
mostró el mensaje **"¡Saldo insuficiente!"**. El estado de la propiedad no fue
modificado por el intento de compra.


## Estado

`APROBADA`


## Evidencia

[▶ Ver video de evidencia — Saldo Insuficiente](evidencia/pruebas/prueba-02-saldo-insuficiente.mp4)

**Figura 2. Evidencia del comportamiento ante una operación con saldo
insuficiente.**

## Observaciones

La validación de saldo funcionó correctamente. La aplicación detectó que los
recursos disponibles no eran suficientes, rechazó la operación e informó al
jugador mediante un mensaje visible en la ventana de gestión.


---

# Prueba 3 — Recreación de la aplicación

## Objetivo

Comprobar el comportamiento de EDU-TYCOON cuando la actividad Android es
recreada durante una partida.

## Precondiciones

- Existe una partida activa.
- El jugador se encuentra dentro del mapa.

## Pasos

1. Iniciar EDU-TYCOON.
2. Acceder a una partida.
3. Llegar al mapa principal.
4. Identificar el saldo y estado actual de la partida.
5. Seleccionar e interactuar con al menos una propiedad.
6. Enviar la aplicación a segundo plano.
7. Abrir otra aplicación o regresar a la pantalla principal del dispositivo.
8. Esperar unos segundos.
9. Regresar a EDU-TYCOON.
10. Observar la pantalla mostrada.
11. Comprobar si el mapa continúa funcionando.
12. Revisar si el estado relevante de la partida continúa disponible.

> Si el dispositivo o emulador permite forzar la recreación de la actividad
> mediante las opciones de desarrollador, puede repetirse la prueba utilizando
> dicho mecanismo y registrarlo en las observaciones.

## Resultado esperado

La aplicación no debe cerrarse inesperadamente al regresar a ella y debe
mantener un estado utilizable y coherente con la partida.

## Resultado real

Durante una partida se interrumpió temporalmente el uso de EDU-TYCOON y se
interactuó con otra aplicación antes de regresar al juego.

Al volver a EDU-TYCOON, la aplicación recuperó la pantalla del mapa y mantuvo
el estado de la partida. Posteriormente fue posible continuar interactuando
con una propiedad y consultar su información sin que se presentara un cierre
inesperado.

## Estado

`APROBADA`


## Evidencia

[▶ Ver video de evidencia — Recreación](evidencia/pruebas/prueba-03-recreacion.mp4)

**Figura 3. Estado de EDU-TYCOON después de regresar a la aplicación.**

## Observaciones

Después de interrumpir la aplicación y regresar a ella, EDU-TYCOON continuó
funcionando correctamente. El mapa volvió a mostrarse, el estado visible de la
partida se conservó y fue posible continuar interactuando con los elementos
del juego.

No se observaron cierres inesperados, pérdida evidente del estado de la partida
ni bloqueos durante el procedimiento realizado.


---

# Prueba 4 — Funcionamiento sin conexión a Internet

## Objetivo

Comprobar si las funciones principales de EDU-TYCOON pueden utilizarse sin una
conexión activa a Internet.

## Precondiciones

- EDU-TYCOON se encuentra instalado.
- Existe una partida que pueda iniciarse o cargarse.

## Pasos

1. Cerrar EDU-TYCOON si se encuentra abierto.
2. Desactivar Wi-Fi.
3. Desactivar los datos móviles, si el dispositivo dispone de ellos.
4. Verificar que el dispositivo se encuentre sin conexión a Internet.
5. Iniciar EDU-TYCOON.
6. Acceder a una partida.
7. Esperar a que se muestre el mapa.
8. Desplazarse por el mapa.
9. Seleccionar una propiedad.
10. Consultar su información.
11. Realizar una operación válida si el saldo lo permite.
12. Guardar la partida mediante el mecanismo disponible en el juego.
13. Continuar utilizando la aplicación.

## Resultado esperado

Las funciones principales de la partida deben continuar disponibles sin
conexión a Internet, incluyendo la exploración del mapa, interacción con
propiedades y operaciones que dependan únicamente del almacenamiento local.

## Resultado real

Se desactivaron las conexiones de red disponibles en el dispositivo antes de
iniciar EDU-TYCOON.

La aplicación inició correctamente sin conexión a Internet y permitió acceder
a una partida previamente existente. El mapa se cargó y fue posible continuar
utilizando las funciones locales del juego.

No se presentó un error de conexión ni un cierre inesperado durante la prueba.

## Estado

`APROBADA`


## Evidencia
[▶ Ver video de evidencia — Sin red](evidencia/pruebas/prueba-04-sin-red.mp4)

**Figura 4. EDU-TYCOON ejecutándose con el dispositivo sin conexión a
Internet.**

## Observaciones

Durante la prueba no se identificó una dependencia de Internet para iniciar la
aplicación y acceder al flujo principal de una partida. El estado almacenado
localmente pudo utilizarse aun con las conexiones de red desactivadas.


---

# Prueba 5 — Accesibilidad con tamaño de texto ampliado

## Objetivo

Comprobar el comportamiento de las principales interfaces de EDU-TYCOON al
utilizar un tamaño de fuente mayor desde la configuración de accesibilidad de
Android.

## Precondiciones

- EDU-TYCOON se encuentra instalado.
- El dispositivo permite modificar el tamaño de fuente del sistema.

## Pasos

1. Cerrar EDU-TYCOON.
2. Abrir la configuración de Android.
3. Acceder a las opciones de pantalla o accesibilidad.
4. Aumentar el tamaño de fuente por encima del valor predeterminado.
5. Iniciar nuevamente EDU-TYCOON.
6. Revisar la pantalla inicial.
7. Acceder a una partida.
8. Revisar los elementos de interfaz del mapa.
9. Seleccionar una propiedad.
10. Revisar la ventana de información de la propiedad.
11. Comprobar si los textos pueden leerse y si los controles continúan
    disponibles.
12. Intentar interactuar normalmente con los botones mostrados.

## Resultado esperado

El aumento del tamaño de fuente no debe provocar el cierre inesperado de la
aplicación.

Los controles principales deben continuar disponibles y los textos necesarios
para realizar las acciones principales deben permanecer utilizables.

## Resultado real

Se modificó el tamaño de fuente del sistema Android, estableciendo un valor
mayor al utilizado inicialmente.

Después del cambio se inició nuevamente EDU-TYCOON. La aplicación pudo mostrar
la pantalla de selección de partida y posteriormente cargar el mapa sin
presentar un cierre inesperado.

Los elementos principales utilizados durante la prueba continuaron
disponibles para la interacción.

## Estado

`APROBADA`


## Evidencia

[▶ Ver video de evidencia — Texto ampliado](evidencia/pruebas/prueba-05-texto-ampliado.mp4)

**Figura 5. Interfaz de EDU-TYCOON utilizando un tamaño de texto ampliado en
Android.**

## Observaciones

El cambio en el tamaño de fuente del sistema no impidió iniciar la aplicación,
seleccionar una partida ni acceder al mapa. Durante el recorrido realizado no
se identificaron bloqueos que impidieran continuar utilizando las funciones
principales observadas.


---

# Resumen de resultados
A continuación se presentan los resultados obtenidos durante la ejecución
de las pruebas:

| ID | Prueba | Estado | Defecto encontrado |
|---|---|---|---|
| P-01 | Flujo principal | FALLIDA | Mac and Cheese se adquiere, pero su representación gráfica no aparece en el mapa. |
| P-02 | Saldo insuficiente | APROBADA | Ninguno durante la prueba. |
| P-03 | Segundo plano y reanudación | APROBADA | Ninguno durante la prueba. |
| P-04 | Funcionamiento sin conexión | APROBADA | Ninguno durante la prueba. |
| P-05 | Texto ampliado | APROBADA | Ninguno durante la prueba. |


# Registro de defectos

Cuando una prueba presente un comportamiento diferente al esperado y este sea
reproducible, se registrará como un issue en el repositorio.

Cada defecto deberá incluir como mínimo:

- descripción del problema;
- pasos para reproducirlo;
- comportamiento esperado;
- comportamiento obtenido;
- dispositivo y versión de Android utilizados;
- evidencia visual cuando corresponda;
- relación con la prueba en la que fue detectado.

Los enlaces a los issues generados podrán registrarse en la siguiente tabla:

| Prueba | Defecto | Issue |
|---|---|---|
| P-01 | Mac and Cheese se registra como adquirido, pero su recurso gráfico no aparece en el mapa. | [Issue #1](https://github.com/FiniteMusic/EDU_Tycoon/issues/1) |