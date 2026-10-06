package br.edu.eep.cademeusparca.service

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.repository.CompartilhamentoLocalizacaoRepository
import br.edu.eep.cademeusparca.repository.StatusCompartilhamento

object LocationServiceController {
    private var notificacoesPermitidasNoUltimoInicio: Boolean? = null
    // Nunca chamado por listener/retry/background: somente pela Tela 08 em RESUMED.
    fun iniciar(activity: ComponentActivity, roleId: String) {
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (roleId.isBlank() || roleId.contains('/')) return
        val repository = LocationRepository(activity)
        if (!repository.temLocalizacaoPrecisa()) {
            activity.stopService(Intent(activity, LocationForegroundService::class.java))
            CompartilhamentoLocalizacaoRepository.informarErro("Habilite a localização precisa para compartilhar sua posição.")
            return
        }
        val notificacoesPermitidas = NotificationManagerCompat.from(activity).areNotificationsEnabled()
        val reexibirNotificacao = notificacoesPermitidas && notificacoesPermitidasNoUltimoInicio != true
        notificacoesPermitidasNoUltimoInicio = notificacoesPermitidas
        val estado = CompartilhamentoLocalizacaoRepository.estado.value
        if (estado.roleId == roleId &&
            estado.status in setOf(StatusCompartilhamento.INICIANDO, StatusCompartilhamento.COMPARTILHANDO) &&
            !reexibirNotificacao
        ) return
        try {
            ContextCompat.startForegroundService(
                activity, Intent(activity, LocationForegroundService::class.java)
                    .setAction(LocationForegroundService.ACTION_START)
                    .putExtra(LocationForegroundService.EXTRA_ROLE_ID, roleId)
            )
        } catch (_: SecurityException) {
            CompartilhamentoLocalizacaoRepository.informarErro("Permita localização precisa e abra o mapa novamente.")
            Log.w("LocationForegroundService", "SERVICE_START_REJECTED timestamp=${System.currentTimeMillis()} type=SecurityException")
        } catch (_: IllegalStateException) {
            // Inclui ForegroundServiceStartNotAllowedException no Android 12+.
            CompartilhamentoLocalizacaoRepository.informarErro("O Android bloqueou o início. Abra o mapa com o app visível.")
            Log.w("LocationForegroundService", "SERVICE_START_REJECTED timestamp=${System.currentTimeMillis()} type=IllegalStateException")
        }
    }
}
