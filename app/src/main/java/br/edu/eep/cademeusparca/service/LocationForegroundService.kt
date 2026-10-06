package br.edu.eep.cademeusparca.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import br.edu.eep.cademeusparca.MainActivity
import br.edu.eep.cademeusparca.R
import br.edu.eep.cademeusparca.location.LocationRepository
import br.edu.eep.cademeusparca.repository.CompartilhamentoLocalizacaoRepository
import com.google.firebase.auth.FirebaseAuth

class LocationForegroundService : Service() {
    private lateinit var locationRepository: LocationRepository
    private val compartilhamento = CompartilhamentoLocalizacaoRepository
    private val handler = Handler(Looper.getMainLooper())
    private var recuperacao: Runnable? = null
    private var roleId: String? = null
    private var userId: String? = null
    private var callbackSolicitado = false
    private var callbackRegistrado = false
    private var geracaoCallback = 0
    private var encerrado = false
    private val authListener = FirebaseAuth.AuthStateListener {
        if (userId != null && it.currentUser?.uid != userId) {
            encerrar("A sessão de autenticação mudou. Abra o mapa novamente.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository(this, TAG)
        compartilhamento.configurar(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Compartilhamento de localização", NotificationManager.IMPORTANCE_LOW)
        )
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            encerrar()
            return START_NOT_STICKY
        }
        val novoRole = intent?.takeIf { it.action == ACTION_START }?.getStringExtra(EXTRA_ROLE_ID)
        Log.d(TAG, "SERVICE_START_REQUEST timestamp=${System.currentTimeMillis()}")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (novoRole.isNullOrBlank() || novoRole.contains('/') || uid == null) {
            encerrar("Não foi possível iniciar: rolê ou autenticação inválidos.")
            return START_NOT_STICKY
        }
        if (!locationRepository.temLocalizacaoPrecisa()) {
            encerrar("Habilite a localização precisa para compartilhar sua posição.")
            return START_NOT_STICKY
        }
        try {
            // Promover imediatamente, antes de registrar localização ou gravar no Firestore.
            val notification = criarNotificacao()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "SERVICE_FOREGROUND_STARTED timestamp=${System.currentTimeMillis()}")
        } catch (_: SecurityException) {
            encerrar("O Android não permitiu iniciar localização. Abra o mapa com permissão precisa.")
            return START_NOT_STICKY
        } catch (_: IllegalStateException) {
            encerrar("Não foi possível iniciar o serviço. Abra o mapa com o aplicativo visível.")
            return START_NOT_STICKY
        }

        val mudou = roleId != novoRole || userId != uid
        if (mudou) {
            val anterior = roleId
            roleId = novoRole
            userId = uid
            compartilhamento.iniciar(novoRole, uid)
            Log.d(TAG, "ROLE_CHANGED timestamp=${System.currentTimeMillis()} from=${anterior?.take(8)} to=${novoRole.take(8)}")
            // O mesmo callback passa a enviar ao destino atual; não há segundo stream.
            if (callbackRegistrado) compartilhamento.registrado()
        }
        iniciarColeta()
        return START_NOT_STICKY
    }

    private fun iniciarColeta() {
        if (encerrado || callbackSolicitado) return
        if (!locationRepository.temLocalizacaoPrecisa() ||
            FirebaseAuth.getInstance().currentUser?.uid != userId
        ) {
            encerrar("A permissão precisa ou a autenticação foi removida.")
            return
        }
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        callbackSolicitado = true
        val geracao = ++geracaoCallback
        locationRepository.iniciarAtualizacoes(
            onLocation = {
                if (!encerrado && geracao == geracaoCallback) {
                    if (locationRepository.localizacaoDoAparelhoAtivada()) compartilhamento.registrado()
                    compartilhamento.receberLocalizacao(it)
                }
            },
            onError = {
                if (!encerrado && geracao == geracaoCallback) {
                    callbackSolicitado = false
                    callbackRegistrado = false
                    compartilhamento.informarErro(it)
                    if (!locationRepository.temLocalizacaoPrecisa()) encerrar(it)
                    else agendarRecuperacao()
                }
            },
            onRegistered = {
                if (!encerrado && geracao == geracaoCallback) {
                    callbackRegistrado = true
                    compartilhamento.registrado()
                }
            },
            onAvailability = { disponivel ->
                if (!encerrado && geracao == geracaoCallback) {
                    if (disponivel) compartilhamento.registrado()
                    else compartilhamento.informarErro(
                        if (locationRepository.localizacaoDoAparelhoAtivada())
                            "Aguardando uma posição válida do aparelho."
                        else "A localização do aparelho está desligada. Ative-a para continuar."
                    )
                }
            }
        )
    }

    private fun agendarRecuperacao() {
        if (encerrado || recuperacao != null) return
        val tarefa = Runnable {
            recuperacao = null
            iniciarColeta()
        }
        recuperacao = tarefa
        handler.postDelayed(tarefa, 5_000L) // Apenas após falha no registro/GPS desligado.
    }

    private fun criarNotificacao(): Notification {
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val parar = PendingIntent.getService(
            this, 1, Intent(this, LocationForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_sharing)
            .setContentTitle("Compartilhando localização")
            .setContentText("Cadê meus parça? está compartilhando sua posição no rolê.")
            .setContentIntent(abrir)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_location_sharing, "Parar compartilhamento", parar)
            .build()
    }

    private fun encerrar(erro: String? = null) {
        if (encerrado) return
        encerrado = true
        geracaoCallback++
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
        locationRepository.pararAtualizacoes()
        callbackSolicitado = false
        callbackRegistrado = false
        compartilhamento.parar(erro)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "SERVICE_STOPPED timestamp=${System.currentTimeMillis()} error=${erro != null}")
    }

    override fun onDestroy() {
        encerrar()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "br.edu.eep.cademeusparca.location.START"
        const val ACTION_STOP = "br.edu.eep.cademeusparca.location.STOP"
        const val EXTRA_ROLE_ID = "roleId"
        private const val TAG = "LocationForegroundService"
        private const val CHANNEL_ID = "compartilhamento_localizacao"
        private const val NOTIFICATION_ID = 8
    }
}
