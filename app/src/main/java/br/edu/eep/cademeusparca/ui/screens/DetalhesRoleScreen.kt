package br.edu.eep.cademeusparca.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.ui.theme.CadeMeusParcaTheme
import br.edu.eep.cademeusparca.viewmodel.DetalhesRoleUi
import br.edu.eep.cademeusparca.viewmodel.DetalhesRoleViewModel
import br.edu.eep.cademeusparca.viewmodel.comporDetalhesRole

@Composable
fun DetalhesRoleScreen(
    roleId: String,
    viewModel: DetalhesRoleViewModel,
    onVoltar: () -> Unit,
    onSairDoRoleConcluido: () -> Unit = {},
    onVerParcas: () -> Unit,
    onEditarRole: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var exibirDialogoSair by remember { mutableStateOf(false) }
    var exibirDialogoEncerrar by remember { mutableStateOf(false) }
    var exibirDialogoExcluir by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel, roleId) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> viewModel.iniciarObservacao(roleId, atualizar = true)
                Lifecycle.Event.ON_STOP -> viewModel.pararObservacao()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.iniciarObservacao(roleId, atualizar = true)
        }
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

    if (exibirDialogoSair) {
        AlertDialog(
            onDismissRequest = { exibirDialogoSair = false },
            title = { Text("Sair do rolê") },
            text = { Text("Deseja realmente sair deste rolê? Você deixará de compartilhar sua localização e de ver os parças.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exibirDialogoSair = false
                        viewModel.sairDoRole(context, onSairDoRoleConcluido)
                    }
                ) { Text("Sair", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exibirDialogoSair = false }) { Text("Cancelar") }
            }
        )
    }

    if (exibirDialogoEncerrar) {
        AlertDialog(
            onDismissRequest = { exibirDialogoEncerrar = false },
            title = { Text("Encerrar rolê") },
            text = { Text("Deseja realmente encerrar este rolê? O rolê será finalizado e novos participantes não poderão entrar com o código.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exibirDialogoEncerrar = false
                        viewModel.encerrarRole(context) {}
                    }
                ) { Text("Encerrar rolê", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exibirDialogoEncerrar = false }) { Text("Cancelar") }
            }
        )
    }

    if (exibirDialogoExcluir) {
        AlertDialog(
            onDismissRequest = { exibirDialogoExcluir = false },
            title = { Text("Excluir rolê") },
            text = { Text("Deseja realmente excluir este rolê? Esta ação é definitiva e removerá o rolê e todos os seus dados para todos os participantes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exibirDialogoExcluir = false
                        viewModel.excluirRole(context, onSairDoRoleConcluido)
                    }
                ) { Text("Excluir rolê", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { exibirDialogoExcluir = false }) { Text("Cancelar") }
            }
        )
    }

    DetalhesRoleLayout(
        dados = viewModel.dados,
        carregando = viewModel.carregando || viewModel.saindoDoRole || viewModel.encerrandoRole || viewModel.excluindoRole,
        mensagem = viewModel.mensagem,
        aviso = viewModel.aviso,
        snackbar = snackbar,
        onVoltar = onVoltar,
        onTentarNovamente = { viewModel.iniciarObservacao(roleId) },
        onCopiarCodigo = viewModel::copiarCodigo,
        onCompartilharCodigo = { viewModel.compartilharCodigo(context) },
        onVerParcas = { if (viewModel.podeVerParcas()) onVerParcas() },
        onEditarRole = { if (viewModel.podeEditarRole()) onEditarRole() },
        onSairDoRole = { exibirDialogoSair = true },
        onEncerrarRole = { exibirDialogoEncerrar = true },
        onExcluirRole = { exibirDialogoExcluir = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalhesRoleLayout(
    dados: DetalhesRoleUi?,
    carregando: Boolean = false,
    mensagem: String = "",
    aviso: String = "",
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onVoltar: () -> Unit = {},
    onTentarNovamente: () -> Unit = {},
    onCopiarCodigo: () -> Unit = {},
    onCompartilharCodigo: () -> Unit = {},
    onVerParcas: () -> Unit = {},
    onEditarRole: () -> Unit = {},
    onSairDoRole: () -> Unit = {},
    onEncerrarRole: () -> Unit = {},
    onExcluirRole: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do rolê") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when {
            carregando -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                CircularProgressIndicator()
                Text("Processando...")
            }
            mensagem.isNotBlank() || dados == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(mensagem.ifBlank { "Não foi possível carregar os detalhes do rolê." })
                OutlinedButton(onClick = onTentarNovamente) { Text("Tentar novamente") }
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(dados.nome, style = MaterialTheme.typography.headlineSmall)
                DetalheRoleCampo("Status", dados.status)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Código do rolê", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        dados.codigo.ifBlank { "Código indisponível" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopiarCodigo,
                            enabled = dados.codigo.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copiar código")
                        }
                        if (dados.status.lowercase().contains("ativo")) {
                            Button(
                                onClick = onCompartilharCodigo,
                                enabled = dados.codigo.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Compartilhar")
                            }
                        }
                    }
                }
                if (dados.nomeLocal != null) DetalheRoleCampo("Local", dados.nomeLocal)
                if (dados.endereco != null) DetalheRoleCampo("Endereço", dados.endereco)
                if (dados.nomeLocal == null && dados.endereco == null) {
                    Text("Local do rolê não informado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DetalheRoleCampo("Administrador", dados.administrador)
                DetalheRoleCampo("Participantes", dados.participantes)
                if (aviso.isNotBlank()) Text(aviso, color = MaterialTheme.colorScheme.error)
                Button(onClick = onVerParcas, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver parças do rolê")
                }
                if (dados.podeEditar) {
                    OutlinedButton(onClick = onEditarRole, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar rolê")
                    }
                }
                if (!dados.eAdmin) {
                    OutlinedButton(
                        onClick = onSairDoRole,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sair do rolê")
                    }
                } else {
                    if (dados.status.lowercase().contains("ativo")) {
                        OutlinedButton(
                            onClick = onEncerrarRole,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Encerrar rolê")
                        }
                    } else if (dados.status.lowercase().contains("encerrado")) {
                        OutlinedButton(
                            onClick = onExcluirRole,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir rolê")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalheRoleCampo(titulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalhesRolePreview() {
    CadeMeusParcaTheme {
        DetalhesRoleLayout(
            comporDetalhesRole(
                Role(
                    roleId = "teste", nome = "Churrasco PIC IV", codigo = "FEVECG",
                    nomeLocal = "Casa do Nicolas", endereco = "Rua de Teste, 123", adminId = "eu"
                ),
                "eu", listOf(PerfilPublicoRole("eu", "Nicolas")), 2
            )
        )
    }
}
