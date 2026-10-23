package br.edu.eep.cademeusparca.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.edu.eep.cademeusparca.model.EdicaoRole
import br.edu.eep.cademeusparca.model.PontoLocal
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.model.erroAutorizacaoEdicaoRole
import br.edu.eep.cademeusparca.model.podeEditarDadosRole
import br.edu.eep.cademeusparca.repository.EdicaoRoleRepository
import br.edu.eep.cademeusparca.repository.RoleRepository

class EditarRoleViewModel(
    private val repository: EdicaoRoleRepository = RoleRepository()
) : ViewModel() {
    // Reutiliza as transições do seletor do Passo B, com uma instância exclusiva da edição.
    var formulario by mutableStateOf<CriacaoRoleFormulario?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var salvando by mutableStateOf(false)
        private set
    var salvo by mutableStateOf(false)
        private set
    var mensagem by mutableStateOf("")
        private set
    var nomeInvalido by mutableStateOf(false)
        private set

    private var roleAtual: Role? = null
    private var roleIdAtual: String? = null
    private var uidCarregado: String? = null
    private var geracao = 0
    private var cancelado = false

    val podeSalvar: Boolean
        get() = !carregando && !salvando && !salvo && !cancelado && formulario != null &&
            repository.userIdAtual == uidCarregado &&
            podeEditarDadosRole(repository.userIdAtual, roleAtual?.adminId, roleAtual?.status)

    fun carregarRole(roleId: String) {
        if (cancelado || salvando || salvo || carregando) return
        if (roleIdAtual == roleId && formulario != null) return
        val versao = ++geracao
        roleIdAtual = roleId
        uidCarregado = repository.userIdAtual
        formulario = null
        roleAtual = null
        mensagem = ""
        if (!parametrosDetalhesRoleValidos(roleId, uidCarregado)) {
            mensagem = "Não foi possível identificar este rolê ou sua autenticação."
            return
        }
        carregando = true
        repository.buscarRolePorId(roleId) { role, erro ->
            if (geracao != versao || cancelado) return@buscarRolePorId
            carregando = false
            when {
                uidCarregado != repository.userIdAtual ->
                    mensagem = "Sua autenticação mudou. Reabra o rolê."
                erro != null || role == null ->
                    mensagem = erro ?: "Rolê não encontrado."
                else -> {
                    val erroAcesso = erroAutorizacaoEdicaoRole(
                        repository.userIdAtual, role.adminId, role.status
                    )
                    if (erroAcesso != null) mensagem = erroAcesso
                    else {
                        roleAtual = role
                        formulario = CriacaoRoleFormulario(
                            nome = role.nome,
                            nomeLocal = role.nomeLocal,
                            endereco = role.endereco,
                            pontoSelecionado = PontoLocal.deCoordenadas(
                                role.latitudeLocal, role.longitudeLocal
                            )
                        )
                    }
                }
            }
        }
    }

    private fun editarFormulario(acao: (CriacaoRoleFormulario) -> CriacaoRoleFormulario) {
        if (!podeSalvar) return
        formulario = formulario?.let(acao)
        mensagem = ""
    }

    fun atualizarNome(valor: String) {
        editarFormulario { it.copy(nome = valor) }
        if (podeSalvar) nomeInvalido = false
    }

    fun atualizarNomeLocal(valor: String) = editarFormulario { it.copy(nomeLocal = valor) }
    fun atualizarEndereco(valor: String) = editarFormulario { it.copy(endereco = valor) }
    fun abrirSeletorLocal() = editarFormulario { it.abrirSeletor() }
    fun selecionarPonto(latitude: Double, longitude: Double) =
        editarFormulario { it.selecionarPonto(latitude, longitude) }
    fun confirmarLocal() = editarFormulario { it.confirmarLocal() }
    fun cancelarSelecaoLocal() {
        if (!salvando) formulario = formulario?.cancelarSelecao()
    }
    fun limparLocal() = editarFormulario { it.limparLocal() }

    fun salvarAlteracoes() {
        if (carregando || salvando || salvo || cancelado) return
        val atual = formulario ?: return
        if (!podeSalvar) {
            mensagem = "Sua autorização mudou. Reabra o rolê."
            return
        }
        val edicao = EdicaoRole(
            nome = atual.nome, nomeLocal = atual.nomeLocal, endereco = atual.endereco,
            latitudeLocal = atual.pontoSelecionado?.latitude,
            longitudeLocal = atual.pontoSelecionado?.longitude
        )
        edicao.erroValidacao()?.let {
            nomeInvalido = atual.nome.isBlank()
            mensagem = it
            return
        }
        val roleId = roleIdAtual ?: return
        val versao = geracao
        salvando = true
        mensagem = ""
        nomeInvalido = false
        repository.atualizarDadosRole(roleId, edicao) { sucesso, erro ->
            if (geracao != versao || cancelado) return@atualizarDadosRole
            salvando = false
            if (uidCarregado != repository.userIdAtual) {
                mensagem = "Sua autenticação mudou. Reabra o rolê."
            } else {
                salvo = sucesso
                mensagem = if (sucesso) "" else erro ?: "Não foi possível salvar as alterações."
            }
        }
    }

    /** Cancelar não chama o repository. Durante a transação, aguarda a confirmação do servidor. */
    fun cancelarEdicao(): Boolean {
        if (salvando) return false
        cancelado = true
        geracao++
        carregando = false
        formulario = null
        return true
    }

    override fun onCleared() {
        geracao++
        cancelado = true
    }
}
