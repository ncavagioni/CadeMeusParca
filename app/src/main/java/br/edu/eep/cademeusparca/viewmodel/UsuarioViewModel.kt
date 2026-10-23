package br.edu.eep.cademeusparca.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.eep.cademeusparca.util.PrazoCarregamento
import br.edu.eep.cademeusparca.util.ValidacaoPerfilUtils
import br.edu.eep.cademeusparca.model.Usuario
import br.edu.eep.cademeusparca.repository.UsuarioRepository

class UsuarioViewModel(
    private val repository: UsuarioRepository = UsuarioRepository()
) : ViewModel() {

    private val prazoInicial = PrazoCarregamento.criar(viewModelScope)
    private var geracaoInicial = 0

    var possuiPerfilInicial by mutableStateOf<Boolean?>(null)
        private set

    var parcaname by mutableStateOf("")
        private set

    var telefone by mutableStateOf("")
        private set

    var contatoEmergencia by mutableStateOf("")
        private set

    var mensagem by mutableStateOf("")
        private set

    var mensagemSucesso by mutableStateOf("")
        private set

    var carregando by mutableStateOf(false)
        private set

    var carregandoPerfil by mutableStateOf(false)
        private set

    fun atualizarParcaname(valor: String) {
        parcaname = valor
        mensagem = ""
        mensagemSucesso = ""
    }

    fun atualizarTelefone(valor: String) {
        telefone = valor
        mensagem = ""
        mensagemSucesso = ""
    }

    fun atualizarContatoEmergencia(valor: String) {
        contatoEmergencia = valor
        mensagem = ""
        mensagemSucesso = ""
    }

    fun limparMensagens() {
        mensagem = ""
        mensagemSucesso = ""
    }

    fun carregarPerfilParaEdicao(onResult: (Usuario?) -> Unit = {}) {
        carregandoPerfil = true
        mensagem = ""
        mensagemSucesso = ""

        repository.buscarPerfil { usuario, erro ->
            carregandoPerfil = false
            if (erro != null) {
                mensagem = "Não foi possível carregar seu perfil: $erro"
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

    fun salvarEdicaoPerfil(onResult: (Boolean) -> Unit = {}) {
        if (carregando || carregandoPerfil) return

        val erroValidacao = ValidacaoPerfilUtils.erroCadastro(parcaname, telefone)
        if (erroValidacao != null) {
            mensagem = erroValidacao
            mensagemSucesso = ""
            onResult(false)
            return
        }

        carregando = true
        mensagem = ""
        mensagemSucesso = ""

        repository.atualizarPerfilEPropagar(
            parcaname = parcaname,
            telefone = telefone,
            contatoEmergencia = contatoEmergencia
        ) { sucesso, erro ->
            carregando = false
            if (sucesso) {
                mensagem = ""
                mensagemSucesso = "Perfil atualizado com sucesso!"
            } else {
                mensagem = erro ?: "Não foi possível atualizar o perfil."
                mensagemSucesso = ""
            }
            onResult(sucesso)
        }
    }

    fun salvarPerfil(onResult: (Boolean) -> Unit = {}) {
        if (carregando) return

        val erroValidacao = ValidacaoPerfilUtils.erroCadastro(parcaname, telefone)
        if (erroValidacao != null) {
            mensagem = erroValidacao
            mensagemSucesso = ""
            onResult(false)
            return
        }

        carregando = true
        mensagem = ""
        mensagemSucesso = ""

        repository.salvarPerfil(
            parcaname = parcaname,
            telefone = telefone,
            contatoEmergencia = contatoEmergencia
        ) { sucesso, erro ->
            carregando = false
            mensagem = if (sucesso) {
                ""
            } else {
                "Erro ao salvar: ${erro ?: "erro desconhecido"}"
            }
            onResult(sucesso)
        }
    }

    fun buscarPerfil(
        onResult: (Usuario?) -> Unit = {}
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

    fun verificarFluxoInicial() {
        if (carregando || possuiPerfilInicial != null) return
        val versao = ++geracaoInicial
        carregando = true
        mensagem = ""
        prazoInicial.iniciar {
            if (versao == geracaoInicial) {
                falharInicializacao("O início está demorando. Verifique sua internet e tente novamente.")
            }
        }

        repository.garantirAutenticacao { autenticado, _ ->
            if (versao != geracaoInicial) return@garantirAutenticacao
            if (!autenticado) {
                falharInicializacao("Não foi possível iniciar sua sessão. Verifique sua internet e tente novamente.")
                return@garantirAutenticacao
            }
            repository.buscarPerfil { usuario, erroPerfil ->
                if (versao != geracaoInicial) return@buscarPerfil
                if (erroPerfil != null) {
                    falharInicializacao("Não foi possível verificar seu perfil. Verifique sua internet e tente novamente.")
                } else {
                    prazoInicial.cancelar()
                    geracaoInicial++
                    carregando = false
                    possuiPerfilInicial = usuario != null
                }
            }
        }
    }

    private fun falharInicializacao(erro: String) {
        prazoInicial.cancelar()
        geracaoInicial++
        carregando = false
        mensagem = erro
    }

    override fun onCleared() {
        prazoInicial.cancelar()
        geracaoInicial++
        super.onCleared()
    }
}