package br.edu.eep.cademeusparca.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Limita uma espera inicial, sem polling e sem autorizar dados por timeout. */
internal class PrazoCarregamento(
    private val agendar: (Long, () -> Unit) -> (() -> Unit)
) {
    private var geracao = 0
    private var cancelarAgendamento: (() -> Unit)? = null

    fun iniciar(aoExpirar: () -> Unit) {
        cancelar()
        val versao = geracao
        val cancelarNovo = agendar(LIMITE_MS) {
            if (versao == geracao) {
                geracao++
                cancelarAgendamento = null
                aoExpirar()
            }
        }
        if (versao == geracao) cancelarAgendamento = cancelarNovo else cancelarNovo()
    }

    fun cancelar() {
        geracao++
        cancelarAgendamento?.invoke()
        cancelarAgendamento = null
    }

    companion object {
        const val LIMITE_MS = 30_000L

        fun criar(scope: CoroutineScope): PrazoCarregamento = PrazoCarregamento { atraso, acao ->
            val job = scope.launch {
                delay(atraso)
                acao()
            }
            val cancelar: () -> Unit = { job.cancel() }
            cancelar
        }
    }
}
