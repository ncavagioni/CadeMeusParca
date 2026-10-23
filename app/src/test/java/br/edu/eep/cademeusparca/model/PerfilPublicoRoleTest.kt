package br.edu.eep.cademeusparca.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfilPublicoRoleTest {
    private val dados: Map<String, Any?> = mapOf(
        "userId" to "parca", "parcaname" to "Parça Teste", "telefone" to "(19) 99999-0001"
    )

    @Test
    fun lePerfilComTelefone() {
        assertEquals(
            PerfilPublicoRole("parca", "Parça Teste", "(19) 99999-0001"),
            PerfilPublicoRole.deDocumento("parca", dados)
        )
    }

    @Test
    fun projecaoAntigaSemTelefoneContinuaValida() {
        assertEquals("", PerfilPublicoRole.deDocumento("parca", dados - "telefone")!!.telefone)
    }

    @Test
    fun telefoneVazioEhValido() {
        assertEquals("", PerfilPublicoRole.deDocumento("parca", dados + ("telefone" to ""))!!.telefone)
    }

    @Test
    fun rejeitaIdentidadeOuNomeInvalidos() {
        assertNull(PerfilPublicoRole.deDocumento("outro", dados))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("userId" to "")))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("parcaname" to " ")))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados - "parcaname"))
    }

    @Test
    fun telefoneComTipoInvalidoNaoDerrubaLeitura() {
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("telefone" to 123)))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("telefone" to null)))
    }

    @Test
    fun validaLimitesDeNomeETelefone() {
        assertNotNull(PerfilPublicoRole.deDocumento(
            "parca", dados + mapOf("parcaname" to "P".repeat(100), "telefone" to "1".repeat(30))
        ))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("parcaname" to "P".repeat(101))))
        assertNull(PerfilPublicoRole.deDocumento("parca", dados + ("telefone" to "1".repeat(31))))
    }

    @Test
    fun paraMapaDocumentoExportaApenasCamposPublicosPermitidos() {
        val perfil = PerfilPublicoRole("user123", "Parça Amigo", "(19) 98765-4321")
        val mapa = perfil.paraMapaDocumento()

        assertEquals(
            setOf("userId", "parcaname", "telefone"),
            mapa.keys
        )
        assertEquals("user123", mapa["userId"])
        assertEquals("Parça Amigo", mapa["parcaname"])
        assertEquals("(19) 98765-4321", mapa["telefone"])
        assertFalse(mapa.containsKey("contatoEmergencia"))
    }

    @Test
    fun paraMapaDocumentoNuncaPublicaContatoDeEmergenciaMesmoSePresenteNoPerfilPrivado() {
        val perfil = PerfilPublicoRole("user123", "Parça", "")
        val mapa = perfil.paraMapaDocumento()
        assertNull(mapa["contatoEmergencia"])
        assertTrue("contatoEmergencia" !in mapa)
    }
}
