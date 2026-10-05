package br.edu.eep.cademeusparca.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.edu.eep.cademeusparca.R
import br.edu.eep.cademeusparca.viewmodel.MapaRoleViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaRoleScreen(
    roleId: String,
    viewModel: MapaRoleViewModel,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferencias = remember(context) {
        context.getSharedPreferences("permissao_mapa", Context.MODE_PRIVATE)
    }
    // Estados apenas para apresentação: a permissão é reavaliada antes de cada acesso.
    var permissaoConcedida by remember { mutableStateOf(temPermissaoLocalizacao(context)) }
    var permissaoBloqueada by remember { mutableStateOf(false) }
    var localizacaoPrecisa by remember { mutableStateOf(temLocalizacaoPrecisa(context)) }
    var mapaCarregado by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val chaveConfigurada = booleanResource(R.bool.maps_key_configured)

    fun atualizarPermissao() {
        permissaoConcedida = temPermissaoLocalizacao(context)
        localizacaoPrecisa = temLocalizacaoPrecisa(context)
        val activity = context.encontrarActivity()
        val rationale = activity != null && (
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        permissaoBloqueada = !permissaoConcedida &&
            preferencias.getBoolean("solicitada", false) && !rationale
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        atualizarPermissao()
        viewModel.verificarLocalizacaoAoAbrir()
    }

    LaunchedEffect(roleId) {
        viewModel.carregarRole(roleId)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> {
                    atualizarPermissao()
                    viewModel.verificarLocalizacaoAoAbrir()
                }
                Lifecycle.Event.ON_STOP -> viewModel.cancelarBuscaLocalizacao()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelarBuscaLocalizacao()
        }
    }

    val localizacao = viewModel.localizacao
    LaunchedEffect(localizacao, mapaCarregado) {
        if (mapaCarregado && localizacao != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(localizacao.latitude, localizacao.longitude), 16f
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.nomeRole.ifBlank { "Mapa do rolê" }) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (viewModel.mensagemRole.isNotBlank()) {
                Text(
                    text = viewModel.mensagemRole,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (chaveConfigurada) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            mapToolbarEnabled = false,
                            myLocationButtonEnabled = false
                        ),
                        onMapLoaded = { mapaCarregado = true }
                    ) {
                        localizacao?.let {
                            Marker(
                                state = rememberUpdatedMarkerState(
                                    position = LatLng(it.latitude, it.longitude)
                                ),
                                title = "Você"
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Mapa indisponível: configuração pendente.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (viewModel.carregandoLocalizacao || viewModel.salvandoLocalizacao) {
                    CircularProgressIndicator()
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!permissaoConcedida) {
                    Text(
                        "Permita a localização durante o uso para mostrar sua posição no mapa.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            atualizarPermissao()
                            when {
                                permissaoConcedida -> viewModel.obterLocalizacaoAtual()
                                permissaoBloqueada -> {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                }
                                else -> {
                                    preferencias.edit().putBoolean("solicitada", true).apply()
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (permissaoBloqueada) "Abrir configurações"
                            else "Permitir localização"
                        )
                    }
                } else {
                    if (viewModel.carregandoLocalizacao) {
                        Text("Obtendo sua localização...", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (viewModel.salvandoLocalizacao) {
                        Text(
                            "Salvando sua posição... Aguardando confirmação do servidor.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!localizacaoPrecisa) {
                        Text(
                            "Habilite a localização precisa para compartilhar sua posição.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            },
                            enabled = !viewModel.carregandoLocalizacao && !viewModel.salvandoLocalizacao,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Habilitar localização precisa")
                        }
                    }
                    if (viewModel.mensagemLocalizacao.isNotBlank()) {
                        Text(
                            viewModel.mensagemLocalizacao,
                            color = if (viewModel.posicaoAtualizada) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            atualizarPermissao()
                            viewModel.atualizarMinhaPosicao(roleId)
                        },
                        enabled = !viewModel.carregandoLocalizacao && !viewModel.salvandoLocalizacao,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Atualizar minha posição")
                    }
                }
            }
        }
    }
}

private fun temLocalizacaoPrecisa(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun temPermissaoLocalizacao(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.encontrarActivity(): Activity? {
    var atual = this
    while (atual is ContextWrapper) {
        if (atual is Activity) return atual
        atual = atual.baseContext
    }
    return atual as? Activity
}
