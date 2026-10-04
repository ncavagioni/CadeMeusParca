package br.edu.eep.cademeusparca.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel

@Composable
fun MeusRolesScreen(
    viewModel: RoleViewModel,
    onCriarRoleClick: () -> Unit,
    onEntrarRoleClick: () -> Unit,
    onRoleClick: (String) -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Meus rolês",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Os rolês dos quais você participa aparecerão nesta tela.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    viewModel.carregandoListagem -> {
                        CircularProgressIndicator()
                    }

                    viewModel.mensagemListagem.isNotBlank() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = viewModel.mensagemListagem,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = viewModel::buscarRolesDoUsuario) {
                                Text("Tentar novamente")
                            }
                        }
                    }

                    viewModel.roles.isEmpty() -> {
                        Text(
                            text = "Você ainda não participa de nenhum rolê.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.roles, key = { it.roleId }) { role ->
                                Card(
                                    onClick = { onRoleClick(role.roleId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = role.nome,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Código: ${role.codigo}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (role.nomeLocal.isNotBlank()) {
                                            Text(
                                                text = role.nomeLocal,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Text(
                                            text = if (role.status == "ativo") "Ativo" else role.status,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCriarRoleClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Criar um rolê")
                }

                OutlinedButton(
                    onClick = onEntrarRoleClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Entrar em um rolê")
                }
            }
        }
    }
}
