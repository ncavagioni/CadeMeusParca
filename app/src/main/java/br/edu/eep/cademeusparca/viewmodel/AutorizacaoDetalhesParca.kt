package br.edu.eep.cademeusparca.viewmodel

/** Recebe apenas confirmações do servidor, filtradas pelo repository. */
internal class AutorizacaoDetalhesParca(
    private val aoAutorizar: () -> Unit,
    private val aoNegar: () -> Unit
) {
    private var usuarioConfirmado = false
    private var parcaConfirmado = false
    private var dadosIniciados = false
    private var negado = false

    fun atualizarUsuarioAtual(pertence: Boolean) = atualizar(pertence, usuarioAtual = true)

    fun atualizarParca(pertence: Boolean) = atualizar(pertence, usuarioAtual = false)

    private fun atualizar(pertence: Boolean, usuarioAtual: Boolean) {
        if (negado) return
        if (!pertence) {
            negado = true
            aoNegar()
            return
        }
        if (usuarioAtual) usuarioConfirmado = true else parcaConfirmado = true
        if (usuarioConfirmado && parcaConfirmado && !dadosIniciados) {
            dadosIniciados = true
            aoAutorizar()
        }
    }
}
