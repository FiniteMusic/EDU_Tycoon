package io.moviles.IPN_Tycoon

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.widget.VisWindow

/** Ventanas del paso 5. Todo el contenido es desplazable en pantallas Android pequeñas. */
private fun mensaje(stage: Stage, titulo: String, contenido: String) {
    val dialogo = VisWindow(titulo)
    dialogo.isModal = true
    dialogo.addCloseButton()
    val cuerpo = Table().apply { background = UIEstiloPaso5.panel; pad(18f) }
    val texto = UIEstiloPaso5.etiqueta(contenido)
    texto.setWrap(true)
    cuerpo.add(texto).width(360f).padBottom(14f).row()
    val aceptar = UIEstiloPaso5.boton("ACEPTAR", true)
    aceptar.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) { dialogo.remove() }
    })
    cuerpo.add(aceptar).width(160f).height(42f)
    dialogo.add(cuerpo).width(420f)
    dialogo.pack(); dialogo.centerWindow()
    stage.addActor(dialogo); dialogo.toFront()
}

class MinijuegoWindow(
    private val propiedad: Propiedad,
    private val alCambiarEstado: () -> Unit
) : VisWindow("Misión académica") {
    fun mostrarEn(stage: Stage) {
        val nivel = ProgresoAdministrador.siguienteReto(propiedad.id)
        val reto = CatalogoRetos.obtener(propiedad.id, nivel)
        if (reto == null) {
            mensaje(stage, "Reto no encontrado", "No se encontró la actividad de ${propiedad.nombre}.")
            return
        }
        if (ProgresoAdministrador.energia <= 0) {
            mensaje(stage, "Energía agotada", "Visita Dormitorios para recuperar energía.")
            return
        }
        isModal = true
        addCloseButton()
        val contenido = Table().apply { background = UIEstiloPaso5.panel; pad(16f) }
        val repaso = ProgresoAdministrador.completados(propiedad.id) >= ProgresoAdministrador.RETOS_POR_EDIFICIO
        contenido.add(UIEstiloPaso5.etiqueta(propiedad.nombre.uppercase(), UIEstiloPaso5.oro))
            .width(408f).padBottom(8f).left().row()
        contenido.add(UIEstiloPaso5.etiqueta(if (repaso) "REPASO FINAL" else "RETO $nivel DE 5", UIEstiloPaso5.celeste))
            .width(408f).padBottom(10f).left().row()
        contenido.add(UIEstiloPaso5.etiqueta("Recompensa de conocimiento y dinero al acertar · Error: -2 energía", UIEstiloPaso5.verde))
            .width(408f).padBottom(12f).left().row()
        val pregunta = UIEstiloPaso5.etiqueta(reto.pregunta)
        pregunta.setWrap(true)
        contenido.add(pregunta).width(408f).padBottom(14f).left().row()
        reto.opciones.forEachIndexed { index, opcion ->
            val boton = UIEstiloPaso5.boton("${'A' + index}.  $opcion")
            boton.label.setWrap(true)
            boton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val resultado = ProgresoAdministrador.jugar(propiedad, index == reto.correcta)
                    alCambiarEstado()
                    remove()
                    mensaje(stage, if (resultado.correcto) "¡Misión superada!" else "Respuesta incorrecta", resultado.mensaje)
                }
            })
            contenido.add(boton).width(408f).height(58f).padBottom(8f).row()
        }
        val desplazar = ScrollPane(contenido)
        desplazar.setFadeScrollBars(false)
        add(desplazar).width(460f).height(355f)
        pack(); centerWindow()
        stage.addActor(this); toFront()
    }
}

class VentanaComida(
    private val esRestaurante: Boolean,
    private val alCambiarEstado: () -> Unit
) : VisWindow(if (esRestaurante) "Restaurante universitario" else "Comida del campus") {
    fun mostrarEn(stage: Stage) {
        isModal = true
        addCloseButton()
        val menu = if (esRestaurante) ProgresoAdministrador.menuRestaurante else ProgresoAdministrador.menuPuesto
        val rutas = if (esRestaurante) listOf("ensalada", "menu", "plato") else listOf("fruta", "sandwich", "casera")
        val lista = Table().apply { background = UIEstiloPaso5.panel; pad(14f) }
        lista.add(UIEstiloPaso5.etiqueta("MENÚ DEL DÍA", UIEstiloPaso5.oro)).left().padBottom(5f).row()
        lista.add(UIEstiloPaso5.etiqueta("Salud actual: ${ProgresoAdministrador.salud}/100", UIEstiloPaso5.verde))
            .left().padBottom(12f).row()
        menu.forEachIndexed { i, alimento ->
            val tarjeta = Table().apply { background = UIEstiloPaso5.panelSecundario; pad(7f) }
            val icono = UIEstiloPaso5.icono("ui/comida/${rutas[i]}.png")
            if (icono != null) tarjeta.add(icono).size(62f).padRight(10f)
            val info = Table()
            val titulo = UIEstiloPaso5.etiqueta(alimento.nombre, UIEstiloPaso5.oro)
            titulo.setWrap(true)
            info.add(titulo).width(185f).left().row()
            info.add(UIEstiloPaso5.etiqueta("\$${alimento.precio} · +${alimento.recuperacion} salud", UIEstiloPaso5.verde))
                .left().padTop(4f)
            tarjeta.add(info).width(185f).padRight(9f)
            val comprar = UIEstiloPaso5.boton("COMPRAR", true)
            comprar.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val ok = ProgresoAdministrador.comer(alimento)
                    if (ok) alCambiarEstado()
                    remove()
                    mensaje(stage, "Servicio de alimentos", if (ok)
                        "Consumiste ${alimento.nombre}. Salud actual: ${ProgresoAdministrador.salud}/100."
                        else "No tienes dinero suficiente para este platillo.")
                }
            })
            tarjeta.add(comprar).width(94f).height(42f)
            lista.add(tarjeta).width(435f).padBottom(9f).row()
        }
        val scroll = ScrollPane(lista)
        scroll.setFadeScrollBars(false)
        add(scroll).width(475f).height(348f)
        pack(); centerWindow()
        stage.addActor(this); toFront()
    }
}

class VentanaDescanso(private val alCambiarEstado: () -> Unit) : VisWindow("Dormitorios") {
    fun mostrarEn(stage: Stage) {
        isModal = true
        addCloseButton()
        val cuerpo = Table().apply { background = UIEstiloPaso5.panel; pad(18f) }
        cuerpo.add(UIEstiloPaso5.etiqueta("ÁREA DE DESCANSO", UIEstiloPaso5.oro)).padBottom(12f).row()
        cuerpo.add(UIEstiloPaso5.etiqueta("Energía: ${ProgresoAdministrador.energia}/20", UIEstiloPaso5.verde))
            .padBottom(12f).row()
        val barra = UIEstiloPaso5.barra(20f, ProgresoAdministrador.energia.toFloat(), "energia")
        cuerpo.add(barra).width(320f).height(16f).padBottom(15f).row()
        val descansar = UIEstiloPaso5.boton("DESCANSAR Y RECUPERAR ENERGÍA", true)
        descansar.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val recuperado = ProgresoAdministrador.descansarEnDormitorios()
                alCambiarEstado(); remove()
                mensaje(stage, "Descanso completado", "Recuperaste $recuperado puntos de energía. Total: 20/20.")
            }
        })
        cuerpo.add(descansar).width(335f).height(52f)
        add(cuerpo).width(390f)
        pack(); centerWindow()
        stage.addActor(this); toFront()
    }
}
