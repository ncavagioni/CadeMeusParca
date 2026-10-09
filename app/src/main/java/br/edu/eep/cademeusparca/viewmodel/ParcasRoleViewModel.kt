package br.edu.eep.cademeusparca.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.edu.eep.cademeusparca.location.RelogioLocalizacao
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.model.ParcaListaUi
import br.edu.eep.cademeusparca.model.ParticipanteRole
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.repository.CompartilhamentoLocalizacaoRepository
import br.edu.eep.cademeusparca.repository.LocalizacaoRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import br.edu.eep.cademeusparca.repository.UsuarioRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class ParcasRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
    private val usuarioRepository = UsuarioRepository()
    private val localizacaoRepository = LocalizacaoRepository()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var ticker: Job? = null
    private var observacaoAndroid: Job? = null
    private var geracao = 0
    private var roleObservado: String? = null
    private var participantes by mutableStateOf<List<ParticipanteRole>>(emptyList())
    private var perfis by mutableStateOf<List<PerfilPublicoRole>>(emptyList())
    private var localizacoes by mutableStateOf<List<LocalizacaoParticipante>>(emptyList())
    private var propriaAndroid by mutableStateOf<Location?>(null)
    private var agoraMs by mutableStateOf(System.currentTimeMillis())

    var userIdAtual by mutableStateOf("")
        private set
    var carregando by mutableStateOf(true)
        private set
    var mensagem by mutableStateOf("")
        private set

    val parcas: List<ParcaListaUi>
        get() {
            val origem = propriaAndroid?.takeIf { temPermissao() }?.let {
                LocalizacaoParticipante(userIdAtual, it.latitude, it.longitude)
            }
            return comporParcasDoRole(
                participantes, perfis, localizacoes, userIdAtual, origem, agoraMs
            )
        }

    fun podeAbrirParca(userId: String): Boolean =
        roleObservado != null && usuarioRepository.userIdAtual == userIdAtual &&
            userId != userIdAtual && participantes.any { it.userId == userId }

    fun iniciarObservacao(roleId: String, posicaoInicial: Location? = null) {
        val uid = usuarioRepository.userIdAtual
        if (roleObservado == roleId && userIdAtual == uid) return
        pararObservacao()
        participantes = emptyList()
        perfis = emptyList()
        localizacoes = emptyList()
        propriaAndroid = posicaoInicial?.let(::Location)
        carregando = true
        mensagem = ""
        if (uid.isNullOrBlank() || roleId.isBlank() || '/' in roleId || roleId == "." || roleId == "..") {
            falhar("Não foi possível identificar os parças deste rolê.")
            return
        }
        userIdAtual = uid
        roleObservado = roleId
        val versao = geracao
        ticker = RelogioLocalizacao.iniciar(viewModelScope) { agoraMs = it }
        observacaoAndroid = viewModelScope.launch {
            CompartilhamentoLocalizacaoRepository.estado.collect { estado ->
                if (aceita(versao) && estado.roleId == roleId && estado.userId == uid && temPermissao()) {
                    val nova = estado.localizacao
                    if (nova != null && (propriaAndroid == null ||
                            nova.elapsedRealtimeNanos >= propriaAndroid!!.elapsedRealtimeNanos)
                    ) propriaAndroid = Location(nova)
                }
            }
        }
        registrar(usuarioRepository.observarAutenticacao {
            if (geracao == versao && it != uid) falhar("Sua autenticação mudou. Reabra o rolê.")
        }, versao)
        if (!aceita(versao)) return
        registrar(roleRepository.observarParticipantesDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarParticipantesDoRole
            if (erro != null) falhar(erro) else {
                participantes = lista.orEmpty()
                carregando = false
            }
        }, versao)
        if (!aceita(versao)) return
        registrar(usuarioRepository.observarPerfisPublicosDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarPerfisPublicosDoRole
            if (erro != null) falhar(erro) else perfis = lista.orEmpty()
        }, versao)
        if (!aceita(versao)) return
        registrar(localizacaoRepository.observarLocalizacoesDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarLocalizacoesDoRole
            if (erro != null) falhar(erro) else localizacoes = lista.orEmpty()
        }, versao)
    }

    private fun aceita(versao: Int): Boolean =
        geracao == versao && roleObservado != null && usuarioRepository.userIdAtual == userIdAtual

    private fun registrar(listener: ListenerRegistration?, versao: Int) {
        if (listener == null) return
        if (aceita(versao)) listeners.add(listener) else listener.remove()
    }

    private fun falhar(erro: String) {
        pararObservacao()
        participantes = emptyList()
        perfis = emptyList()
        localizacoes = emptyList()
        propriaAndroid = null
        carregando = false
        mensagem = erro
    }

    fun pararObservacao() {
        roleObservado = null
        geracao++
        listeners.forEach { it.remove() }
        listeners.clear()
        ticker?.cancel()
        ticker = null
        observacaoAndroid?.cancel()
        observacaoAndroid = null
    }

    private fun temPermissao(): Boolean {
        val context = getApplication<Application>()
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        pararObservacao()
        super.onCleared()
    }
}

/** Somente participantes criam linhas; perfis e posições apenas complementam membros. */
fun comporParcasDoRole(
    participantes: List<ParticipanteRole>,
    perfis: List<PerfilPublicoRole>,
    localizacoes: List<LocalizacaoParticipante>,
    userIdAtual: String,
    propriaAndroid: LocalizacaoParticipante?,
    agoraMs: Long
): List<ParcaListaUi> {
    val perfisPorUsuario = perfis.associateBy { it.userId }
    val posicoesPorUsuario = localizacoes.associateBy { it.userId }
    val android = propriaAndroid?.takeIf { it.userId == userIdAtual }
    val propriaSalva = posicoesPorUsuario[userIdAtual]
    val linhas = participantes.filter { it.userId.isNotBlank() }.distinctBy { it.userId }.map { membro ->
        val proprio = membro.userId == userIdAtual
        val nome = perfisPorUsuario[membro.userId]?.parcaname?.trim()?.takeIf { it.isNotBlank() } ?: "Parça"
        val informacao = if (proprio) {
            "Sua localização"
        } else {
            val estado = estadoLocalizacaoParca(
                posicoesPorUsuario[membro.userId], android, propriaSalva, agoraMs
            )
            estado.distancia ?: estado.textoPrincipal
        }
        ParcaListaUi(membro.userId, nome, proprio, informacao)
    }
    val alfabeto = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
        strength = Collator.PRIMARY
    }
    return linhas.sortedWith { a, b ->
        when {
            a.proprioUsuario != b.proprioUsuario -> if (a.proprioUsuario) -1 else 1
            else -> alfabeto.compare(a.parcaname, b.parcaname).takeIf { it != 0 }
                ?: a.userId.compareTo(b.userId)
        }
    }
}
