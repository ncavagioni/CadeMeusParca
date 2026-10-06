package br.edu.eep.cademeusparca.repository

import org.junit.Assert.*
import org.junit.Test

class SessaoCompartilhamentoTest {
    @Test fun mesmaEntradaNaoCriaOutraSessao() {
        val sessao = SessaoCompartilhamento()
        val primeira = sessao.selecionar("roleA", "usuarioA")
        assertSame(primeira, sessao.selecionar("roleA", "usuarioA"))
        assertTrue(sessao.aceita(primeira))
    }

    @Test fun trocaDeRoleInvalidaPosicoesAnteriores() {
        val sessao = SessaoCompartilhamento()
        val anterior = sessao.selecionar("roleA", "usuarioA")
        val atual = sessao.selecionar("roleB", "usuarioA")
        assertFalse(sessao.aceita(anterior))
        assertTrue(sessao.aceita(atual))
        assertEquals("roleB", atual.roleId)
    }

    @Test fun trocaDeUsuarioInvalidaPosicoesAnteriores() {
        val sessao = SessaoCompartilhamento()
        val anterior = sessao.selecionar("roleA", "usuarioA")
        val atual = sessao.selecionar("roleA", "usuarioB")
        assertFalse(sessao.aceita(anterior))
        assertTrue(sessao.aceita(atual))
    }

    @Test fun pararInvalidaSessaoMesmoAoReabrirMesmoRole() {
        val sessao = SessaoCompartilhamento()
        val anterior = sessao.selecionar("roleA", "usuarioA")
        sessao.encerrar()
        assertFalse(sessao.aceita(anterior))
        assertNull(sessao.destino)
        val atual = sessao.selecionar("roleA", "usuarioA")
        assertNotEquals(anterior.geracao, atual.geracao)
        assertFalse(sessao.aceita(anterior))
        assertTrue(sessao.aceita(atual))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaRoleVazio() {
        SessaoCompartilhamento().selecionar("", "usuarioA")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaRoleComSeparadorDeCaminho() {
        SessaoCompartilhamento().selecionar("roles/outro", "usuarioA")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaUsuarioVazio() {
        SessaoCompartilhamento().selecionar("roleA", "")
    }
}
