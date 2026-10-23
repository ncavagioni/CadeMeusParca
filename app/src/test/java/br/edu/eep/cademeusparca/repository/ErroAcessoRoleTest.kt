package br.edu.eep.cademeusparca.repository

import org.junit.Assert.*
import org.junit.Test

class ErroAcessoRoleTest {
    @Test fun permissaoNegadaImpedeAcesso() {
        assertTrue(erroImpedeAcessoAoRole("PERMISSION_DENIED"))
    }

    @Test fun autenticacaoInvalidaImpedeAcesso() {
        assertTrue(erroImpedeAcessoAoRole("UNAUTHENTICATED"))
    }

    @Test fun outrosErrosNaoSaoTratadosComoRevogacao() {
        listOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "FAILED_PRECONDITION",
            "UNKNOWN", "INVALID_ARGUMENT", "")
            .forEach { assertFalse(it, erroImpedeAcessoAoRole(it)) }
    }
}
