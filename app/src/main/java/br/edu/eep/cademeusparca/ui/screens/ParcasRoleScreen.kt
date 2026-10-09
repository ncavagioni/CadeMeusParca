package br.edu.eep.cademeusparca.ui.screens

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.edu.eep.cademeusparca.model.ParcaListaUi
import br.edu.eep.cademeusparca.ui.theme.CadeMeusParcaTheme
import br.edu.eep.cademeusparca.viewmodel.ParcasRoleViewModel

@Composable
fun ParcasRoleScreen(
    roleId: String,
    viewModel: ParcasRoleViewModel,
    posicaoAndroidInicial: Location?,
    onVoltar: () -> Unit,
    onAbrirParca: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val posicaoInicial = rememberUpdatedState(posicaoAndroidInicial)
    DisposableEffect(lifecycleOwner, viewModel, roleId) {
        fun iniciar() = viewModel.iniciarObservacao(roleId, posicaoInicial.value)
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
    ParcasRoleLayout(
        parcas = viewModel.parcas,
        carregando = viewModel.carregando,
        mensagem = viewModel.mensagem,
        onVoltar = onVoltar,
        onTentarNovamente = { viewModel.iniciarObservacao(roleId, posicaoInicial.value) },
        onAbrirParca = { uid -> if (viewModel.podeAbrirParca(uid)) onAbrirParca(uid) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParcasRoleLayout(
    parcas: List<ParcaListaUi>,
    carregando: Boolean = false,
    mensagem: String = "",
    onVoltar: () -> Unit = {},
    onTentarNovamente: () -> Unit = {},
    onAbrirParca: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parças do rolê") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        when {
            carregando -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                CircularProgressIndicator()
                Text("Carregando parças...")
            }
            mensagem.isNotBlank() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(mensagem)
                OutlinedButton(onClick = onTentarNovamente) { Text("Tentar novamente") }
            }
            parcas.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) { Text("Nenhum parça encontrado neste rolê.") }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(parcas, key = { it.userId }) { parca ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !parca.proprioUsuario) { onAbrirParca(parca.userId) }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    parca.parcaname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                parca.parcaname + if (parca.proprioUsuario) " (Você)" else "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                parca.informacaoLocalizacao,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ParcasRolePreview() {
    CadeMeusParcaTheme {
        ParcasRoleLayout(listOf(
            ParcaListaUi("eu", "Nicolas", true, "Sua localização"),
            ParcaListaUi("ana", "Ana", false, "Visto há 5 min"),
            ParcaListaUi("parca", "Parça Teste", false, "1,7 km"),
            ParcaListaUi("sem", "Parça", false, "Localização indisponível")
        ))
    }
}
