package br.edu.eep.cademeusparca.navigation

/**
 * Agenda a navegação para depois do retorno do callback do Maps.
 * [agendar] deve enfileirar a ação, nunca executá-la imediatamente.
 */
internal class NavegacaoInfoWindow(
    private val agendar: (() -> Unit) -> Unit
) {
    private var pendente = false

    fun solicitar(podeNavegar: () -> Boolean, navegar: () -> Unit) {
        if (pendente) return
        pendente = true
        agendar {
            pendente = false
            if (podeNavegar()) navegar()
        }
    }
}
