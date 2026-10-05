package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.location.Location
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
        if (roleObservado == roleId &&
            (listenerLocalizacoes != null || listenerParcanames != null)
        ) return
        pararObservacao()
        roleObservado = roleId
        val geracao = geracaoObservacao
        userIdAtual = usuarioRepository.userIdAtual.orEmpty()
        carregandoParticipantes = true
        mensagemLeitura = ""
        mensagemPerfis = ""
        listenerParcanames = usuarioRepository.observarParcanamesDoRole(roleId) { nomes, erro ->
            if (geracao != geracaoObservacao) return@observarParcanamesDoRole
            parcanames = nomes.orEmpty()
            mensagemPerfis = erro.orEmpty()
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
        listenerLocalizacoes = localizacaoRepository.observarLocalizacoesDoRole(roleId) { lista, erro ->
            if (geracao != geracaoObservacao) return@observarLocalizacoesDoRole
            localizacoesParticipantes = lista.orEmpty()
            mensagemLeitura = erro.orEmpty()
            carregandoParticipantes = false
        }
    }

    fun pararObservacao() {
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
        localizacao = null
        mensagemLocalizacao = ""
        posicaoAtualizada = false
        val source = CancellationTokenSource()
        cancellationSource = source

        locationRepository.obterLocalizacaoAtual(source.token) { location, erro ->
            if (cancellationSource !== source) return@obterLocalizacaoAtual
            localizacao = location
            mensagemLocalizacao = erro ?: ""
            carregandoLocalizacao = false
            cancellationSource = null
            if (roleIdParaSalvar != null && location != null) {
                // Reavaliar após a busca: a permissão pode ter mudado enquanto aguardávamos.
                if (!locationRepository.temLocalizacaoPrecisa()) {
                    mensagemLocalizacao = "Habilite a localização precisa nas configurações para compartilhar sua posição."
                    return@obterLocalizacaoAtual
                }
                salvandoLocalizacao = true
                localizacaoRepository.salvarMinhaLocalizacao(
                    roleId = roleIdParaSalvar,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    precisao = if (location.hasAccuracy()) location.accuracy.toDouble() else null
                ) { sucesso, erroGravacao ->
                    salvandoLocalizacao = false
                    posicaoAtualizada = sucesso
                    mensagemLocalizacao = if (sucesso) "Posição atualizada."
                    else erroGravacao ?: "Não foi possível atualizar sua posição."
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
}
