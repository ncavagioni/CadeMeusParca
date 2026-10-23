package br.edu.eep.cademeusparca.util

import org.junit.Assert.*
import org.junit.Test

class ValidacaoPerfilUtilsTest {
    @Test fun nomeObrigatorioNaoAceitaVazioOuEspacos() {
        for (nome in listOf("", "   ")) {
            assertEquals("Informe o Parcaname.", ValidacaoPerfilUtils.erroCadastro(nome, ""))
        }
    }

    @Test fun nomeNoLimiteDoPerfilPublicoEhAceito() {
        assertNull(ValidacaoPerfilUtils.erroCadastro("P".repeat(100), ""))
    }

    @Test fun nomeAcimaDoLimiteEhRejeitadoAntesDaGravacao() {
        assertNotNull(ValidacaoPerfilUtils.erroCadastro("P".repeat(101), ""))
    }

    @Test fun telefoneOpcionalEVazioEhAceito() {
        assertNull(ValidacaoPerfilUtils.erroCadastro("Nicolas", ""))
    }

    @Test fun telefoneNoLimiteDoPerfilPublicoEhAceito() {
        assertNull(ValidacaoPerfilUtils.erroCadastro("Nicolas", "1".repeat(30)))
    }

    @Test fun telefoneAcimaDoLimiteEhRejeitado() {
        assertNotNull(ValidacaoPerfilUtils.erroCadastro("Nicolas", "1".repeat(31)))
    }

    @Test fun limitesConsideramValoresQueSeraoGravadosComTrim() {
        assertNull(ValidacaoPerfilUtils.erroCadastro("  " + "P".repeat(100) + "  ", " " + "1".repeat(30) + " "))
    }

    @Test fun naoMudaValidacaoDeNumeroDasAcoesExternas() {
        // Cadastro não passa a exigir telefone válido: Tela 09 mantém sua validação.
        assertNull(ValidacaoPerfilUtils.erroCadastro("Nicolas", "numero invalido"))
    }
}
