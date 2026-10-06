package br.edu.eep.cademeusparca.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilaUltimaPosicaoTest {
    @Test
    fun soUmaEscritaPodeAguardarConfirmacao() {
        val fila = FilaUltimaPosicao<String>()
        fila.oferecer("primeira")
        assertEquals("primeira", fila.retirarParaGravar())
        fila.oferecer("segunda")
        assertNull(fila.retirarParaGravar())
        fila.concluir()
        assertEquals("segunda", fila.retirarParaGravar())
    }

    @Test
    fun durantePerdaDeRedeRetemApenasAPosicaoMaisRecente() {
        val fila = FilaUltimaPosicao<Int>()
        fila.oferecer(1)
        assertEquals(1, fila.retirarParaGravar())
        for (posicao in 2..100) {
            fila.oferecer(posicao)
            assertNull(fila.retirarParaGravar())
        }
        fila.concluir()
        assertEquals(100, fila.retirarParaGravar())
        fila.concluir()
        assertNull(fila.retirarParaGravar())
    }

    @Test
    fun pausaDescartaPendenteMasMantemTaskAnteriorProtegidaNoRetorno() {
        val fila = FilaUltimaPosicao<String>()
        fila.oferecer("sessao anterior em andamento")
        assertEquals("sessao anterior em andamento", fila.retirarParaGravar())
        fila.oferecer("pendente anterior")
        fila.descartarPendente()
        fila.oferecer("nova sessao")
        assertNull(fila.retirarParaGravar())
        fila.concluir()
        assertEquals("nova sessao", fila.retirarParaGravar())
    }

    @Test
    fun completarAposSaidaNaoProduzNovaEscrita() {
        val fila = FilaUltimaPosicao<Int>()
        fila.oferecer(1)
        fila.retirarParaGravar()
        fila.oferecer(2)
        fila.descartarPendente()
        fila.concluir()
        assertNull(fila.retirarParaGravar())
    }

    @Test
    fun falhaLiberaFilaERetentaSemNecessitarOutroCallback() {
        val fila = FilaUltimaPosicao<Int>()
        fila.oferecer(1)
        val falhou = fila.retirarParaGravar()!!
        fila.concluir()
        fila.reporSeVazia(falhou)
        assertEquals(1, fila.retirarParaGravar())
    }

    @Test
    fun posicaoMaisNovaTemPrioridadeSobreEscritaQueFalhou() {
        val fila = FilaUltimaPosicao<Int>()
        fila.oferecer(1)
        val falhou = fila.retirarParaGravar()!!
        fila.oferecer(2)
        fila.concluir()
        fila.reporSeVazia(falhou)
        assertEquals(2, fila.retirarParaGravar())
    }

    @Test
    fun manualEAutomaticoCompartilhamAMesmaProtecao() {
        val fila = FilaUltimaPosicao<String>()
        fila.oferecer("automatico anterior")
        fila.retirarParaGravar()
        fila.oferecer("manual")
        assertNull(fila.retirarParaGravar())
        fila.oferecer("automatico mais recente")
        fila.concluir()
        assertEquals("automatico mais recente", fila.retirarParaGravar())
    }
}
