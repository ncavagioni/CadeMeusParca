package br.edu.eep.cademeusparca.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarRoleScreen(
    viewModel: RoleViewModel,
    onVoltar: () -> Unit,
    onRoleCriado: (String, String) -> Unit
) {
    val roleCriado = viewModel.roleCriado
    LaunchedEffect(roleCriado) {
        roleCriado?.let { onRoleCriado(it.nome, it.codigo) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar um rolê") },
                navigationIcon = {
                    IconButton(onClick = onVoltar, enabled = !viewModel.carregando) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.nome,
                onValueChange = viewModel::atualizarNome,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome do rolê") },
                supportingText = { Text("Obrigatório") },
                isError = viewModel.nomeInvalido,
                singleLine = true,
                enabled = !viewModel.carregando,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = viewModel.nomeLocal,
                onValueChange = viewModel::atualizarNomeLocal,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Local do rolê (opcional)") },
                singleLine = true,
                enabled = !viewModel.carregando,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = viewModel.endereco,
                onValueChange = viewModel::atualizarEndereco,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Endereço (opcional)") },
                enabled = !viewModel.carregando,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (viewModel.mensagem.isNotBlank()) {
                Text(
                    text = viewModel.mensagem,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = viewModel::criarRole,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.carregando && roleCriado == null
            ) {
                if (viewModel.carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (viewModel.carregando) "Criando..." else "Criar rolê")
            }

            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.carregando
            ) {
                Text("Voltar para Meus rolês")
            }
        }
    }
}
