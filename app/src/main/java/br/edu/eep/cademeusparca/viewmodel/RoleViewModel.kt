package br.edu.eep.cademeusparca.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.repository.RoleRepository
import java.util.Locale

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

    var roles by mutableStateOf<List<Role>>(emptyList())
        private set
    var carregandoListagem by mutableStateOf(false)
        private set
    var mensagemListagem by mutableStateOf("")
        private set

    private var buscaAtual = 0

    fun buscarRolesDoUsuario() {
        val busca = ++buscaAtual
        carregandoListagem = true
        mensagemListagem = ""

        repository.buscarRolesDoUsuario { resultado, erro ->
            // Uma resposta antiga não pode sobrescrever a consulta feita ao retornar à tela.
            if (busca != buscaAtual) return@buscarRolesDoUsuario

            roles = resultado.orEmpty().sortedWith(
                compareBy<Role> { it.nome.lowercase(Locale.ROOT) }
                    .thenBy { it.nome }
                    .thenBy { it.roleId }
            )
            mensagemListagem = erro ?: ""
            carregandoListagem = false
        }
    }

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
