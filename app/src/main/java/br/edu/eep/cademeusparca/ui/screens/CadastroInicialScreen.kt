package br.edu.eep.cademeusparca.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.viewmodel.UsuarioViewModel

@Composable
fun CadastroInicialScreen(
    viewModel: UsuarioViewModel,
    onCadastroConcluido: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crie seu perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Como seus parças vão te encontrar por aqui.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.parcaname,
            onValueChange = viewModel::atualizarParcaname,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Parcaname *") },
            supportingText = { Text("Obrigatório") },
            singleLine = true,
            enabled = !viewModel.carregando,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.telefone,
            onValueChange = viewModel::atualizarTelefone,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Telefone (opcional)") },
            singleLine = true,
            enabled = !viewModel.carregando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.contatoEmergencia,
            onValueChange = viewModel::atualizarContatoEmergencia,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contato de emergência (opcional)") },
            singleLine = true,
            enabled = !viewModel.carregando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.mensagem.isNotBlank()) {
            Text(
                text = viewModel.mensagem,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                viewModel.salvarPerfil { sucesso ->
                    if (sucesso) onCadastroConcluido()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.carregando
        ) {
            if (viewModel.carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Continuar")
        }
    }
}
