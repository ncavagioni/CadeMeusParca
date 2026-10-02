package br.edu.eep.cademeusparca.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.edu.eep.cademeusparca.model.Usuario
import br.edu.eep.cademeusparca.repository.UsuarioRepository

class UsuarioViewModel : ViewModel() {

    private val repository = UsuarioRepository()

    var parcaname by mutableStateOf("")
        private set

    var telefone by mutableStateOf("")
        private set

    var contatoEmergencia by mutableStateOf("")
        private set

    var mensagem by mutableStateOf("")
        private set

    var carregando by mutableStateOf(false)
        private set

    fun atualizarParcaname(valor: String) {
        parcaname = valor
    }

    fun atualizarTelefone(valor: String) {
        telefone = valor
    }

    fun atualizarContatoEmergencia(valor: String) {
        contatoEmergencia = valor
    }

    fun salvarPerfil() {

        if (parcaname.isBlank()) {
            mensagem = "Informe o Parcaname."
            return
        }

        carregando = true
        mensagem = ""

        repository.salvarPerfil(
            parcaname = parcaname,
            telefone = telefone,
            contatoEmergencia = contatoEmergencia
        ) { sucesso, erro ->

            carregando = false

            mensagem = if (sucesso) {
                "Perfil salvo com sucesso!"
            } else {
                "Erro ao salvar: ${erro ?: "erro desconhecido"}"
            }
        }
    }

    fun buscarPerfil(
        onResult: (Usuario?) -> Unit
    ) {
        repository.buscarPerfil { usuario, erro ->

            if (erro != null) {
                mensagem = "Erro ao buscar perfil: $erro"
                onResult(null)
                return@buscarPerfil
            }

            if (usuario != null) {
                parcaname = usuario.parcaname
                telefone = usuario.telefone
                contatoEmergencia = usuario.contatoEmergencia
            }

            onResult(usuario)
        }
    }

    fun verificarFluxoInicial(
        onResultado: (Boolean) -> Unit
    ) {
        carregando = true
        mensagem = ""

        repository.garantirAutenticacao { autenticado, erroAutenticacao ->

            if (!autenticado) {
                carregando = false
                mensagem =
                    "Erro na autenticação: ${erroAutenticacao ?: "erro desconhecido"}"
                return@garantirAutenticacao
            }

            repository.buscarPerfil { usuario, erroPerfil ->

                carregando = false

                if (erroPerfil != null) {
                    mensagem = "Erro ao verificar perfil: $erroPerfil"
                    return@buscarPerfil
                }

                onResultado(usuario != null)
            }
        }
    }
}