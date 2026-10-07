package br.edu.eep.cademeusparca.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelefoneUtilsTest {
    @Test
    fun removeFormatacaoParaSomenteDigitos() {
        assertEquals("19999990001", TelefoneUtils.somenteDigitos("(19) 99999-0001"))
    }

    @Test
    fun adicionaBrasilAoCelularComOnzeDigitos() {
        assertEquals("5519999990001", TelefoneUtils.numeroWhatsApp("19999990001"))
    }

    @Test
    fun adicionaBrasilAoFixoComDezDigitos() {
        assertEquals("551933330001", TelefoneUtils.numeroWhatsApp("(19) 3333-0001"))
    }

    @Test
    fun naoDuplicaCodigoDoPais() {
        assertEquals("5519999990001", TelefoneUtils.numeroWhatsApp("5519999990001"))
        assertEquals("551933330001", TelefoneUtils.numeroWhatsApp("551933330001"))
    }

    @Test
    fun telefoneVazioNaoGeraUri() {
        for (telefone in listOf("", "   ")) {
            assertNull(TelefoneUtils.numeroWhatsApp(telefone))
            assertNull(TelefoneUtils.uriWhatsApp(telefone))
            assertNull(TelefoneUtils.uriDiscador(telefone))
        }
    }

    @Test
    fun normalizaMaisBrasilESimbolos() {
        assertEquals("5519999990001", TelefoneUtils.numeroWhatsApp("+55 (19) 99999-0001"))
    }

    @Test
    fun dddCinquentaECincoAindaRecebeCodigoDoPais() {
        assertEquals("5555999990001", TelefoneUtils.numeroWhatsApp("(55) 99999-0001"))
    }

    @Test
    fun naoCompletaNumerosInvalidosNemInterpretaComandos() {
        for (telefone in listOf(
            "99999-0001", "00000000000", "(00) 99999-0001", "551999999000123",
            "19+999990001", "++55 19999990001", "telefone 19999990001",
            "19999990001#123", "*19999990001", "+19 99999-0001"
        )) {
            assertNull(telefone, TelefoneUtils.numeroWhatsApp(telefone))
            assertNull(telefone, TelefoneUtils.uriDiscador(telefone))
        }
    }

    @Test
    fun discadorMantemNumeroSanitizadoSemChamadaDireta() {
        assertEquals("tel:19999990001", TelefoneUtils.uriDiscador("(19) 99999-0001"))
        assertEquals("tel:+5519999990001", TelefoneUtils.uriDiscador("+55 (19) 99999-0001"))
        assertEquals("tel:+5519999990001", TelefoneUtils.uriDiscador("5519999990001"))
    }

    @Test
    fun whatsappUsaUrlComNumeroCompleto() {
        assertEquals("https://wa.me/5519999990001", TelefoneUtils.uriWhatsApp("(19) 99999-0001"))
    }
}
