package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.PontoLocal
import org.junit.Assert.*
import org.junit.Test

class CriacaoRoleFormularioTest {
    private val formulario = CriacaoRoleFormulario(
        nome = "Aniversário", nomeLocal = "Casa do parça", endereco = "Rua A, 123"
    )
    private val primeiro = PontoLocal(-22.725, -47.649)
    private val segundo = PontoLocal(-23.5505, -46.6333)

    @Test
    fun abrirECancelarSemSelecaoPreservaTodosOsDadosManuais() {
        assertEquals(formulario, formulario.abrirSeletor().cancelarSelecao())
    }

    @Test
    fun confirmarSemPontoMantemSeletorAberto() {
        val aberto = formulario.abrirSeletor()
        assertEquals(aberto, aberto.confirmarLocal())
        assertNull(aberto.pontoSelecionado)
    }

    @Test
    fun tocarNoMapaAlteraSomenteORascunho() {
        val tocado = formulario.abrirSeletor().selecionarPonto(primeiro.latitude, primeiro.longitude)
        assertNull(tocado.pontoSelecionado)
        assertEquals(primeiro, tocado.pontoRascunho)
        assertEquals(formulario.nome, tocado.nome)
        assertEquals(formulario.nomeLocal, tocado.nomeLocal)
        assertEquals(formulario.endereco, tocado.endereco)
    }

    @Test
    fun confirmarAplicaPontoEFechaSeletor() {
        val confirmado = formulario.abrirSeletor()
            .selecionarPonto(primeiro.latitude, primeiro.longitude).confirmarLocal()
        assertEquals(formulario.copy(pontoSelecionado = primeiro), confirmado)
    }

    @Test
    fun toquesSeguintesSubstituemRascunho() {
        val confirmado = formulario.abrirSeletor()
            .selecionarPonto(primeiro.latitude, primeiro.longitude)
            .selecionarPonto(segundo.latitude, segundo.longitude).confirmarLocal()
        assertEquals(segundo, confirmado.pontoSelecionado)
    }

    @Test
    fun cancelarDescartaPontoNovoSemApagarFormulario() {
        val cancelado = formulario.abrirSeletor()
            .selecionarPonto(primeiro.latitude, primeiro.longitude).cancelarSelecao()
        assertEquals(formulario, cancelado)
    }

    @Test
    fun cancelarAlteracaoConservaPontoAnteriorEDadosManuais() {
        val anterior = formulario.copy(pontoSelecionado = primeiro)
        val cancelado = anterior.abrirSeletor()
            .selecionarPonto(segundo.latitude, segundo.longitude).cancelarSelecao()
        assertEquals(anterior, cancelado)
    }

    @Test
    fun reabrirSeletorComecaNoPontoConfirmado() {
        val reaberto = formulario.copy(pontoSelecionado = primeiro).abrirSeletor()
        assertTrue(reaberto.selecionandoLocal)
        assertEquals(primeiro, reaberto.pontoRascunho)
    }

    @Test
    fun confirmarAlteracaoSubstituiPontoAnterior() {
        val alterado = formulario.copy(pontoSelecionado = primeiro).abrirSeletor()
            .selecionarPonto(segundo.latitude, segundo.longitude).confirmarLocal()
        assertEquals(formulario.copy(pontoSelecionado = segundo), alterado)
    }

    @Test
    fun limparSelecaoPreservaTextosEPermiteCriacaoSemCoordenadas() {
        assertEquals(formulario, formulario.copy(pontoSelecionado = primeiro).limparLocal())
    }

    @Test
    fun pontoInvalidoNaoSubstituiRascunhoValido() {
        val valido = formulario.copy(pontoSelecionado = primeiro).abrirSeletor()
        listOf(91.0 to 0.0, 0.0 to 181.0, Double.NaN to 0.0, 0.0 to Double.NEGATIVE_INFINITY)
            .forEach { assertEquals(valido, valido.selecionarPonto(it.first, it.second)) }
    }

    @Test
    fun selecaoForaDoFluxoDoMapaNaoAlteraFormulario() {
        assertEquals(formulario, formulario.selecionarPonto(primeiro.latitude, primeiro.longitude))
        assertEquals(formulario, formulario.confirmarLocal())
    }
}
