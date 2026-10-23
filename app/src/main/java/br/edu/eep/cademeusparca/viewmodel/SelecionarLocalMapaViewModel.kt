package br.edu.eep.cademeusparca.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.model.PontoLocal
import com.google.android.gms.tasks.CancellationTokenSource

/** Busca pontual apenas com permissão existente; não inicia compartilhamento nem serviço. */
class SelecionarLocalMapaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationRepository(application)
    private var busca: CancellationTokenSource? = null
    var posicaoAtual by mutableStateOf<PontoLocal?>(null)
        private set
    var buscando by mutableStateOf(false)
        private set

    fun buscarPosicaoInicial() {
        cancelarBusca()
        posicaoAtual = null
        if (!repository.temPermissao()) return
        val token = CancellationTokenSource()
        busca = token
        buscando = true
        repository.obterLocalizacaoAtual(token.token) { localizacao, _ ->
            if (busca !== token) return@obterLocalizacaoAtual
            buscando = false
            posicaoAtual = localizacao?.let {
                PontoLocal.deCoordenadas(it.latitude, it.longitude)
            }
        }
    }

    fun cancelarBusca() {
        busca?.cancel()
        busca = null
        buscando = false
    }

    override fun onCleared() {
        cancelarBusca()
    }
}
