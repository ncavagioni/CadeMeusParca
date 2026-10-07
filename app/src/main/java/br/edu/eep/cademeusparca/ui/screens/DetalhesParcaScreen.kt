package br.edu.eep.cademeusparca.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.ui.theme.CadeMeusParcaTheme
import br.edu.eep.cademeusparca.viewmodel.DetalhesParcaViewModel
import br.edu.eep.cademeusparca.viewmodel.EstadoLocalizacaoParca
import com.google.firebase.Timestamp
import java.text.DateFormat

@Composable
fun DetalhesParcaScreen(
    roleId: String,
    userId: String,
    viewModel: DetalhesParcaViewModel,
    posicaoAndroidInicial: Location?,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val posicaoInicial = rememberUpdatedState(posicaoAndroidInicial)
    val snackbar = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner, viewModel, roleId, userId) {
        fun iniciar() = viewModel.iniciarObservacao(roleId, userId, posicaoInicial.value)
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> iniciar()
                Lifecycle.Event.ON_STOP -> viewModel.pararObservacao()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) iniciar()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pararObservacao()
        }
    }

    LaunchedEffect(viewModel.mensagemAcao) {
        val mensagem = viewModel.mensagemAcao
        if (mensagem.isNotBlank()) {
            snackbar.showSnackbar(mensagem)
            viewModel.consumirMensagemAcao()
        }
    }

    DetalhesParcaLayout(
        perfil = viewModel.perfil,
        localizacao = viewModel.localizacao,
        carregando = viewModel.carregando,
        mensagem = viewModel.mensagem,
        podeLigar = viewModel.podeLigar,
        podeWhatsApp = viewModel.podeWhatsApp,
        podeTracarRota = viewModel.podeTracarRota,
        snackbar = snackbar,
        onVoltar = onVoltar,
        onTentarNovamente = { viewModel.iniciarObservacao(roleId, userId, posicaoInicial.value) },
        onLigar = { viewModel.ligar(context) },
        onWhatsApp = { viewModel.whatsapp(context) },
        onTracarRota = { viewModel.tracarRota(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalhesParcaLayout(
    perfil: PerfilPublicoRole?,
    localizacao: EstadoLocalizacaoParca,
    carregando: Boolean = false,
    mensagem: String = "",
    podeLigar: Boolean = false,
    podeWhatsApp: Boolean = false,
    podeTracarRota: Boolean = false,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onVoltar: () -> Unit = {},
    onTentarNovamente: () -> Unit = {},
    onLigar: () -> Unit = {},
    onWhatsApp: () -> Unit = {},
    onTracarRota: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do parça") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (carregando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                CircularProgressIndicator()
                Text("Verificando participação e carregando o parça...")
            }
        } else if (mensagem.isNotBlank() || perfil == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(mensagem.ifBlank { "Não foi possível carregar os dados deste parça." })
                OutlinedButton(onClick = onTentarNovamente) { Text("Tentar novamente") }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                perfil.parcaname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                    Text(perfil.parcaname, style = MaterialTheme.typography.headlineSmall)
                    Text(localizacao.textoPrincipal, style = MaterialTheme.typography.titleMedium)
                    if (localizacao.destino != null &&
                        (localizacao.distancia != null || !localizacao.recente)
                    ) {
                        Text(
                            if (localizacao.recente) "Localização recente" else "Última posição conhecida",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Última atualização", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        localizacao.destino?.atualizadoEm?.let {
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(it.toDate())
                        } ?: "Horário indisponível"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Telefone", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(perfil.telefone.ifBlank { "Telefone não informado" })
                    if (perfil.telefone.isNotBlank() && !podeLigar && !podeWhatsApp) {
                        Text(
                            "Número inválido para ligar ou abrir o WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onLigar, enabled = podeLigar, modifier = Modifier.weight(1f)
                    ) { Text("Ligar") }
                    OutlinedButton(
                        onClick = onWhatsApp, enabled = podeWhatsApp, modifier = Modifier.weight(1f)
                    ) { Text("WhatsApp") }
                }
                Button(
                    onClick = onTracarRota,
                    enabled = podeTracarRota,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Traçar rota") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhesParcaRecentePreview() {
    CadeMeusParcaTheme {
        DetalhesParcaLayout(
            perfil = PerfilPublicoRole("parca", "Parça Teste", "(19) 99999-0001"),
            localizacao = EstadoLocalizacaoParca(
                "284 m de você", true, "284 m",
                LocalizacaoParticipante("parca", -22.7, -47.6, atualizadoEm = Timestamp(1_700_000_000L, 0))
            ),
            podeLigar = true, podeWhatsApp = true, podeTracarRota = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhesParcaAntigaPreview() {
    CadeMeusParcaTheme {
        DetalhesParcaLayout(
            perfil = PerfilPublicoRole("parca", "Parça Teste"),
            localizacao = EstadoLocalizacaoParca(
                textoPrincipal = "Visto há 5 min",
                destino = LocalizacaoParticipante(
                    "parca", -22.7, -47.6, atualizadoEm = Timestamp(1_700_000_000L, 0)
                )
            ),
            podeTracarRota = true
        )
    }
}
