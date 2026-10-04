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

    var codigoEntrada by mutableStateOf("")
        private set
    var codigoInvalido by mutableStateOf(false)
        private set
    var buscandoRole by mutableStateOf(false)
        private set
    var mensagemBusca by mutableStateOf("")
        private set
    var roleEncontrado by mutableStateOf<Role?>(null)
        private set
    var entrandoNoRole by mutableStateOf(false)
        private set
    var mensagemEntrada by mutableStateOf("")
        private set
    var entradaConcluida by mutableStateOf(false)
        private set

    fun atualizarCodigoEntrada(valor: String) {
        if (buscandoRole) return

        if (valor.any { it !in 'A'..'Z' && it !in 'a'..'z' && it !in '0'..'9' }) {
            codigoInvalido = true
            mensagemBusca = "Use apenas letras de A a Z e números de 0 a 9."
            return
        }

        codigoEntrada = valor.uppercase(Locale.ROOT).take(6)
        codigoInvalido = false
        mensagemBusca = ""
        roleEncontrado = null
    }

    fun buscarRolePorCodigo() {
        if (buscandoRole || roleEncontrado != null) return

        if (codigoInvalido || !codigoEntrada.matches(Regex("[A-Z0-9]{6}"))) {
            codigoInvalido = true
            mensagemBusca = "Informe um código de 6 letras ou números."
            return
        }

        buscandoRole = true
        mensagemBusca = ""
        repository.buscarRolePorCodigo(codigoEntrada) { role, erro ->
            roleEncontrado = role
            mensagemBusca = erro ?: ""
            buscandoRole = false
        }
    }

    fun consumirRoleEncontrado() {
        roleEncontrado = null
    }

    fun entrarNoRole(roleId: String) {
        if (entrandoNoRole || entradaConcluida) return

        entrandoNoRole = true
        mensagemEntrada = ""
        repository.entrarNoRole(roleId) { sucesso, erro ->
            entradaConcluida = sucesso
            mensagemEntrada = erro ?: ""
            entrandoNoRole = false
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
