package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetalhesRoleUiTest {
    private val role = Role(
        roleId = "role", nome = "Churrasco PIC IV", codigo = "FEVECG", adminId = "admin",
        nomeLocal = "Casa do Nicolas", endereco = "Rua de Teste, 123"
    )
    private val perfis = listOf(PerfilPublicoRole("admin", "Nicolas"))

    private fun dados(
        evento: Role = role,
        uid: String = "parca",
        nomes: List<PerfilPublicoRole> = perfis,
        quantidade: Int? = 2
    ) = comporDetalhesRole(evento, uid, nomes, quantidade)

    @Test
    fun statusAtivoRecebeApresentacaoCapitalizada() {
        assertEquals("Ativo", dados().status)
    }

    @Test
    fun statusDesconhecidoNaoCausaCrash() {
        assertEquals("Pausado", dados(role.copy(status = "pausado")).status)
        assertEquals("Outro status", formatarStatusRole("outro status"))
    }

    @Test
    fun umParticipanteUsaSingular() {
        assertEquals("1 parça", dados(quantidade = 1).participantes)
    }

    @Test
    fun doisParticipantesUsamPlural() {
        assertEquals("2 parças", dados().participantes)
    }

    @Test
    fun administradorResolvidoPeloPerfilPublicoCorrespondente() {
        val nomes = listOf(PerfilPublicoRole("outro", "Outra pessoa")) + perfis
        assertEquals("Nicolas", dados(nomes = nomes).administrador)
    }

    @Test
    fun administradorAtualRecebeIndicacaoVoce() {
        assertEquals("Nicolas (Você)", dados(uid = "admin").administrador)
    }

    @Test
    fun administradorSemPerfilOuComNomeVazioRecebeFallback() {
        for (nomes in listOf(emptyList(), listOf(PerfilPublicoRole("admin", "   ")))) {
            assertEquals("Nome indisponível", dados(uid = "admin", nomes = nomes).administrador)
        }
    }

    @Test
    fun localEEnderecoPresentesSaoExibidos() {
        assertEquals("Casa do Nicolas", dados().nomeLocal)
        assertEquals("Rua de Teste, 123", dados().endereco)
    }

    @Test
    fun apenasLocalPresenteNaoInventaEndereco() {
        val ui = dados(role.copy(endereco = ""))
        assertEquals("Casa do Nicolas", ui.nomeLocal)
        assertNull(ui.endereco)
    }

    @Test
    fun apenasEnderecoPresenteNaoInventaLocal() {
        val ui = dados(role.copy(nomeLocal = ""))
        assertNull(ui.nomeLocal)
        assertEquals("Rua de Teste, 123", ui.endereco)
    }

    @Test
    fun nenhumLocalOuEnderecoPresenteResultaEmAmbosAusentes() {
        val ui = dados(role.copy(nomeLocal = "  ", endereco = ""))
        assertNull(ui.nomeLocal)
        assertNull(ui.endereco)
    }

    @Test
    fun codigoEhPreservadoExatamenteParaExibicaoECopia() {
        assertEquals("FEVECG", dados().codigo)
        assertEquals("", dados(role.copy(codigo = "")).codigo)
    }

    @Test
    fun roleIdInvalidoOuAutenticacaoAusenteEhRejeitado() {
        for (id in listOf("", " ", ".", "..", "role/invalido", "/role")) {
            assertFalse(parametrosDetalhesRoleValidos(id, "eu"))
        }
        for (uid in listOf(null, "", " ")) {
            assertFalse(parametrosDetalhesRoleValidos("role", uid))
        }
    }

    @Test
    fun opcionaisNullEVaziosNaoQuebramComposicao() {
        val ui = dados(
            Role(roleId = "role", latitudeLocal = null, longitudeLocal = null, fotoUrl = ""),
            nomes = emptyList(), quantidade = null
        )
        assertEquals("Rolê", ui.nome)
        assertEquals("Ativo", ui.status)
        assertEquals("", ui.codigo)
        assertNull(ui.nomeLocal)
        assertNull(ui.endereco)
        assertEquals("Nome indisponível", ui.administrador)
        assertEquals("Carregando participantes...", ui.participantes)
    }

    @Test
    fun statusAusenteNaoInventaAtividade() {
        assertEquals("Não informado", formatarStatusRole(null))
        assertEquals("Não informado", formatarStatusRole(""))
        assertEquals("Não informado", dados(role.copy(status = "   ")).status)
    }

    @Test
    fun roleIdValidoComAutenticacaoEhAceito() {
        assertTrue(parametrosDetalhesRoleValidos("role-123", "eu"))
    }

    @Test
    fun dadosComplementaresPodemPreencherProgressivamente() {
        val inicial = dados(nomes = emptyList(), quantidade = null)
        assertEquals(role.nome, inicial.nome)
        assertEquals(role.codigo, inicial.codigo)
        assertEquals("Nome indisponível", inicial.administrador)
        val completa = dados()
        assertEquals("Nicolas", completa.administrador)
        assertEquals("2 parças", completa.participantes)
    }

    @Test
    fun contagemVaziaEIndisponivelNaoCausamCrash() {
        assertEquals("0 parças", formatarQuantidadeParcas(0))
        assertEquals("5 parças", formatarQuantidadeParcas(5))
        assertEquals("Quantidade indisponível", formatarQuantidadeParcas(-1))
    }
}
