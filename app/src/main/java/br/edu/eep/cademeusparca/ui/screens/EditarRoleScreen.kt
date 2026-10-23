package br.edu.eep.cademeusparca.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.viewmodel.EditarRoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarRoleScreen(
    viewModel: EditarRoleViewModel,
    onSelecionarLocal: () -> Unit,
    onCancelar: () -> Unit,
    onTentarNovamente: () -> Unit
) {
    // Evita sair com uma gravação ainda sem confirmação e interpretá-la como cancelada.
    BackHandler(enabled = viewModel.salvando) {}
    val formulario = viewModel.formulario
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar rolê") },
                navigationIcon = {
                    IconButton(onClick = onCancelar, enabled = !viewModel.salvando) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                viewModel.carregando || (formulario == null && viewModel.mensagem.isBlank()) -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Carregando dados do rolê...")
                }
                formulario == null -> {
                    Text(viewModel.mensagem, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onTentarNovamente, modifier = Modifier.fillMaxWidth()) {
                        Text("Tentar novamente")
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = formulario.nome,
                        onValueChange = viewModel::atualizarNome,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome do rolê") },
                        supportingText = { Text("Obrigatório") },
                        isError = viewModel.nomeInvalido,
                        singleLine = true,
                        enabled = viewModel.podeSalvar,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = formulario.nomeLocal,
                        onValueChange = viewModel::atualizarNomeLocal,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Local do rolê (opcional)") },
                        singleLine = true,
                        enabled = viewModel.podeSalvar,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = formulario.endereco,
                        onValueChange = viewModel::atualizarEndereco,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Endereço (opcional)") },
                        enabled = viewModel.podeSalvar,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    OutlinedButton(
                        onClick = onSelecionarLocal,
                        enabled = viewModel.podeSalvar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (formulario.pontoSelecionado == null) "Selecionar no mapa"
                            else "Alterar seleção no mapa"
                        )
                    }
                    formulario.pontoSelecionado?.let { ponto ->
                        Text(
                            "Ponto selecionado no mapa: ${ponto.latitude}, ${ponto.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        TextButton(
                            onClick = viewModel::limparLocal,
                            enabled = viewModel.podeSalvar
                        ) { Text("Limpar seleção") }
                    }
                    if (viewModel.mensagem.isNotBlank()) {
                        Text(viewModel.mensagem, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = viewModel::salvarAlteracoes,
                        enabled = viewModel.podeSalvar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.salvando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (viewModel.salvando) "Salvando..." else "Salvar alterações")
                    }
                }
            }
            OutlinedButton(
                onClick = onCancelar,
                enabled = !viewModel.salvando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    }
}
