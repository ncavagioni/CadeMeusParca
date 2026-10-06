package br.edu.eep.cademeusparca.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import br.edu.eep.cademeusparca.viewmodel.FilaUltimaPosicao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StatusCompartilhamento { PARADO, INICIANDO, COMPARTILHANDO, ERRO }

data class EstadoCompartilhamento(
    val roleId: String? = null,
    val userId: String? = null,
    val status: StatusCompartilhamento = StatusCompartilhamento.PARADO,
    val localizacao: Location? = null,
    val erro: String? = null
)

// Estado e uma única fila para serviço e atualização manual. Todos os acessos são na main thread.
// Uma Task em andamento continua mantendo a trava mesmo após stop/troca de rolê.
object CompartilhamentoLocalizacaoRepository {
    private val estadoMutavel = MutableStateFlow(EstadoCompartilhamento())
    val estado = estadoMutavel.asStateFlow()
    private val sessao = SessaoCompartilhamento()
    private val repository = LocalizacaoRepository()
    private val fila = FilaUltimaPosicao<Gravacao>()
    private val handler = Handler(Looper.getMainLooper())
    private var recuperacao: Runnable? = null
    private var context: Context? = null
    private var ativo = false
    private var erroColeta: String? = null
    private var erroEnvio: String? = null
    private var versaoManual = 0L
    private val pedidosManuais = mutableListOf<PedidoManual>()

    private data class Gravacao(
        val destino: SessaoCompartilhamento.Destino,
        val location: Location,
        val versaoManual: Long
    )
    private data class PedidoManual(
        val destino: SessaoCompartilhamento.Destino,
        val versao: Long,
        val resultado: (Boolean, String?) -> Unit
    )

    fun configurar(context: Context) {
        this.context = context.applicationContext
    }

    fun iniciar(roleId: String, userId: String) {
        selecionarDestino(roleId, userId)
        ativo = true
        erroColeta = null
        erroEnvio = null
        estadoMutavel.value = estado.value.copy(
            roleId = roleId, status = StatusCompartilhamento.INICIANDO, erro = null
        )
    }

    fun registrado() {
        erroColeta = null
        if (ativo) atualizarEstadoAtivo()
    }

    fun informarErro(mensagem: String, falhaEnvio: Boolean = false) {
        if (falhaEnvio) erroEnvio = mensagem else erroColeta = mensagem
        estadoMutavel.value = estado.value.copy(
            status = StatusCompartilhamento.ERRO, erro = erroColeta ?: erroEnvio
        )
    }

    private fun atualizarEstadoAtivo() {
        val erro = erroColeta ?: erroEnvio
        estadoMutavel.value = estado.value.copy(
            status = if (erro == null) StatusCompartilhamento.COMPARTILHANDO else StatusCompartilhamento.ERRO,
            erro = erro
        )
    }

    fun parar(erro: String? = null) {
        ativo = false
        erroColeta = erro
        erroEnvio = null
        cancelarRecuperacao()
        sessao.encerrar()
        fila.descartarPendente()
        finalizarManuais("Compartilhamento interrompido.")
        estadoMutavel.value = estado.value.copy(
            status = if (erro == null) StatusCompartilhamento.PARADO else StatusCompartilhamento.ERRO,
            erro = erro
        )
    }

    fun receberLocalizacao(location: Location) {
        if (!ativo) return
        val destino = sessao.destino ?: return
        val maisRecente = atualizarLocal(location)
        fila.oferecer(Gravacao(destino, maisRecente, versaoManual))
        processarFila()
    }

    fun salvarManual(roleId: String, location: Location, resultado: (Boolean, String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || !permissaoPrecisa()) {
            resultado(false, "É necessário estar autenticado e permitir localização precisa.")
            return
        }
        if (ativo && sessao.destino?.roleId != roleId) {
            resultado(false, "Outro rolê está ativo. Reabra este mapa antes de atualizar.")
            return
        }
        val destino = selecionarDestino(roleId, uid)
        val maisRecente = atualizarLocal(location)
        pedidosManuais.add(PedidoManual(destino, ++versaoManual, resultado))
        fila.oferecer(Gravacao(destino, maisRecente, versaoManual))
        processarFila()
    }

    private fun selecionarDestino(roleId: String, uid: String): SessaoCompartilhamento.Destino {
        val anterior = sessao.destino
        val destino = sessao.selecionar(roleId, uid)
        if (destino != anterior) {
            cancelarRecuperacao()
            fila.descartarPendente()
            finalizarManuais("O rolê ativo foi alterado.")
            estadoMutavel.value = estado.value.copy(
                roleId = roleId,
                userId = uid,
                localizacao = estado.value.localizacao.takeIf { estado.value.userId == uid },
                erro = null
            )
        }
        return destino
    }

    private fun atualizarLocal(location: Location): Location {
        val anterior = estado.value.localizacao
        val nova = if (anterior != null && anterior.elapsedRealtimeNanos > location.elapsedRealtimeNanos)
            anterior else Location(location)
        estadoMutavel.value = estado.value.copy(localizacao = nova)
        return Location(nova)
    }

    private fun processarFila() {
        val pedido = fila.retirarParaGravar() ?: return
        if (!sessao.aceita(pedido.destino) || !permissaoPrecisa() ||
            FirebaseAuth.getInstance().currentUser?.uid != pedido.destino.userId
        ) {
            fila.concluir()
            finalizarManuais("A sessão ou a permissão de localização mudou.")
            return
        }
        val location = pedido.location
        repository.salvarMinhaLocalizacao(
            pedido.destino.roleId, location.latitude, location.longitude,
            if (location.hasAccuracy()) location.accuracy.toDouble() else null
        ) { sucesso, erro ->
            fila.concluir() // Inclusive se o serviço parou ou o destino mudou durante a Task.
            if (sessao.aceita(pedido.destino)) {
                val concluidos = pedidosManuais.filter {
                    it.destino == pedido.destino && it.versao <= pedido.versaoManual
                }
                pedidosManuais.removeAll(concluidos.toSet())
                concluidos.forEach { it.resultado(sucesso, erro) }
                if (!sucesso) {
                    informarErro(erro ?: "Não foi possível enviar sua posição.", falhaEnvio = true)
                    if (ativo) {
                        fila.reporSeVazia(pedido) // Uma posição mais nova sempre tem prioridade.
                        agendarRecuperacao()
                        return@salvarMinhaLocalizacao
                    }
                } else if (ativo) {
                    // Confirmar escrita não deve apagar um erro de GPS/permissão.
                    erroEnvio = null
                    atualizarEstadoAtivo()
                }
            }
            processarFila()
        }
    }

    private fun agendarRecuperacao() {
        if (!ativo || recuperacao != null) return
        val tarefa = Runnable {
            recuperacao = null
            if (ativo) processarFila()
        }
        recuperacao = tarefa
        handler.postDelayed(tarefa, 5_000L) // Só recuperação de escrita, não coleta periódica.
    }

    private fun cancelarRecuperacao() {
        recuperacao?.let(handler::removeCallbacks)
        recuperacao = null
    }

    private fun finalizarManuais(mensagem: String) {
        val anteriores = pedidosManuais.toList()
        pedidosManuais.clear()
        anteriores.forEach { it.resultado(false, mensagem) }
    }

    private fun permissaoPrecisa(): Boolean = context?.let {
        ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    } == true
}
