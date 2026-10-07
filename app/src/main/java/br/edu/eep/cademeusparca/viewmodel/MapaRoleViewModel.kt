package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.edu.eep.cademeusparca.location.DistanciaUtils
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.location.LocalizacaoTempoUtils
import br.edu.eep.cademeusparca.location.RelogioLocalizacao
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.repository.CompartilhamentoLocalizacaoRepository
import br.edu.eep.cademeusparca.repository.LocalizacaoRepository
import br.edu.eep.cademeusparca.repository.StatusCompartilhamento
import br.edu.eep.cademeusparca.service.LocationServiceController
import br.edu.eep.cademeusparca.repository.RoleRepository
import br.edu.eep.cademeusparca.repository.UsuarioRepository
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapaRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
    private val localizacaoRepository = LocalizacaoRepository()
    private val locationRepository = LocationRepository(application)
    private val usuarioRepository = UsuarioRepository()
    private var listenerLocalizacoes: ListenerRegistration? = null
    private var listenerPerfisPublicos: ListenerRegistration? = null
    private val rolesComPerfilPublicado = mutableSetOf<String>()
    private val rolesPublicandoPerfil = mutableSetOf<String>()
    private var roleObservado: String? = null
    private var geracaoObservacao = 0
    private var cancellationSource: CancellationTokenSource? = null
    private var tentativaLocalizacaoFeita = false
    private var mapaResumido = false
    private val handler = Handler(Looper.getMainLooper())
    private var recuperacao: Runnable? = null
    private var versaoManual = 0
    private var tickerTempo: Job? = null
    private var agoraMs by mutableStateOf(System.currentTimeMillis())
    private val compartilhamento = CompartilhamentoLocalizacaoRepository

    var mensagemCompartilhamento by mutableStateOf("Compartilhamento parado")
        private set

    fun iniciarCompartilhamento(activity: ComponentActivity, roleId: String) {
        LocationServiceController.iniciar(activity, roleId)
    }

    private fun atualizarEstadoCompartilhamento() {
        val estado = compartilhamento.estado.value
        mensagemCompartilhamento = if (estado.roleId != null && estado.roleId != roleObservado &&
            estado.status in setOf(StatusCompartilhamento.INICIANDO, StatusCompartilhamento.COMPARTILHANDO)
        ) {
            "Compartilhando em outro rolê"
        } else when (estado.status) {
            StatusCompartilhamento.PARADO -> "Compartilhamento parado"
            StatusCompartilhamento.INICIANDO -> "Iniciando compartilhamento..."
            StatusCompartilhamento.COMPARTILHANDO -> "Compartilhando localização"
            StatusCompartilhamento.ERRO -> estado.erro ?: "Erro no compartilhamento"
        }
        if (estado.roleId == roleObservado &&
            estado.userId == userIdAtual && userIdAtual == usuarioRepository.userIdAtual &&
            locationRepository.temPermissao()
        ) {
            estado.localizacao?.let(::atualizarPosicaoLocal)
        }
    }

    var nomeRole by mutableStateOf("")
        private set
    var mensagemRole by mutableStateOf("")
        private set
    var localizacao by mutableStateOf<Location?>(null)
        private set
    var carregandoLocalizacao by mutableStateOf(false)
        private set
    var mensagemLocalizacao by mutableStateOf("")
        private set

    var salvandoLocalizacao by mutableStateOf(false)
        private set
    var posicaoAtualizada by mutableStateOf(false)
        private set

    var localizacoesParticipantes by mutableStateOf<List<LocalizacaoParticipante>>(emptyList())
        private set
    var mensagemLeitura by mutableStateOf("")
        private set
    var carregandoParticipantes by mutableStateOf(false)
        private set
    var userIdAtual by mutableStateOf("")
        private set

    var parcanames by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var mensagemPerfis by mutableStateOf("")
        private set
    private var meuParcaname by mutableStateOf("")

    init {
        compartilhamento.configurar(application)
        viewModelScope.launch {
            compartilhamento.estado.collect { atualizarEstadoCompartilhamento() }
        }
    }

    fun tituloMarcador(userId: String): String {
        val nome = parcanames[userId]
            ?: meuParcaname.takeIf { userId == userIdAtual && it.isNotBlank() }
        return if (userId == userIdAtual) {
            if (nome == null) "Você" else "$nome (Você)"
        } else nome ?: "Parça (nome indisponível)"
    }

    fun localizacaoRecente(participante: LocalizacaoParticipante): Boolean =
        LocalizacaoTempoUtils.estaAtualizada(participante.atualizadoEm, agoraMs)

    fun vistoPorUltimo(participante: LocalizacaoParticipante): String? {
        if (participante.userId == userIdAtual || localizacaoRecente(participante)) return null
        return LocalizacaoTempoUtils.formatarVistoPorUltimo(participante.atualizadoEm, agoraMs)
            ?: "Horário indisponível"
    }

    fun distanciaFormatadaAte(participante: LocalizacaoParticipante): String? {
        if (userIdAtual.isBlank() || participante.userId == userIdAtual ||
            !localizacaoRecente(participante)
        ) return null

        val propriaAndroid = localizacao?.takeIf {
            DistanciaUtils.coordenadasValidas(it.latitude, it.longitude)
        }
        val propriaSalva = localizacoesParticipantes.firstOrNull {
            it.userId == userIdAtual &&
                DistanciaUtils.coordenadasValidas(it.latitude, it.longitude)
        }
        val latitudeOrigem = propriaAndroid?.latitude ?: propriaSalva?.latitude ?: return null
        val longitudeOrigem = propriaAndroid?.longitude ?: propriaSalva?.longitude ?: return null
        val metros = DistanciaUtils.calcularMetros(
            latitudeOrigem,
            longitudeOrigem,
            participante.latitude,
            participante.longitude
        ) ?: return null
        return DistanciaUtils.formatar(metros)
    }

    val localizacoesNoMapa: List<LocalizacaoParticipante>
        get() {
            val porUsuario = localizacoesParticipantes.associateBy { it.userId }.toMutableMap()
            // A posição Android tem prioridade apenas para o próprio usuário.
            // A câmera inicial usa a posição Android ou o próprio documento remoto como fallback.
            localizacao?.let { propria ->
                if (userIdAtual.isNotBlank()) {
                    val salva = porUsuario[userIdAtual]
                    val timestamp = salva?.takeIf {
                        it.latitude == propria.latitude && it.longitude == propria.longitude
                    }?.atualizadoEm
                    porUsuario[userIdAtual] = LocalizacaoParticipante(
                        userId = userIdAtual,
                        latitude = propria.latitude,
                        longitude = propria.longitude,
                        precisao = if (propria.hasAccuracy()) propria.accuracy.toDouble() else null,
                        atualizadoEm = timestamp
                    )
                }
            }
            return porUsuario.values.toList()
        }

    fun iniciarObservacao(roleId: String) {
        if (roleId.isBlank() || roleId.contains('/')) return
        if (roleObservado != roleId) {
            pararObservacao()
            roleObservado = roleId
            userIdAtual = usuarioRepository.userIdAtual.orEmpty()
        }
        mapaResumido = true
        iniciarTickerTempo()
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        atualizarEstadoCompartilhamento()
        val geracao = geracaoObservacao

        if (listenerPerfisPublicos == null) {
            listenerPerfisPublicos = usuarioRepository.observarPerfisPublicosDoRole(roleId) { perfis, erro ->
                if (geracao != geracaoObservacao) return@observarPerfisPublicosDoRole
                mensagemPerfis = erro.orEmpty()
                if (erro == null) {
                    parcanames = perfis.orEmpty().associate { it.userId to it.parcaname }
                } else {
                    listenerPerfisPublicos?.remove()
                    listenerPerfisPublicos = null
                    agendarRecuperacao()
                }
            }
        }
        if (roleId !in rolesComPerfilPublicado && rolesPublicandoPerfil.add(roleId)) {
            usuarioRepository.publicarMeuPerfilPublicoNoRole(roleId) { perfil, erro ->
                rolesPublicandoPerfil.remove(roleId)
                if (erro == null) rolesComPerfilPublicado.add(roleId)
                if (geracao != geracaoObservacao) return@publicarMeuPerfilPublicoNoRole
                meuParcaname = perfil?.parcaname.orEmpty()
                if (erro != null) mensagemPerfis = erro
            }
        }
        if (listenerLocalizacoes == null) {
            carregandoParticipantes = true
            listenerLocalizacoes = localizacaoRepository.observarLocalizacoesDoRole(roleId) { lista, erro ->
                if (geracao != geracaoObservacao) return@observarLocalizacoesDoRole
                mensagemLeitura = erro.orEmpty()
                carregandoParticipantes = false
                if (erro == null) {
                    localizacoesParticipantes = lista.orEmpty()
                    registrarEstadoMarcadores("snapshot")
                } else {
                    // Após erro terminal o Firestore não entrega novos eventos neste registro.
                    listenerLocalizacoes?.remove()
                    listenerLocalizacoes = null
                    agendarRecuperacao()
                }
            }
        }
    }

    private fun iniciarTickerTempo() {
        agoraMs = System.currentTimeMillis()
        if (tickerTempo?.isActive == true) return
        tickerTempo = RelogioLocalizacao.iniciar(viewModelScope) { agoraMs = it }
    }

    fun pausarLocalizacao() {
        mapaResumido = false
        tickerTempo?.cancel()
        tickerTempo = null
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        // Só cancela a busca pontual/feedback da tela. O serviço continua compartilhando.
        versaoManual++
        salvandoLocalizacao = false
        cancelarBuscaLocalizacao()
    }

    fun pararObservacao() {
        pausarLocalizacao()
        geracaoObservacao++
        listenerLocalizacoes?.remove()
        listenerLocalizacoes = null
        listenerPerfisPublicos?.remove()
        listenerPerfisPublicos = null
        parcanames = emptyMap()
        roleObservado = null
        localizacoesParticipantes = emptyList()
        carregandoParticipantes = false
    }

    private fun agendarRecuperacao() {
        if (!mapaResumido || recuperacao != null) return
        val roleId = roleObservado ?: return
        Log.d(
            TAG,
            "MAP_RECOVERY_SCHEDULED timestamp=${System.currentTimeMillis()} delayMs=$RECUPERACAO_MS"
        )
        val tarefa = Runnable {
            recuperacao = null
            if (mapaResumido && roleObservado == roleId) iniciarObservacao(roleId)
        }
        recuperacao = tarefa
        handler.postDelayed(tarefa, RECUPERACAO_MS)
    }

    private fun atualizarPosicaoLocal(nova: Location): Boolean {
        val anterior = localizacao
        if (anterior != null && nova.elapsedRealtimeNanos < anterior.elapsedRealtimeNanos) {
            Log.d(TAG, "LOCATION_OLDER_IGNORED timestamp=${System.currentTimeMillis()}")
            return false
        }
        localizacao = Location(nova)
        registrarEstadoMarcadores("android")
        return true
    }

    private fun registrarEstadoMarcadores(origem: String) {
        Log.d(
            TAG,
            "MARKERS_STATE_UPDATED timestamp=${System.currentTimeMillis()} " +
                "count=${localizacoesNoMapa.size} source=$origem"
        )
    }

    fun atualizarMinhaPosicao(roleId: String) {
        if (carregandoLocalizacao || salvandoLocalizacao) return
        posicaoAtualizada = false
        if (!locationRepository.temLocalizacaoPrecisa()) {
            mensagemLocalizacao = "Habilite a localização precisa nas configurações para compartilhar sua posição."
            return
        }
        buscarLocalizacao(roleId)
    }

    fun carregarRole(roleId: String) {
        roleRepository.buscarRolePorId(roleId) { role, erro ->
            nomeRole = role?.nome.orEmpty()
            mensagemRole = erro ?: ""
        }
    }

    fun verificarLocalizacaoAoAbrir() {
        if (!locationRepository.temPermissao()) {
            semPermissao()
        } else if (!tentativaLocalizacaoFeita) {
            obterLocalizacaoAtual()
        }
    }

    fun obterLocalizacaoAtual() {
        buscarLocalizacao()
    }

    private fun buscarLocalizacao(roleIdParaSalvar: String? = null) {
        if (carregandoLocalizacao || salvandoLocalizacao) return
        if (!locationRepository.temPermissao()) {
            semPermissao()
            return
        }

        tentativaLocalizacaoFeita = true
        carregandoLocalizacao = true
        mensagemLocalizacao = ""
        posicaoAtualizada = false
        val source = CancellationTokenSource()
        cancellationSource = source

        locationRepository.obterLocalizacaoAtual(source.token) { location, erro ->
            if (cancellationSource !== source) return@obterLocalizacaoAtual
            // getCurrentLocation pode terminar depois de um callback automático mais recente.
            val aceita = location?.let(::atualizarPosicaoLocal) ?: false
            mensagemLocalizacao = erro ?: ""
            carregandoLocalizacao = false
            cancellationSource = null
            if (roleIdParaSalvar != null && location != null) {
                // Reavaliar após a busca: a permissão pode ter mudado enquanto aguardávamos.
                if (!locationRepository.temLocalizacaoPrecisa()) {
                    mensagemLocalizacao = "Habilite a localização precisa nas configurações para compartilhar sua posição."
                    return@obterLocalizacaoAtual
                }
                val maisRecente = if (aceita) location else localizacao ?: return@obterLocalizacaoAtual
                if (!mapaResumido || roleObservado != roleIdParaSalvar) return@obterLocalizacaoAtual
                val versao = ++versaoManual
                salvandoLocalizacao = true
                compartilhamento.salvarManual(roleIdParaSalvar, maisRecente) { sucesso, mensagem ->
                    if (versao == versaoManual && mapaResumido && roleObservado == roleIdParaSalvar) {
                        salvandoLocalizacao = false
                        posicaoAtualizada = sucesso
                        mensagemLocalizacao = if (sucesso) "Posição atualizada."
                        else mensagem ?: "Não foi possível atualizar sua posição."
                    }
                }
            }
        }
    }

    fun semPermissao() {
        cancelarBuscaLocalizacao()
        posicaoAtualizada = false
        localizacao = null
        tentativaLocalizacaoFeita = false
        mensagemLocalizacao = "A localização é necessária para mostrar sua posição no mapa."
    }

    fun cancelarBuscaLocalizacao() {
        val source = cancellationSource
        cancellationSource = null
        source?.cancel()
        if (carregandoLocalizacao) {
            carregandoLocalizacao = false
            tentativaLocalizacaoFeita = false
        }
    }

    override fun onCleared() {
        pararObservacao()
        cancelarBuscaLocalizacao()
        super.onCleared()
    }

    private companion object {
        const val TAG = "MapaRoleViewModel"
        const val RECUPERACAO_MS = 5_000L
    }
}
