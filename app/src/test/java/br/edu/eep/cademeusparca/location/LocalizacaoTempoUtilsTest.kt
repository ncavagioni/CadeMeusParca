package br.edu.eep.cademeusparca.location

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizacaoTempoUtilsTest {
    private val agoraMs = 1_700_000_000_000L

    private fun timestampComIdade(idadeMs: Long): Timestamp {
        val instanteMs = agoraMs - idadeMs
        return Timestamp(instanteMs / 1_000L, (instanteMs % 1_000L).toInt() * 1_000_000)
    }

    @Test
    fun dezSegundosSaoRecentes() {
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(10_000L), agoraMs))
    }

    @Test
    fun cinquentaENoveSegundosSaoRecentes() {
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(59_000L), agoraMs))
    }

    @Test
    fun limiteDeSessentaSegundosEhInclusivo() {
        assertEquals(60_000L, LocalizacaoTempoUtils.LIMITE_LOCALIZACAO_RECENTE_MS)
        val limite = LocalizacaoTempoUtils.LIMITE_LOCALIZACAO_RECENTE_MS
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(limite - 1L), agoraMs))
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(limite), agoraMs))
        assertFalse(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(limite + 1L), agoraMs))
    }

    @Test
    fun acimaDoLimiteEhDesatualizada() {
        assertFalse(LocalizacaoTempoUtils.estaAtualizada(timestampComIdade(90_000L), agoraMs))
    }

    @Test
    fun menosDeUmMinutoMostraAgora() {
        assertEquals("Agora", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(59_999L), agoraMs
        ))
    }

    @Test
    fun formataUmMinuto() {
        for (idade in listOf(60_000L, 60_001L, 119_999L)) {
            assertEquals("Visto há 1 min", LocalizacaoTempoUtils.formatarVistoPorUltimo(
                timestampComIdade(idade), agoraMs
            ))
        }
    }

    @Test
    fun formataDoisEAteCinquentaENoveMinutos() {
        assertEquals("Visto há 2 min", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(120_000L), agoraMs
        ))
        assertEquals("Visto há 59 min", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(3_599_999L), agoraMs
        ))
    }

    @Test
    fun formataHoras() {
        for (horas in listOf(1L, 2L, 23L)) {
            assertEquals("Visto há $horas h", LocalizacaoTempoUtils.formatarVistoPorUltimo(
                timestampComIdade(horas * 3_600_000L), agoraMs
            ))
        }
    }

    @Test
    fun formataDiasComSingularEPlural() {
        assertEquals("Visto há 1 dia", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(86_400_000L), agoraMs
        ))
        assertEquals("Visto há 1 dia", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(172_799_999L), agoraMs
        ))
        assertEquals("Visto há 2 dias", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestampComIdade(172_800_000L), agoraMs
        ))
    }

    @Test
    fun timestampFuturoTemIdadeZero() {
        val futuro = timestampComIdade(-5_000L)
        assertEquals(0L, LocalizacaoTempoUtils.idadeMs(futuro, agoraMs))
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(futuro, agoraMs))
        assertEquals("Agora", LocalizacaoTempoUtils.formatarVistoPorUltimo(futuro, agoraMs))
    }

    @Test
    fun timestampAusenteNaoInventaHorarioNemRecencia() {
        assertNull(LocalizacaoTempoUtils.idadeMs(null, agoraMs))
        assertFalse(LocalizacaoTempoUtils.estaAtualizada(null, agoraMs))
        assertNull(LocalizacaoTempoUtils.formatarVistoPorUltimo(null, agoraMs))
    }

    @Test
    fun passagemDoTempoSemNovaPosicaoMudaOEstado() {
        val timestamp = timestampComIdade(10_000L)
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(timestamp, agoraMs))
        assertFalse(LocalizacaoTempoUtils.estaAtualizada(timestamp, agoraMs + 50_001L))
        assertEquals("Visto há 1 min", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestamp, agoraMs + 50_001L
        ))
        assertEquals("Visto há 2 min", LocalizacaoTempoUtils.formatarVistoPorUltimo(
            timestamp, agoraMs + 110_000L
        ))
        assertTrue(LocalizacaoTempoUtils.estaAtualizada(
            Timestamp((agoraMs + 110_000L) / 1_000L, 0), agoraMs + 110_000L
        ))
    }
}
