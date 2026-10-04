package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.repository.RoleRepository
import com.google.android.gms.tasks.CancellationTokenSource

class MapaRoleViewModel(application: Application) : AndroidViewModel(application) {
    private val roleRepository = RoleRepository()
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
        if (carregandoLocalizacao) return
        if (!locationRepository.temPermissao()) {
            semPermissao()
            return
        }

        tentativaLocalizacaoFeita = true
        carregandoLocalizacao = true
        localizacao = null
        mensagemLocalizacao = ""
        val source = CancellationTokenSource()
        cancellationSource = source

        locationRepository.obterLocalizacaoAtual(source.token) { location, erro ->
            if (cancellationSource !== source) return@obterLocalizacaoAtual
            localizacao = location
            mensagemLocalizacao = erro ?: ""
            carregandoLocalizacao = false
            cancellationSource = null
        }
    }

    fun semPermissao() {
        cancelarBuscaLocalizacao()
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
