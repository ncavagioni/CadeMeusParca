package br.edu.eep.cademeusparca.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmarRoleScreen(
    role: Role,
    viewModel: RoleViewModel,
    onCancelar: () -> Unit,
    onVoltarMeusRoles: () -> Unit
) {
    LaunchedEffect(viewModel.entradaConcluida) {
        if (viewModel.entradaConcluida) onVoltarMeusRoles()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Confirmar rolê") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(role.nome, style = MaterialTheme.typography.headlineMedium)
            Text("Código: ${role.codigo}", style = MaterialTheme.typography.titleMedium)
            if (role.nomeLocal.isNotBlank()) {
                Text("Local: ${role.nomeLocal}", style = MaterialTheme.typography.bodyLarge)
            }
            if (role.endereco.isNotBlank()) {
                Text("Endereço: ${role.endereco}", style = MaterialTheme.typography.bodyLarge)
            }
            if (viewModel.mensagemEntrada.isNotBlank()) {
                Text(
                    text = viewModel.mensagemEntrada,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onVoltarMeusRoles,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.entrandoNoRole
                ) {
                    Text("Voltar para Meus rolês")
                }
            }
            Button(
                onClick = { viewModel.entrarNoRole(role.roleId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.entrandoNoRole && !viewModel.entradaConcluida
            ) {
                if (viewModel.entrandoNoRole) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (viewModel.entrandoNoRole) "Entrando..." else "Entrar no rolê")
            }
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.entrandoNoRole
            ) {
                Text("Cancelar")
            }
        }
    }
}
