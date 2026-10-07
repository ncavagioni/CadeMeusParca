package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.location.RelogioLocalizacao
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.repository.AcoesExternasRepository
import br.edu.eep.cademeusparca.repository.CompartilhamentoLocalizacaoRepository
import br.edu.eep.cademeusparca.repository.LocalizacaoRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import br.edu.eep.cademeusparca.repository.UsuarioRepository
import br.edu.eep.cademeusparca.util.TelefoneUtils
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DetalhesParcaViewModel(application: Application) : AndroidViewModel(application) {
    private val usuarioRepository = UsuarioRepository()
    private val roleRepository = RoleRepository()
    private val localizacaoRepository = LocalizacaoRepository()
    // Usado apenas para conferir permissão; esta tela não solicita posições ao GPS.
    private val locationRepository = LocationRepository(application)
    private val acoesExternas = AcoesExternasRepository()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var ticker: Job? = null
    private var observacaoAndroid: Job? = null
    private var geracao = 0
    private var observando = false
    private var roleObservado: String? = null
    private var uidObservado: String? = null
    private var parcaObservado: String? = null
    private var localizacoes by mutableStateOf<List<LocalizacaoParticipante>>(emptyList())
    private var propriaAndroid by mutableStateOf<Location?>(null)
    private var agoraMs by mutableStateOf(System.currentTimeMillis())

    var perfil by mutableStateOf<PerfilPublicoRole?>(null)
        private set
    var carregando by mutableStateOf(true)
        private set
    var mensagem by mutableStateOf("")
        private set
    var mensagemAcao by mutableStateOf("")
        private set

    val localizacao: EstadoLocalizacaoParca
        get() {
            val android = propriaAndroid?.takeIf { locationRepository.temPermissao() }?.let {
                LocalizacaoParticipante(
                    userId = uidObservado.orEmpty(), latitude = it.latitude, longitude = it.longitude
                )
            }
            return estadoLocalizacaoParca(
                participante = localizacoes.firstOrNull { it.userId == parcaObservado },
                propriaAndroid = android,
                propriaSalva = localizacoes.firstOrNull { it.userId == uidObservado },
                agoraMs = agoraMs
            )
        }

    val podeLigar: Boolean
        get() = acessoValido() && TelefoneUtils.uriDiscador(perfil?.telefone.orEmpty()) != null
    val podeWhatsApp: Boolean
        get() = acessoValido() && TelefoneUtils.uriWhatsApp(perfil?.telefone.orEmpty()) != null
    val podeTracarRota: Boolean
        get() = acessoValido() && localizacao.destino != null

    fun iniciarObservacao(roleId: String, userId: String, posicaoInicial: Location? = null) {
        val uid = usuarioRepository.userIdAtual
        if (observando && roleObservado == roleId && parcaObservado == userId && uidObservado == uid) return
        pararObservacao()
        perfil = null
        localizacoes = emptyList()
        propriaAndroid = posicaoInicial?.let(::Location)
        mensagem = ""
        mensagemAcao = ""
        carregando = true
        if (!parametrosDetalhesValidos(roleId, userId, uid)) {
            falhar()
            return
        }
        observando = true
        roleObservado = roleId
        uidObservado = uid
        parcaObservado = userId
        val versao = geracao
        val inicioMs = SystemClock.elapsedRealtime()
        Log.d("DetalhesParcaVM", "TELA09_LOAD_START geracao=$versao elapsedMs=0")
        registrar(usuarioRepository.observarAutenticacao {
            if (geracao == versao && it != uid) falhar()
        }, versao)

        val autorizacao = AutorizacaoDetalhesParca(
            aoAutorizar = {
                Log.d(
                    "DetalhesParcaVM",
                    "TELA09_AUTH_CONFIRMED geracao=$versao elapsedMs=${SystemClock.elapsedRealtime() - inicioMs}"
                )
                observarDados(roleId, userId, versao, inicioMs)
            },
            aoNegar = { falhar() }
        )
        // Ambos os listeners são registrados sem esperar o retorno do outro.
        // O repository só confirma participação com snapshot recebido do servidor.
        if (!aceita(versao)) return
        registrar(roleRepository.observarParticipacao(roleId, uid!!) { pertence, erro ->
            if (aceita(versao)) autorizacao.atualizarUsuarioAtual(erro == null && pertence)
        }, versao)
        if (!aceita(versao)) return
        registrar(roleRepository.observarParticipacao(roleId, userId) { pertence, erro ->
            if (aceita(versao)) autorizacao.atualizarParca(erro == null && pertence)
        }, versao)
    }

    private fun observarDados(roleId: String, userId: String, versao: Int, inicioMs: Long) {
        var dadosProntosRegistrados = false
        ticker = RelogioLocalizacao.iniciar(viewModelScope) { agoraMs = it }
        observacaoAndroid = viewModelScope.launch {
            CompartilhamentoLocalizacaoRepository.estado.collect { estado ->
                if (aceita(versao) && estado.roleId == roleId &&
                    estado.userId == uidObservado && locationRepository.temPermissao()
                ) {
                    val nova = estado.localizacao
                    if (nova != null && (propriaAndroid == null ||
                            nova.elapsedRealtimeNanos >= propriaAndroid!!.elapsedRealtimeNanos)
                    ) propriaAndroid = Location(nova)
                }
            }
        }
        registrar(usuarioRepository.observarPerfilPublicoNoRole(roleId, userId) { novoPerfil, erro ->
            if (!aceita(versao)) return@observarPerfilPublicoNoRole
            if (erro != null || novoPerfil == null) {
                falhar()
            } else {
                perfil = novoPerfil
                carregando = false
                if (!dadosProntosRegistrados) {
                    dadosProntosRegistrados = true
                    Log.d(
                        "DetalhesParcaVM",
                        "TELA09_DATA_READY geracao=$versao elapsedMs=${SystemClock.elapsedRealtime() - inicioMs}"
                    )
                }
            }
        }, versao)
        registrar(localizacaoRepository.observarLocalizacoesDoRole(roleId) { lista, erro ->
            if (!aceita(versao)) return@observarLocalizacoesDoRole
            if (erro != null) falhar() else localizacoes = lista.orEmpty()
        }, versao)
    }

    private fun aceita(versao: Int): Boolean =
        observando && geracao == versao && uidObservado == usuarioRepository.userIdAtual

    private fun registrar(listener: ListenerRegistration?, versao: Int) {
        if (listener == null) return
        if (aceita(versao)) listeners.add(listener) else listener.remove()
    }

    private fun acessoValido(): Boolean =
        observando && uidObservado == usuarioRepository.userIdAtual &&
            perfil != null && !carregando && mensagem.isBlank()

    private fun falhar() {
        pararObservacao()
        perfil = null
        localizacoes = emptyList()
        propriaAndroid = null
        carregando = false
        mensagem = "Não foi possível carregar os dados deste parça."
    }

    fun pararObservacao() {
        observando = false
        geracao++
        listeners.forEach { it.remove() }
        listeners.clear()
        ticker?.cancel()
        ticker = null
        observacaoAndroid?.cancel()
        observacaoAndroid = null
    }

    fun ligar(context: Context) {
        if (acessoValido()) mensagemAcao = acoesExternas.ligar(context, perfil!!.telefone).orEmpty()
    }

    fun whatsapp(context: Context) {
        if (acessoValido()) mensagemAcao = acoesExternas.whatsapp(context, perfil!!.telefone).orEmpty()
    }

    fun tracarRota(context: Context) {
        if (!acessoValido()) return
        val destino = localizacao.destino ?: return
        mensagemAcao = acoesExternas.tracarRota(context, destino.latitude, destino.longitude).orEmpty()
    }

    fun consumirMensagemAcao() {
        mensagemAcao = ""
    }

    override fun onCleared() {
        pararObservacao()
        super.onCleared()
    }
}
