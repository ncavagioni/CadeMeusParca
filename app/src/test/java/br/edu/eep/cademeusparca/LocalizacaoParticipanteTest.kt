package br.edu.eep.cademeusparca

import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalizacaoParticipanteTest {
    private val uid = "parca-1"
    private val timestamp = Timestamp(1_700_000_000L, 0)
    private val dados: Map<String, Any?> = mapOf(
        "userId" to uid,
        "latitude" to -22.7,
        "longitude" to -47.6,
        "precisao" to 5.0,
        "atualizadoEm" to timestamp
    )

    @Test
    fun leCoordenadasETimestampSemAlterarValores() {
        val resultado = LocalizacaoParticipante.deDocumento(uid, dados)
        assertNotNull(resultado)
        assertEquals(-22.7, resultado!!.latitude, 0.0)
        assertEquals(-47.6, resultado.longitude, 0.0)
        assertEquals(5.0, resultado.precisao!!, 0.0)
        assertEquals(timestamp, resultado.atualizadoEm)
    }

    @Test
    fun aceitaPrecisaoNulaETimestampPendente() {
        val resultado = LocalizacaoParticipante.deDocumento(
            uid, dados + mapOf("precisao" to null, "atualizadoEm" to null)
        )
        assertNotNull(resultado)
        assertNull(resultado!!.precisao)
        assertNull(resultado.atualizadoEm)
    }

    @Test
    fun naoInventaCoordenadasParaDocumentosIncompletos() {
        assertNull(LocalizacaoParticipante.deDocumento(uid, dados - "latitude"))
        assertNull(LocalizacaoParticipante.deDocumento(uid, dados - "longitude"))
        assertNull(LocalizacaoParticipante.deDocumento(uid, emptyMap()))
    }

    @Test
    fun rejeitaUidDiferenteDoDocumentoOuVazio() {
        assertNull(LocalizacaoParticipante.deDocumento("parca-2", dados))
        assertNull(LocalizacaoParticipante.deDocumento("", dados + ("userId" to "")))
    }

    @Test
    fun aceitaLimitesGeograficosENumerosInteiros() {
        assertNotNull(LocalizacaoParticipante.deDocumento(
            uid, dados + mapOf("latitude" to -90, "longitude" to 180, "precisao" to 0)
        ))
    }

    @Test
    fun rejeitaCoordenadasForaDosLimitesENaoFinitas() {
        for (latitude in listOf(-90.1, 90.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("latitude" to latitude)))
        }
        for (longitude in listOf(-180.1, 180.1, Double.NaN, Double.NEGATIVE_INFINITY)) {
            assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("longitude" to longitude)))
        }
    }

    @Test
    fun rejeitaPrecisaoNegativaNaoFinitaOuTexto() {
        for (precisao in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, "5")) {
            assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("precisao" to precisao)))
        }
    }

    @Test
    fun rejeitaTiposInvalidosSemDerrubarOListener() {
        assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("latitude" to "-22.7")))
        assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("longitude" to true)))
        assertNull(LocalizacaoParticipante.deDocumento(uid, dados + ("atualizadoEm" to "ontem")))
    }
}
