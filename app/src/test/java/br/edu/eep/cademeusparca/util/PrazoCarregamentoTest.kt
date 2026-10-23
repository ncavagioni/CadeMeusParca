package br.edu.eep.cademeusparca.util

import org.junit.Assert.*
import org.junit.Test

class PrazoCarregamentoTest {
    private data class Tarefa(val atraso: Long, val executar: () -> Unit, var cancelada: Boolean = false)
    private val tarefas = mutableListOf<Tarefa>()
    private val prazo = PrazoCarregamento { atraso, executar ->
        val tarefa = Tarefa(atraso, executar)
        tarefas.add(tarefa)
        val cancelar: () -> Unit = { tarefa.cancelada = true }
        cancelar
    }

    @Test fun agendaTrintaSegundosSemExecutarAoIniciar() {
        var expiracoes = 0
        prazo.iniciar { expiracoes++ }
        assertEquals(30_000L, tarefas.single().atraso)
        assertEquals(0, expiracoes)
    }

    @Test fun expiracaoExecutaSomenteUmaVez() {
        var expiracoes = 0
        prazo.iniciar { expiracoes++ }
        repeat(3) { tarefas.single().executar() }
        assertEquals(1, expiracoes)
    }

    @Test fun sucessoCancelaPrazoEIgnoraCallbackTardio() {
        var expiracoes = 0
        prazo.iniciar { expiracoes++ }
        prazo.cancelar()
        assertTrue(tarefas.single().cancelada)
        tarefas.single().executar()
        assertEquals(0, expiracoes)
    }

    @Test fun novaTentativaInvalidaPrazoAnterior() {
        var anterior = 0
        var nova = 0
        prazo.iniciar { anterior++ }
        prazo.iniciar { nova++ }
        assertTrue(tarefas.first().cancelada)
        tarefas.first().executar()
        assertEquals(0, anterior)
        tarefas.last().executar()
        assertEquals(1, nova)
    }

    @Test fun sairDaTelaCancelaTodasAsTentativasAntigas() {
        var expiracoes = 0
        repeat(3) { prazo.iniciar { expiracoes++ } }
        prazo.cancelar()
        tarefas.forEach { it.executar() }
        assertEquals(0, expiracoes)
        assertTrue(tarefas.all { it.cancelada })
    }

    @Test fun permiteRepetirDepoisDeExpirar() {
        var expiracoes = 0
        prazo.iniciar { expiracoes++ }
        tarefas.first().executar()
        prazo.iniciar { expiracoes++ }
        tarefas.first().executar()
        tarefas.last().executar()
        assertEquals(2, expiracoes)
    }

    @Test fun cancelarSemAgendamentoOuAposExpiracaoEhSeguro() {
        prazo.cancelar()
        prazo.cancelar()
        prazo.iniciar {}
        tarefas.single().executar()
        prazo.cancelar()
        prazo.cancelar()
    }

    @Test fun agendamentoSincronoNaoDeixaCancelamentoAntigoRegistrado() {
        var expiracoes = 0
        var cancelamentos = 0
        val imediato = PrazoCarregamento { _, executar ->
            executar()
            val cancelar: () -> Unit = { cancelamentos++ }
            cancelar
        }
        imediato.iniciar { expiracoes++ }
        imediato.cancelar()
        assertEquals(1, expiracoes)
        assertEquals(1, cancelamentos)
    }
}
