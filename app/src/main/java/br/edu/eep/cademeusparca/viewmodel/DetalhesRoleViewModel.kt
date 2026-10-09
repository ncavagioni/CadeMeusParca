package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.repository.CodigoRoleClipboardRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import br.edu.eep.cademeusparca.repository.UsuarioRepository
import com.google.firebase.firestore.ListenerRegistration

class DetalhesRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
    private val usuarioRepository = UsuarioRepository()
    private val clipboardRepository = CodigoRoleClipboardRepository()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var geracao = 0
    private var roleObservado: String? = null
    private var uidObservado: String? = null
    private var autorizado by mutableStateOf(false)
    private var role by mutableStateOf<Role?>(null)
    private var perfis by mutableStateOf<List<PerfilPublicoRole>>(emptyList())
    private var quantidade by mutableStateOf<Int?>(null)
    private var erroPerfis by mutableStateOf("")
    private var erroParticipantes by mutableStateOf("")

    var mensagem by mutableStateOf("")
        private set
    var mensagemAcao by mutableStateOf("")
        private set

    val dados: DetalhesRoleUi?
        get() = role?.takeIf { acessoValido() }?.let {
            comporDetalhesRole(it, uidObservado.orEmpty(), perfis, quantidade)
        }
    val carregando: Boolean
        get() = mensagem.isBlank() && dados == null
    val aviso: String
        get() = if (acessoValido()) listOf(erroPerfis, erroParticipantes)
            .filter { it.isNotBlank() }.distinct().joinToString("\n") else ""

    fun iniciarObservacao(roleId: String) {
        val uid = usuarioRepository.userIdAtual
        if (roleObservado == roleId && uidObservado == uid) return
        pararObservacao()
        limparDados()
        mensagem = ""
        mensagemAcao = ""
        if (!parametrosDetalhesRoleValidos(roleId, uid)) {
            falhar("Não foi possível identificar este rolê ou sua autenticação.")
            return
        }
        roleObservado = roleId
        uidObservado = uid
        val versao = geracao
        registrar(usuarioRepository.observarAutenticacao {
            if (geracao == versao && it != uid) falhar("Sua autenticação mudou. Reabra o rolê.")
        }, versao)
        if (!aceita(versao)) return
        registrar(roleRepository.observarParticipacao(roleId, uid!!) { pertence, erro ->
            if (!aceita(versao)) return@observarParticipacao
            if (erro != null || !pertence) {
                falhar("Não foi possível confirmar sua participação neste rolê.")
            } else {
                // O repository ignora cache antigo para confirmar participação.
                autorizado = true
            }
        }, versao)
        if (!aceita(versao)) return
        // Leitura existente do servidor, em paralelo à confirmação de participação.
        // O getter dados mantém todas as informações ocultas até a autorização.
        roleRepository.buscarRolePorId(roleId) { novoRole, erro ->
            if (!aceita(versao)) return@buscarRolePorId
            if (erro != null || novoRole == null) falhar(erro ?: "Rolê não encontrado.")
            else role = novoRole
        }
        if (!aceita(versao)) return
        registrar(usuarioRepository.observarPerfisPublicosDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarPerfisPublicosDoRole
            perfis = if (erro == null) lista.orEmpty() else emptyList()
            erroPerfis = if (erro == null) "" else "Nome do administrador indisponível."
        }, versao)
        if (!aceita(versao)) return
        registrar(roleRepository.observarParticipantesDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarParticipantesDoRole
            quantidade = if (erro == null) lista.orEmpty().distinctBy { it.userId }.size else -1
            erroParticipantes = if (erro == null) "" else "Não foi possível carregar a quantidade de participantes."
        }, versao)
    }

    fun podeVerParcas(): Boolean = acessoValido() && role != null

    fun copiarCodigo() {
        val codigo = dados?.codigo?.takeIf { it.isNotBlank() } ?: return
        mensagemAcao = if (clipboardRepository.copiar(getApplication(), codigo)) "Código copiado"
            else "Não foi possível copiar o código."
    }

    fun consumirMensagemAcao() {
        mensagemAcao = ""
    }

    private fun aceita(versao: Int): Boolean =
        geracao == versao && roleObservado != null && uidObservado == usuarioRepository.userIdAtual

    private fun acessoValido(): Boolean = autorizado && aceita(geracao) && mensagem.isBlank()

    private fun registrar(listener: ListenerRegistration?, versao: Int) {
        if (listener == null) return
        if (aceita(versao)) listeners.add(listener) else listener.remove()
    }

    private fun limparDados() {
        autorizado = false
        role = null
        perfis = emptyList()
        quantidade = null
        erroPerfis = ""
        erroParticipantes = ""
    }

    private fun falhar(erro: String) {
        pararObservacao()
        limparDados()
        mensagemAcao = ""
        mensagem = erro
    }

    fun pararObservacao() {
        autorizado = false
        roleObservado = null
        geracao++
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    override fun onCleared() {
        pararObservacao()
        super.onCleared()
    }
}
