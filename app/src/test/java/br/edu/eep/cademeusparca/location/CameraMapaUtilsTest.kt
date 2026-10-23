package br.edu.eep.cademeusparca.location

import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import org.junit.Assert.*
import org.junit.Test

class CameraMapaUtilsTest {
    private val android = -22.7 to -47.6
    private val propria = LocalizacaoParticipante("eu", -22.8, -47.7)
    private val outro = LocalizacaoParticipante("parca", -22.9, -47.8)

    @Test fun androidProprioContinuaPrioritario() {
        assertEquals(android, CameraMapaUtils.posicaoInicial(android, "eu", listOf(outro, propria), true))
    }

    @Test fun documentoProprioEhFallbackMesmoDuranteBuscaAndroid() {
        assertEquals(-22.8 to -47.7, CameraMapaUtils.posicaoInicial(null, "eu", listOf(outro, propria), true))
    }

    @Test fun esperaBuscaPropriaAntesDeCentralizarOutro() {
        assertNull(CameraMapaUtils.posicaoInicial(null, "eu", listOf(outro), true))
    }

    @Test fun buscaSemResultadoPodeUsarPosicaoRealDeOutro() {
        assertEquals(-22.9 to -47.8, CameraMapaUtils.posicaoInicial(null, "eu", listOf(outro), false))
    }

    @Test fun nenhumDocumentoNaoInventaCentroOuPosicao() {
        assertNull(CameraMapaUtils.posicaoInicial(null, "eu", emptyList(), false))
    }

    @Test fun rejeitaCoordenadasInvalidasEZeroZeroParaCamera() {
        val invalidas = listOf(
            propria.copy(latitude = Double.NaN),
            outro.copy(longitude = 181.0),
            outro.copy(userId = "zero", latitude = 0.0, longitude = 0.0)
        )
        assertNull(CameraMapaUtils.posicaoInicial(0.0 to 0.0, "eu", invalidas, false))
    }

    @Test fun androidInvalidoNaoImpedeFallbackProprio() {
        assertEquals(-22.8 to -47.7, CameraMapaUtils.posicaoInicial(Double.NaN to 0.0, "eu", listOf(propria), false))
    }

    @Test fun fallbackDosOutrosEhPrevisivelPorUid() {
        val primeiro = outro.copy(userId = "a", latitude = -23.0)
        assertEquals(
            -23.0 to -47.8,
            CameraMapaUtils.posicaoInicial(null, "eu", listOf(outro, primeiro), false)
        )
    }

    @Test fun semIdentidadeAtualNaoUsaPosicoesDeSessaoAnterior() {
        assertNull(CameraMapaUtils.posicaoInicial(android, "", listOf(propria, outro), false))
    }
}
