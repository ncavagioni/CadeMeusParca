package br.edu.eep.cademeusparca.location

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanciaUtilsTest {
    private val localePtBr = Locale.forLanguageTag("pt-BR")

    @Test
    fun coordenadasIguaisResultamEmZero() {
        val distancia = DistanciaUtils.calcularMetros(-22.725, -47.649, -22.725, -47.649)
        assertEquals(0.0, distancia!!, 0.01)
    }

    @Test
    fun distanciaConhecidaNoEquadorFicaNaFaixaEsperada() {
        val distancia = DistanciaUtils.calcularMetros(0.0, 0.0, 0.0, 0.1)
        assertTrue(distancia!! in 11_000.0..11_200.0)
    }

    @Test
    fun formataAbaixoDeUmQuilometroSemCasasDecimais() {
        assertEquals("850 m", DistanciaUtils.formatar(849.6, localePtBr))
    }

    @Test
    fun formataExatamenteUmQuilometro() {
        assertEquals("1,0 km", DistanciaUtils.formatar(1_000.0, localePtBr))
    }

    @Test
    fun formataVariosQuilometrosComUmaCasaDecimal() {
        assertEquals("3,7 km", DistanciaUtils.formatar(3_650.0, localePtBr))
        assertEquals("12,4 km", DistanciaUtils.formatar(12_400.0, localePtBr))
    }

    @Test
    fun rejeitaCoordenadasInvalidas() {
        assertNull(DistanciaUtils.calcularMetros(Double.NaN, 0.0, 0.0, 0.0))
        assertNull(DistanciaUtils.calcularMetros(91.0, 0.0, 0.0, 0.0))
        assertNull(DistanciaUtils.calcularMetros(0.0, -181.0, 0.0, 0.0))
    }

    @Test
    fun rejeitaDistanciaInvalidaNaFormatacao() {
        assertNull(DistanciaUtils.formatar(-1.0, localePtBr))
        assertNull(DistanciaUtils.formatar(Double.POSITIVE_INFINITY, localePtBr))
    }
}
