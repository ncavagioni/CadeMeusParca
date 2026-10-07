package br.edu.eep.cademeusparca.util

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RotasExternasUtilsTest {
    @Test
    fun destinoNaPreviaCorrespondeACoordenadaDoParca() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=-22.7%2C-47.6",
            RotasExternasUtils.uriMaps(-22.7, -47.6)
        )
    }

    @Test
    fun coordenadaInvalidaNaoProduzUri() {
        for ((latitude, longitude) in listOf(
            Double.NaN to 0.0, 0.0 to Double.POSITIVE_INFINITY, 91.0 to 0.0, 0.0 to -181.0
        )) {
            assertNull(RotasExternasUtils.uriMaps(latitude, longitude))
        }
    }

    @Test
    fun zeroZeroEhAceitoSomenteComoCoordenadaRecebidaValida() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=0.0%2C0.0",
            RotasExternasUtils.uriMaps(0.0, 0.0)
        )
    }

    @Test
    fun previaNaoImpoeTransporteNemIniciaNavegacao() {
        val uri = URI(RotasExternasUtils.uriMaps(-22.7, -47.6)!!)
        assertEquals("https", uri.scheme)
        assertEquals("www.google.com", uri.host)
        assertEquals("/maps/dir/", uri.path)
        // Somente api e destino: sem travelmode nem dir_action=navigate.
        assertEquals(
            setOf("api=1", "destination=-22.7,-47.6"),
            uri.query.split('&').toSet()
        )
    }
}
