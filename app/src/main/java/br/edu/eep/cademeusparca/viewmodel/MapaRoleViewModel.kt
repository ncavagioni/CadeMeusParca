package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.repository.LocalizacaoRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import com.google.android.gms.tasks.CancellationTokenSource

class MapaRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
    private val localizacaoRepository = LocalizacaoRepository()
    private val locationRepository = LocationRepository(application)
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
        cancelarBuscaLocalizacao()
        super.onCleared()
    }
}
