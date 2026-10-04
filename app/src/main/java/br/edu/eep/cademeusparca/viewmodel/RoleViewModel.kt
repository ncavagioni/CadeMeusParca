package br.edu.eep.cademeusparca.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.repository.RoleRepository

class RoleViewModel : ViewModel() {

    private val repository = RoleRepository()

    var nome by mutableStateOf("")
        private set
    var nomeLocal by mutableStateOf("")
        private set
    var endereco by mutableStateOf("")
        private set
    var mensagem by mutableStateOf("")
        private set
    var nomeInvalido by mutableStateOf(false)
        private set
    var carregando by mutableStateOf(false)
        private set
    var roleCriado by mutableStateOf<Role?>(null)
        private set

    fun atualizarNome(valor: String) {
        if (carregando) return
        nome = valor
        nomeInvalido = false
        mensagem = ""
    }

    fun atualizarNomeLocal(valor: String) {
        if (!carregando) nomeLocal = valor
    }

    fun atualizarEndereco(valor: String) {
        if (!carregando) endereco = valor
    }

    fun criarRole() {
        if (carregando || roleCriado != null) return

        if (nome.isBlank()) {
            nomeInvalido = true
            mensagem = "Informe o nome do rolê."
            return
        }

        carregando = true
        nomeInvalido = false
        mensagem = ""

        repository.criarRole(nome, nomeLocal, endereco) { role, erro ->
            roleCriado = role
            mensagem = erro ?: ""
            carregando = false
        }
    }
}
