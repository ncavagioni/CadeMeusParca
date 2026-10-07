package br.edu.eep.cademeusparca.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object RelogioLocalizacao {
    const val INTERVALO_MS = 15_000L

    fun iniciar(scope: CoroutineScope, aoAtualizar: (Long) -> Unit): Job = scope.launch {
        // Somente referência visual de tempo; nenhum acesso a GPS, Firestore ou rede.
        while (isActive) {
            aoAtualizar(System.currentTimeMillis())
            delay(INTERVALO_MS)
        }
    }
}
