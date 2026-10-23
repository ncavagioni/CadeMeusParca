package br.edu.eep.cademeusparca.model

import org.junit.Assert.*
import org.junit.Test

class EdicaoRoleTest {
    @Test
    fun administradorPodeEditarRoleAtivo() {
        assertTrue(podeEditarDadosRole("admin", "admin", "ativo"))
    }

    @Test
    fun participanteNaoPodeEditar() {
        assertFalse(podeEditarDadosRole("parca", "admin", "ativo"))
    }

    @Test
    fun administradorNaoPodeEditarRoleEncerrado() {
        assertFalse(podeEditarDadosRole("admin", "admin", "encerrado"))
    }

    @Test
    fun statusAusenteOuDesconhecidoNaoAutorizaEdicao() {
        listOf(null, "", "inativo", "Ativo", "pausado").forEach {
            assertFalse(podeEditarDadosRole("admin", "admin", it))
        }
    }

    @Test
    fun autenticacaoAusenteNaoAutorizaMesmoComAdminVazio() {
        listOf(null, "", " ").forEach {
            assertFalse(podeEditarDadosRole(it, it, "ativo"))
        }
    }

    @Test
    fun nomeVazioOuApenasEspacosEhRejeitado() {
        listOf("", " ", "\n\t").forEach {
            assertEquals("Informe o nome do rolê.", EdicaoRole(it).erroValidacao())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun payloadInvalidoNaoPodeSerGerado() {
        EdicaoRole(" ").paraAtualizacao()
    }

    @Test
    fun atualizacaoContemSomenteOsCincoCamposEditaveis() {
        val campos = EdicaoRole(" Novo ", " Praça ", " Rua A ", 1.0, 2.0).paraAtualizacao()
        assertEquals(setOf("nome", "nomeLocal", "endereco", "latitudeLocal", "longitudeLocal"), campos.keys)
        assertEquals("Novo", campos["nome"])
        assertEquals("Praça", campos["nomeLocal"])
        assertEquals("Rua A", campos["endereco"])
        assertEquals(1.0, campos["latitudeLocal"])
        assertEquals(2.0, campos["longitudeLocal"])
    }

    @Test
    fun atualizacaoParcialPreservaIdentidadeCodigoFotoStatusECamposDesconhecidos() {
        val antes = mapOf<String, Any?>(
            "roleId" to "role", "codigo" to "ABC123", "adminId" to "admin",
            "status" to "ativo", "fotoUrl" to "foto-existente", "outroCampo" to 42,
            "nome" to "Nome antigo", "latitudeLocal" to -22.0, "longitudeLocal" to -47.0
        )
        val depois = antes + EdicaoRole("Nome novo").paraAtualizacao()
        listOf("roleId", "codigo", "adminId", "status", "fotoUrl", "outroCampo").forEach {
            assertEquals(antes[it], depois[it])
        }
        assertEquals("Nome novo", depois["nome"])
    }

    @Test
    fun aceitaLimitesDasCoordenadasEZero() {
        listOf(-90.0 to -180.0, 90.0 to 180.0, 0.0 to 0.0, -22.725 to -47.649).forEach {
            assertNull(EdicaoRole("Rolê", latitudeLocal = it.first, longitudeLocal = it.second).erroValidacao())
        }
    }

    @Test
    fun rejeitaCoordenadasInvalidasIncompletasOuNaoFinitas() {
        listOf(
            -90.001 to 0.0, 90.001 to 0.0, 0.0 to -180.001, 0.0 to 180.001,
            Double.NaN to 0.0, 0.0 to Double.NaN,
            Double.POSITIVE_INFINITY to 0.0, 0.0 to Double.NEGATIVE_INFINITY,
            null to 0.0, 0.0 to null
        ).forEach {
            assertNotNull(EdicaoRole("Rolê", latitudeLocal = it.first, longitudeLocal = it.second).erroValidacao())
        }
    }

    @Test
    fun limparPontoGravaAmbosOsCamposExplicitamenteComoNulos() {
        val edicao = EdicaoRole("Rolê")
        assertNull(edicao.erroValidacao())
        val campos = edicao.paraAtualizacao()
        assertTrue(campos.containsKey("latitudeLocal"))
        assertTrue(campos.containsKey("longitudeLocal"))
        assertNull(campos["latitudeLocal"])
        assertNull(campos["longitudeLocal"])
    }

    @Test
    fun marcadorRefletePontoENomeAtualizadosESomeAoLimpar() {
        val original = Role(
            roleId = "role", nome = "Antes", nomeLocal = "Praça antiga",
            latitudeLocal = -22.0, longitudeLocal = -47.0, adminId = "admin", codigo = "ABC123"
        )
        val edicao = EdicaoRole("Depois", "Praça nova", "Endereço novo", -23.0, -46.0)
        val campos = edicao.paraAtualizacao()
        val atualizado = original.copy(
            nome = campos["nome"] as String,
            nomeLocal = campos["nomeLocal"] as String,
            endereco = campos["endereco"] as String,
            latitudeLocal = campos["latitudeLocal"] as Double?,
            longitudeLocal = campos["longitudeLocal"] as Double?
        )
        assertEquals(MarcadorEvento(PontoLocal(-23.0, -46.0), "Praça nova"), atualizado.marcadorEvento())
        assertNotEquals(original.marcadorEvento(), atualizado.marcadorEvento())
        assertEquals(original.codigo, atualizado.codigo)
        assertEquals(original.adminId, atualizado.adminId)
        assertNull(atualizado.copy(latitudeLocal = null, longitudeLocal = null).marcadorEvento())
    }
}
