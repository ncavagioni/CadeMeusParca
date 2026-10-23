package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.PontoLocal

/** O mapa edita um rascunho; somente a confirmação altera o ponto do formulário. */
data class CriacaoRoleFormulario(
    val nome: String = "",
    val nomeLocal: String = "",
    val endereco: String = "",
    val pontoSelecionado: PontoLocal? = null,
    val selecionandoLocal: Boolean = false,
    val pontoRascunho: PontoLocal? = null
) {
    fun abrirSeletor() = copy(selecionandoLocal = true, pontoRascunho = pontoSelecionado)

    fun selecionarPonto(latitude: Double, longitude: Double): CriacaoRoleFormulario {
        if (!selecionandoLocal) return this
        val ponto = PontoLocal.deCoordenadas(latitude, longitude) ?: return this
        return copy(pontoRascunho = ponto)
    }

    fun confirmarLocal(): CriacaoRoleFormulario {
        if (!selecionandoLocal || pontoRascunho == null) return this
        return copy(pontoSelecionado = pontoRascunho, selecionandoLocal = false, pontoRascunho = null)
    }

    fun cancelarSelecao() = copy(selecionandoLocal = false, pontoRascunho = null)

    fun limparLocal() = copy(pontoSelecionado = null, pontoRascunho = null)
}
