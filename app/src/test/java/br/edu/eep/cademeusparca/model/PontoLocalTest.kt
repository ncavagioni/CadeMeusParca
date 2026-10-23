package br.edu.eep.cademeusparca.model

import org.junit.Assert.*
import org.junit.Test

class PontoLocalTest {
    @Test
    fun aceitaCoordenadasValidasIncluindoLimitesEZero() {
        listOf(-90.0 to -180.0, 90.0 to 180.0, 0.0 to 0.0, -22.725 to -47.649).forEach {
            assertEquals(PontoLocal(it.first, it.second), PontoLocal.deCoordenadas(it.first, it.second))
            assertTrue(PontoLocal.coordenadasOpcionaisValidas(it.first, it.second))
        }
    }

    @Test
    fun rejeitaLatitudeForaDosLimitesOuNaoFinita() {
        listOf(-90.001, 90.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach {
                assertNull(PontoLocal.deCoordenadas(it, 0.0))
                assertFalse(PontoLocal.coordenadasOpcionaisValidas(it, 0.0))
            }
    }

    @Test
    fun rejeitaLongitudeForaDosLimitesOuNaoFinita() {
        listOf(-180.001, 180.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach {
                assertNull(PontoLocal.deCoordenadas(0.0, it))
                assertFalse(PontoLocal.coordenadasOpcionaisValidas(0.0, it))
            }
    }

    @Test(expected = IllegalArgumentException::class)
    fun construtorTambemImpedeLatitudeInvalida() {
        PontoLocal(91.0, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun construtorTambemImpedeLongitudeInvalida() {
        PontoLocal(0.0, Double.NaN)
    }

    @Test
    fun coordenadasSaoOpcionaisMasNaoAceitamParIncompleto() {
        assertTrue(PontoLocal.coordenadasOpcionaisValidas(null, null))
        assertFalse(PontoLocal.coordenadasOpcionaisValidas(null, 0.0))
        assertFalse(PontoLocal.coordenadasOpcionaisValidas(0.0, null))
    }

    @Test
    fun roleAntigoNaoGeraMarcadorDeEvento() {
        val role = Role(roleId = "antigo", nome = "Rolê sem ponto")
        assertNull(role.latitudeLocal)
        assertNull(role.longitudeLocal)
        assertNull(role.marcadorEvento())
    }

    @Test
    fun marcadorSoExisteComParValido() {
        assertNotNull(Role(latitudeLocal = 0.0, longitudeLocal = 0.0).marcadorEvento())
        listOf(
            Role(latitudeLocal = 0.0),
            Role(longitudeLocal = 0.0),
            Role(latitudeLocal = 91.0, longitudeLocal = 0.0),
            Role(latitudeLocal = 0.0, longitudeLocal = -181.0),
            Role(latitudeLocal = Double.NaN, longitudeLocal = 0.0),
            Role(latitudeLocal = 0.0, longitudeLocal = Double.POSITIVE_INFINITY)
        ).forEach { assertNull(it.marcadorEvento()) }
    }

    @Test
    fun marcadorIdentificaLocalEConservaCoordenadasDoDocumento() {
        val role = Role(
            nome = "Encontro", nomeLocal = "Praça",
            latitudeLocal = -22.725, longitudeLocal = -47.649
        )
        assertEquals(MarcadorEvento(PontoLocal(-22.725, -47.649), "Praça"), role.marcadorEvento())
    }

    @Test
    fun marcadorUsaNomeDoRoleQuandoLocalNaoFoiInformado() {
        assertEquals(
            "Encontro",
            Role(nome = "Encontro", nomeLocal = "  ", latitudeLocal = 1.0, longitudeLocal = 2.0)
                .marcadorEvento()?.titulo
        )
    }
}
