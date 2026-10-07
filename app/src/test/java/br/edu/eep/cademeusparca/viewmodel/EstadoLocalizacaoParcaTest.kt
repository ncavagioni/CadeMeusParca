package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoLocalizacaoParcaTest {
    private val agora = 1_700_000_000_000L
    private val parca = LocalizacaoParticipante(
        "parca", -22.701, -47.6, atualizadoEm = Timestamp(1_699_999_990L, 0)
    )
    private val android = LocalizacaoParticipante("eu", -22.7, -47.6)
    private val salva = LocalizacaoParticipante("eu", parca.latitude, parca.longitude)

    @Test
    fun recenteUsaAndroidAntesDoDocumentoProprio() {
        val estado = estadoLocalizacaoParca(parca, android, salva, agora)
        assertTrue(estado.recente)
        assertEquals("111 m", estado.distancia)
        assertEquals("111 m de você", estado.textoPrincipal)
        assertEquals(parca, estado.destino)
    }

    @Test
    fun usaDocumentoProprioQuandoAndroidAusenteOuInvalido() {
        for (origem in listOf(null, android.copy(latitude = Double.NaN))) {
            assertEquals("0 m de você", estadoLocalizacaoParca(parca, origem, salva, agora).textoPrincipal)
        }
    }

    @Test
    fun semOrigemNaoInventaDistancia() {
        val estado = estadoLocalizacaoParca(parca, null, null, agora)
        assertTrue(estado.recente)
        assertNull(estado.distancia)
        assertEquals("Localização recente", estado.textoPrincipal)
        assertEquals(parca, estado.destino)
    }

    @Test
    fun antigaOmiteDistanciaEPreservaDestino() {
        val estado = estadoLocalizacaoParca(parca, android, salva, agora + 290_000L)
        assertFalse(estado.recente)
        assertNull(estado.distancia)
        assertEquals("Visto há 5 min", estado.textoPrincipal)
        assertEquals(parca, estado.destino)
    }

    @Test
    fun semTimestampPreservaCoordenadaSemInventarHorario() {
        val semHorario = parca.copy(atualizadoEm = null)
        val estado = estadoLocalizacaoParca(semHorario, android, salva, agora)
        assertFalse(estado.recente)
        assertNull(estado.distancia)
        assertEquals("Horário indisponível", estado.textoPrincipal)
        assertEquals(semHorario, estado.destino)
    }

    @Test
    fun semDocumentoOuCoordenadaValidaNaoPermiteRota() {
        for (destino in listOf(null, parca.copy(latitude = Double.NaN), parca.copy(longitude = 181.0))) {
            val estado = estadoLocalizacaoParca(destino, android, salva, agora)
            assertEquals("Localização indisponível", estado.textoPrincipal)
            assertNull(estado.distancia)
            assertNull(estado.destino)
        }
    }

    @Test
    fun timestampFuturoContinuaRecente() {
        assertTrue(estadoLocalizacaoParca(
            parca.copy(atualizadoEm = Timestamp(1_700_000_005L, 0)), android, salva, agora
        ).recente)
    }

    @Test
    fun tempoMudaEstadoSemNovoSnapshotENovaPosicaoRestauraRecencia() {
        assertTrue(estadoLocalizacaoParca(parca, android, salva, agora + 50_000L).recente)
        val antiga = estadoLocalizacaoParca(parca, android, salva, agora + 50_001L)
        assertFalse(antiga.recente)
        assertNull(antiga.distancia)
        assertEquals("Visto há 1 min", antiga.textoPrincipal)
        assertEquals("Visto há 2 min",
            estadoLocalizacaoParca(parca, android, salva, agora + 110_000L).textoPrincipal)
        val nova = parca.copy(atualizadoEm = Timestamp(1_700_000_110L, 0))
        assertEquals("111 m de você",
            estadoLocalizacaoParca(nova, android, salva, agora + 110_000L).textoPrincipal)
    }

    @Test
    fun aceitaIdentificadoresDeOutroParcaAutenticado() {
        assertTrue(parametrosDetalhesValidos("role", "parca", "eu"))
    }

    @Test
    fun rejeitaRotaInvalidaSemAutenticacaoOuProprioUsuario() {
        assertFalse(parametrosDetalhesValidos("", "parca", "eu"))
        assertFalse(parametrosDetalhesValidos("role", "", "eu"))
        assertFalse(parametrosDetalhesValidos("role/invalido", "parca", "eu"))
        assertFalse(parametrosDetalhesValidos("role", "parca/invalido", "eu"))
        assertFalse(parametrosDetalhesValidos("role", "eu", "eu"))
        assertFalse(parametrosDetalhesValidos("role", "parca", null))
        assertFalse(parametrosDetalhesValidos("role", "parca", ""))
        assertFalse(parametrosDetalhesValidos(".", "parca", "eu"))
    }
}
