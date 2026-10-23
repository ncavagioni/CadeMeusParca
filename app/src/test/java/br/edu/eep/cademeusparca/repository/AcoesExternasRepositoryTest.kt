package br.edu.eep.cademeusparca.repository

import org.junit.Assert.assertTrue
import org.junit.Test

class AcoesExternasRepositoryTest {

    private val repository = AcoesExternasRepository()

    @Test
    fun formatarMensagemCompartilhamentoGeraTextoCorreto() {
        val mensagem = repository.formatarMensagemCompartilhamento("Churrasco PIC IV", "FEVECG")
        assertTrue(mensagem.contains("Churrasco PIC IV"))
        assertTrue(mensagem.contains("FEVECG"))
        assertTrue(mensagem.contains("Cadê meus parça"))
    }

    @Test
    fun formatarMensagemCompartilhamentoTrataNomeVazio() {
        val mensagem = repository.formatarMensagemCompartilhamento("   ", "ABC123")
        assertTrue(mensagem.contains("Rolê"))
        assertTrue(mensagem.contains("ABC123"))
    }
}
