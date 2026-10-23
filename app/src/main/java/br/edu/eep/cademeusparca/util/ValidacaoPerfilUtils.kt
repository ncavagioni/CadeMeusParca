package br.edu.eep.cademeusparca.util

import br.edu.eep.cademeusparca.model.PerfilPublicoRole

object ValidacaoPerfilUtils {
    fun erroCadastro(parcaname: String, telefone: String): String? {
        val nome = parcaname.trim()
        if (nome.isBlank()) return "Informe o Parcaname."
        // A mesma conversão da projeção garante que o cadastro pode ser publicado.
        val perfil = PerfilPublicoRole.deDocumento(
            "validacao",
            mapOf("userId" to "validacao", "parcaname" to nome, "telefone" to telefone.trim())
        )
        return if (perfil != null) null
        else "Use um Parcaname de até 100 caracteres e um telefone de até 30 caracteres."
    }
}
