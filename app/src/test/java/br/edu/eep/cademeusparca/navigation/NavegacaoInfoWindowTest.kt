package br.edu.eep.cademeusparca.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavegacaoInfoWindowTest {
    private val fila = ArrayDeque<() -> Unit>()
    private val navegacao = NavegacaoInfoWindow { fila.addLast(it) }

    @Test
    fun navegacaoEsperaCallbackLiberarOMapa() {
        var callbackEmExecucao = true
        var abriuDetalhes = false

        navegacao.solicitar(podeNavegar = { true }) {
            // Simula a mudança de lifecycle que provoca MapView.onStop().
            assertFalse("O lifecycle não pode mudar dentro do callback do Maps", callbackEmExecucao)
            abriuDetalhes = true
        }

        assertFalse(abriuDetalhes)
        assertEquals(1, fila.size)
        callbackEmExecucao = false
        fila.removeFirst().invoke()
        assertTrue(abriuDetalhes)
    }

    @Test
    fun cliquesRepetidosPendentesAbremUmaUnicaTela() {
        var aberturas = 0
        repeat(3) {
            navegacao.solicitar(podeNavegar = { true }) { aberturas++ }
        }

        assertEquals(1, fila.size)
        fila.removeFirst().invoke()
        assertEquals(1, aberturas)
    }

    @Test
    fun naoNavegaSeEntradaDoMapaDeixouDeSerAtual() {
        var mapaAtual = true
        var aberturas = 0
        navegacao.solicitar(podeNavegar = { mapaAtual }) { aberturas++ }

        mapaAtual = false
        fila.removeFirst().invoke()
        assertEquals(0, aberturas)
    }

    @Test
    fun permiteNovaSolicitacaoDepoisDeProcessarAAnterior() {
        var aberturas = 0
        navegacao.solicitar(podeNavegar = { false }) { aberturas++ }
        fila.removeFirst().invoke()

        navegacao.solicitar(podeNavegar = { true }) { aberturas++ }
        fila.removeFirst().invoke()
        assertEquals(1, aberturas)
    }
}
