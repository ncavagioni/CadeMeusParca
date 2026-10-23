package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.model.Usuario
import br.edu.eep.cademeusparca.util.ValidacaoPerfilUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsuarioEdicaoPerfilTest {

    @Test
    fun validacaoEdicaoExigeParcanameNaoVazio() {
        val erroVazio = ValidacaoPerfilUtils.erroCadastro("", "(19) 99999-0000")
        assertEquals("Informe o Parcaname.", erroVazio)

        val erroEspacos = ValidacaoPerfilUtils.erroCadastro("   ", "(19) 99999-0000")
        assertEquals("Informe o Parcaname.", erroEspacos)
    }

    @Test
    fun validacaoEdicaoAceitaParcanameValido() {
        val erro = ValidacaoPerfilUtils.erroCadastro("Novo Nome do Parça", "(19) 99999-0000")
        assertNull(erro)
    }

    @Test
    fun validacaoEdicaoRejeitaParcanameAcimaDe100Caracteres() {
        val nomeLongo = "A".repeat(101)
        val erro = ValidacaoPerfilUtils.erroCadastro(nomeLongo, "")
        assertNotNull(erro)
    }

    @Test
    fun validacaoEdicaoAceitaTelefoneOpcionalVazio() {
        val erro = ValidacaoPerfilUtils.erroCadastro("Parça", "")
        assertNull(erro)
    }

    @Test
    fun validacaoEdicaoRejeitaTelefoneAcimaDe30Caracteres() {
        val telefoneLongo = "1".repeat(31)
        val erro = ValidacaoPerfilUtils.erroCadastro("Parça", telefoneLongo)
        assertNotNull(erro)
    }

    @Test
    fun perfilPublicoNuncaContemContatoDeEmergencia() {
        val usuario = Usuario(
            userId = "uid_teste",
            parcaname = "Parça Seguro",
            telefone = "(19) 98888-7777",
            contatoEmergencia = "(19) 91111-2222"
        )

        val perfilPublico = PerfilPublicoRole(
            userId = usuario.userId,
            parcaname = usuario.parcaname,
            telefone = usuario.telefone
        )

        val dados = perfilPublico.paraMapaDocumento()

        assertEquals("uid_teste", dados["userId"])
        assertEquals("Parça Seguro", dados["parcaname"])
        assertEquals("(19) 98888-7777", dados["telefone"])
        assertFalse("O contato de emergência jamais pode ser exportado para o rolê público", dados.containsKey("contatoEmergencia"))
        assertEquals(3, dados.size)
        assertTrue(dados.keys.containsAll(listOf("userId", "parcaname", "telefone")))
    }

    @Test
    fun camposPrivadosCompletosSaoPreservadosNoModeloUsuario() {
        val usuario = Usuario(
            userId = "uid1",
            parcaname = "Meu Parça",
            telefone = "(19) 99999-1234",
            contatoEmergencia = "(19) 98888-5678"
        )
        assertEquals("uid1", usuario.userId)
        assertEquals("Meu Parça", usuario.parcaname)
        assertEquals("(19) 99999-1234", usuario.telefone)
        assertEquals("(19) 98888-5678", usuario.contatoEmergencia)
    }
}
