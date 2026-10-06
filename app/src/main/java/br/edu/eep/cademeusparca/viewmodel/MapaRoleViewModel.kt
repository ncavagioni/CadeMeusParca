package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.repository.LocalizacaoRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import br.edu.eep.cademeusparca.repository.UsuarioRepository
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.ListenerRegistration

class MapaRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
    private val localizacaoRepository = LocalizacaoRepository()
    private val locationRepository = LocationRepository(application)
    private val usuarioRepository = UsuarioRepository()
    private var listenerLocalizacoes: ListenerRegistration? = null
    private var listenerParcanames: ListenerRegistration? = null
    private val rolesComNomePublicado = mutableSetOf<String>()
    private val rolesPublicandoNome = mutableSetOf<String>()
    private var roleObservado: String? = null
    private var geracaoObservacao = 0
    private var cancellationSource: CancellationTokenSource? = null
    private var tentativaLocalizacaoFeita = false
    private var roleAtualizacaoAutomatica: String? = null
    private var geracaoAtualizacaoAutomatica = 0
    private var mapaResumido = false
    private val handler = Handler(Looper.getMainLooper())
    private var recuperacao: Runnable? = null
    private val filaGravacao = FilaUltimaPosicao<GravacaoLocalizacao>()
    private var versaoManual = 0

    private data class GravacaoLocalizacao(
        val roleId: String,
        val location: Location,
        val geracao: Int,
        val versaoManual: Int
    )

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

    fun tituloMarcador(userId: String): String {
        val nome = parcanames[userId]
            ?: meuParcaname.takeIf { userId == userIdAtual && it.isNotBlank() }
        return if (userId == userIdAtual) {
            if (nome == null) "Você" else "$nome (Você)"
        } else nome ?: "Parça (nome indisponível)"
    }

    val localizacoesNoMapa: List<LocalizacaoParticipante>
        get() {
            val porUsuario = localizacoesParticipantes.associateBy { it.userId }.toMutableMap()
            // A posição Android tem prioridade apenas para o próprio usuário.
            // A câmera continua observando somente localizacao, nunca esta lista.
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
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        iniciarAtualizacaoAutomatica(roleId)
        val geracao = geracaoObservacao

        if (listenerParcanames == null) {
            listenerParcanames = usuarioRepository.observarParcanamesDoRole(roleId) { nomes, erro ->
                if (geracao != geracaoObservacao) return@observarParcanamesDoRole
                mensagemPerfis = erro.orEmpty()
                if (erro == null) {
                    parcanames = nomes.orEmpty()
                } else {
                    listenerParcanames?.remove()
                    listenerParcanames = null
                    agendarRecuperacao()
                }
            }
        }
        if (roleId !in rolesComNomePublicado && rolesPublicandoNome.add(roleId)) {
            usuarioRepository.publicarMeuParcanameNoRole(roleId) { nome, erro ->
                rolesPublicandoNome.remove(roleId)
                if (erro == null) rolesComNomePublicado.add(roleId)
                if (geracao != geracaoObservacao) return@publicarMeuParcanameNoRole
                meuParcaname = nome.orEmpty()
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
        processarFilaGravacao()
    }

    fun pausarLocalizacao() {
        mapaResumido = false
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        pararAtualizacaoAutomatica()
        cancelarBuscaLocalizacao()
    }

    fun pararObservacao() {
        pausarLocalizacao()
        geracaoObservacao++
        listenerLocalizacoes?.remove()
        listenerLocalizacoes = null
        listenerParcanames?.remove()
        listenerParcanames = null
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

    private fun iniciarAtualizacaoAutomatica(roleId: String) {
        if (!mapaResumido) return
        if (!locationRepository.temLocalizacaoPrecisa()) {
            pararAtualizacaoAutomatica()
            return
        }
        if (roleAtualizacaoAutomatica == roleId) return
        pararAtualizacaoAutomatica()

        roleAtualizacaoAutomatica = roleId
        val geracao = geracaoAtualizacaoAutomatica
        Log.d(
            TAG,
            "AUTO_LOCATION_START timestamp=${System.currentTimeMillis()} " +
                "roleId=${roleId.take(8)} generation=$geracao"
        )
        val iniciou = locationRepository.iniciarAtualizacoes(
            onLocation = localizacaoRecebida@ { novaLocalizacao ->
                if (!mapaResumido || geracao != geracaoAtualizacaoAutomatica ||
                    roleAtualizacaoAutomatica != roleId
                ) return@localizacaoRecebida
                atualizarPosicaoLocal(novaLocalizacao)
                // Um resultado pontual pode ter obtido uma posição ainda mais recente.
                // O callback continua acionando a escrita, sempre com a posição mais nova.
                localizacao?.let { enfileirarLocalizacao(roleId, it) }
            },
            onError = erro@ { mensagem ->
                if (geracao != geracaoAtualizacaoAutomatica ||
                    roleAtualizacaoAutomatica != roleId
                ) return@erro
                roleAtualizacaoAutomatica = null
                mensagemLocalizacao = mensagem
                agendarRecuperacao()
            }
        )
        if (!iniciou) roleAtualizacaoAutomatica = null
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

    private fun enfileirarLocalizacao(roleId: String, location: Location) {
        if (!mapaResumido || roleObservado != roleId ||
            !locationRepository.temLocalizacaoPrecisa()
        ) return
        filaGravacao.oferecer(
            GravacaoLocalizacao(roleId, Location(location), geracaoAtualizacaoAutomatica, versaoManual)
        )
        Log.d(TAG, "LOCATION_WRITE_QUEUED timestamp=${System.currentTimeMillis()}")
        processarFilaGravacao()
    }

    private fun processarFilaGravacao() {
        if (!mapaResumido || !locationRepository.temLocalizacaoPrecisa()) return
        val pedido = filaGravacao.retirarParaGravar() ?: return
        if (pedido.roleId != roleObservado || pedido.geracao != geracaoAtualizacaoAutomatica) {
            filaGravacao.concluir()
            return
        }
        val location = pedido.location
        localizacaoRepository.salvarMinhaLocalizacao(
            roleId = pedido.roleId,
            latitude = location.latitude,
            longitude = location.longitude,
            precisao = if (location.hasAccuracy()) location.accuracy.toDouble() else null
        ) { sucesso, erro ->
            // A Task não é cancelável: liberar a fila mesmo se a sessão que escreveu já saiu.
            filaGravacao.concluir()
            val sessaoAtual = mapaResumido && pedido.roleId == roleObservado &&
                pedido.geracao == geracaoAtualizacaoAutomatica
            if (sessaoAtual) {
                posicaoAtualizada = sucesso
                if (salvandoLocalizacao && pedido.versaoManual == versaoManual) {
                    salvandoLocalizacao = false
                }
                mensagemLocalizacao = if (sucesso) "Posição atualizada."
                else erro ?: "Não foi possível atualizar sua posição."
                if (!sucesso) {
                    // Se chegou uma posição mais nova, ela tem prioridade sobre a que falhou.
                    filaGravacao.reporSeVazia(pedido)
                    agendarRecuperacao()
                    return@salvarMinhaLocalizacao
                }
            }
            // Pode haver uma posição da nova sessão esperando a Task anterior terminar.
            processarFilaGravacao()
        }
    }

    private fun pararAtualizacaoAutomatica() {
        geracaoAtualizacaoAutomatica++
        roleAtualizacaoAutomatica = null
        filaGravacao.descartarPendente()
        salvandoLocalizacao = false
        versaoManual++
        locationRepository.pararAtualizacoes()
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
                versaoManual++
                salvandoLocalizacao = true
                enfileirarLocalizacao(roleIdParaSalvar, maisRecente)
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
