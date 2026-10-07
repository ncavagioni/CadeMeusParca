package br.edu.eep.cademeusparca.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class AutorizacaoDetalhesParcaTest {
    private var inicios = 0
    private var negacoes = 0
    private val autorizacao = AutorizacaoDetalhesParca(
        aoAutorizar = { inicios++ },
        aoNegar = { negacoes++ }
    )

    @Test
    fun semConfirmacoesMantemDadosPendentes() {
        assertEquals(0, inicios)
        assertEquals(0, negacoes)
    }

    @Test
    fun usuarioPrimeiroEsperaParcaAntesDeIniciarDados() {
        autorizacao.atualizarUsuarioAtual(true)
        assertEquals(0, inicios)
        autorizacao.atualizarParca(true)
        assertEquals(1, inicios)
        assertEquals(0, negacoes)
    }

    @Test
    fun parcaPrimeiroEsperaUsuarioAntesDeIniciarDados() {
        autorizacao.atualizarParca(true)
        assertEquals(0, inicios)
        autorizacao.atualizarUsuarioAtual(true)
        assertEquals(1, inicios)
        assertEquals(0, negacoes)
    }

    @Test
    fun confirmacoesRepetidasNaoDuplicamListenersDeDados() {
        repeat(3) {
            autorizacao.atualizarUsuarioAtual(true)
            autorizacao.atualizarParca(true)
        }
        assertEquals(1, inicios)
    }

    @Test
    fun negacaoDeQualquerParticipanteImpedeInicioMesmoComCallbacksPosteriores() {
        for (negarUsuario in listOf(true, false)) {
            var iniciou = 0
            var negou = 0
            val controle = AutorizacaoDetalhesParca({ iniciou++ }, { negou++ })
            if (negarUsuario) controle.atualizarUsuarioAtual(false) else controle.atualizarParca(false)
            controle.atualizarUsuarioAtual(true)
            controle.atualizarParca(true)
            controle.atualizarParca(false)
            assertEquals(0, iniciou)
            assertEquals(1, negou)
        }
    }

    @Test
    fun remocaoDeQualquerParticipanteRevogaAcessoDepoisDaAutorizacao() {
        for (removerUsuario in listOf(true, false)) {
            var iniciou = 0
            var negou = 0
            val controle = AutorizacaoDetalhesParca({ iniciou++ }, { negou++ })
            controle.atualizarParca(true)
            controle.atualizarUsuarioAtual(true)
            if (removerUsuario) controle.atualizarUsuarioAtual(false) else controle.atualizarParca(false)
            controle.atualizarUsuarioAtual(true)
            controle.atualizarParca(true)
            assertEquals(1, iniciou)
            assertEquals(1, negou)
        }
    }
}
