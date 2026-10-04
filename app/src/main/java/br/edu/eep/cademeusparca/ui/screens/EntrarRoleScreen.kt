package br.edu.eep.cademeusparca.ui.screens

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrarRoleScreen(
    viewModel: RoleViewModel,
    onVoltar: () -> Unit,
    onRoleEncontrado: (Role) -> Unit
) {
    val roleEncontrado = viewModel.roleEncontrado
    LaunchedEffect(roleEncontrado) {
        roleEncontrado?.let {
            onRoleEncontrado(it)
            viewModel.consumirRoleEncontrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrar em um rolê") },
                navigationIcon = {
                    IconButton(onClick = onVoltar, enabled = !viewModel.buscandoRole) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Digite o código compartilhado pelo seu parça.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = viewModel.codigoEntrada,
                onValueChange = viewModel::atualizarCodigoEntrada,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código do rolê") },
                supportingText = { Text("${viewModel.codigoEntrada.length}/6 caracteres") },
                singleLine = true,
                isError = viewModel.codigoInvalido,
                enabled = !viewModel.buscandoRole,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                )
            )
            if (viewModel.mensagemBusca.isNotBlank()) {
                Text(
                    text = viewModel.mensagemBusca,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = viewModel::buscarRolePorCodigo,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.buscandoRole
            ) {
                if (viewModel.buscandoRole) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (viewModel.buscandoRole) "Buscando..." else "Buscar rolê")
            }
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.buscandoRole
            ) {
                Text("Voltar para Meus rolês")
            }
        }
    }
}
