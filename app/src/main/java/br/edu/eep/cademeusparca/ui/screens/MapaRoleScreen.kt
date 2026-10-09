package br.edu.eep.cademeusparca.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import br.edu.eep.cademeusparca.ui.components.ParticipanteMapMarker
import br.edu.eep.cademeusparca.viewmodel.MapaRoleViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaRoleScreen(
    roleId: String,
    viewModel: MapaRoleViewModel,
    onVoltar: () -> Unit,
    onAbrirParcas: () -> Unit,
    onAbrirRole: () -> Unit,
    onAbrirParca: (String) -> Unit
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
    var notificacoesPermitidas by remember { mutableStateOf(temPermissaoNotificacoes(context)) }

    fun iniciarCompartilhamentoVisivel() {
        (context.encontrarActivity() as? ComponentActivity)?.let {
            viewModel.iniciarCompartilhamento(it, roleId)
        }
    }
    // Selecionada uma única vez por entrada; atualizações posteriores movem só os marcadores.
    var posicaoInicialCamera by remember(roleId) { mutableStateOf<LatLng?>(null) }
    val chaveConfigurada = booleanResource(R.bool.maps_key_configured)

    fun atualizarPermissao() {
        permissaoConcedida = temPermissaoLocalizacao(context)
        localizacaoPrecisa = temLocalizacaoPrecisa(context)
        notificacoesPermitidas = temPermissaoNotificacoes(context)
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
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.verificarLocalizacaoAoAbrir()
            viewModel.iniciarObservacao(roleId)
            iniciarCompartilhamentoVisivel()
        }
    }

    // Solicitação separada, somente por ação explícita; negar não interrompe o serviço.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificacoesPermitidas = temPermissaoNotificacoes(context)
        if (notificacoesPermitidas &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) iniciarCompartilhamentoVisivel()
    }

    LaunchedEffect(roleId) {
        viewModel.carregarRole(roleId)
    }

    DisposableEffect(lifecycleOwner, viewModel, roleId) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> {
                    atualizarPermissao()
                    viewModel.verificarLocalizacaoAoAbrir()
                    viewModel.iniciarObservacao(roleId)
                    iniciarCompartilhamentoVisivel()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.pausarLocalizacao()
                Lifecycle.Event.ON_STOP -> viewModel.pararObservacao()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            atualizarPermissao()
            viewModel.verificarLocalizacaoAoAbrir()
            viewModel.iniciarObservacao(roleId)
            iniciarCompartilhamentoVisivel()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pararObservacao()
            viewModel.cancelarBuscaLocalizacao()
        }
    }

    val posicaoAndroid = viewModel.localizacao?.let {
        posicaoUtilParaCamera(it.latitude, it.longitude)
    }
    val propriaSalva = viewModel.localizacoesParticipantes.firstOrNull {
        viewModel.userIdAtual.isNotBlank() && it.userId == viewModel.userIdAtual
    }
    val posicaoPreferida = posicaoAndroid ?: propriaSalva?.let {
        posicaoUtilParaCamera(it.latitude, it.longitude)
    }
    LaunchedEffect(roleId, posicaoPreferida) {
        if (posicaoInicialCamera == null && posicaoPreferida != null) {
            posicaoInicialCamera = posicaoPreferida
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
            if (viewModel.carregandoParticipantes) {
                Text("Carregando posições dos parças...", modifier = Modifier.padding(12.dp))
            }
            if (viewModel.mensagemLeitura.isNotBlank()) {
                Text(
                    viewModel.mensagemLeitura,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (viewModel.mensagemPerfis.isNotBlank()) {
                Text(
                    viewModel.mensagemPerfis,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (chaveConfigurada) {
                    val posicaoInicial = posicaoInicialCamera
                    if (posicaoInicial == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Obtendo sua posição...")
                        }
                    } else {
                        key(roleId) {
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(posicaoInicial, 16f)
                            }
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    mapToolbarEnabled = false,
                                    myLocationButtonEnabled = false
                                )
                            ) {
                                viewModel.localizacoesNoMapa.forEach { participante ->
                                    key(participante.userId) {
                                        val titulo = viewModel.tituloMarcador(participante.userId)
                                        val proprioUsuario = participante.userId == viewModel.userIdAtual
                                        val recente = viewModel.localizacaoRecente(participante)
                                        val distancia = viewModel.distanciaFormatadaAte(participante)
                                        val vistoPorUltimo = viewModel.vistoPorUltimo(participante)
                                        val atualizadoEm = participante.atualizadoEm?.let {
                                            val prefixo = if (proprioUsuario || recente) {
                                                "Atualizado em "
                                            } else "Visto por último: "
                                            prefixo + DateFormat.getDateTimeInstance(
                                                DateFormat.SHORT, DateFormat.SHORT
                                            ).format(it.toDate())
                                        }
                                        val detalhes = listOfNotNull(
                                            distancia?.let { "Distância: $it" },
                                            vistoPorUltimo,
                                            atualizadoEm,
                                            "Toque para ver detalhes".takeUnless { proprioUsuario }
                                        ).joinToString("\n").ifBlank { null }
                                        MarkerComposable(
                                            titulo,
                                            proprioUsuario,
                                            distancia.orEmpty(),
                                            vistoPorUltimo.orEmpty(),
                                            recente,
                                            state = rememberUpdatedMarkerState(
                                                position = LatLng(participante.latitude, participante.longitude)
                                            ),
                                            title = titulo,
                                            snippet = detalhes,
                                            onInfoWindowClick = {
                                                if (!proprioUsuario) onAbrirParca(participante.userId)
                                            }
                                        ) {
                                            ParticipanteMapMarker(
                                                titulo = titulo,
                                                proprioUsuario = proprioUsuario,
                                                distancia = distancia,
                                                vistoPorUltimo = vistoPorUltimo,
                                                localizacaoRecente = recente
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Mapa indisponível: configuração pendente.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (posicaoInicialCamera != null &&
                    (viewModel.carregandoLocalizacao || viewModel.salvandoLocalizacao)
                ) {
                    CircularProgressIndicator()
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    viewModel.mensagemCompartilhamento,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onAbrirParcas, modifier = Modifier.weight(1f)) {
                        Text("Parças")
                    }
                    Button(onClick = onAbrirRole, modifier = Modifier.weight(1f)) {
                        Text("Rolê")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !notificacoesPermitidas && localizacaoPrecisa
                ) {
                    Text(
                        "Permita notificações para ver o compartilhamento e a ação de parar. " +
                            "O serviço pode continuar mesmo sem essa permissão.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            val activity = context.encontrarActivity()
                            val bloqueada = preferencias.getBoolean("notificacoes_solicitada", false) &&
                                activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity, Manifest.permission.POST_NOTIFICATIONS
                                )
                            if (bloqueada) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null))
                                )
                            } else {
                                preferencias.edit().putBoolean("notificacoes_solicitada", true).apply()
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Permitir notificações") }
                }
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
                    // Ação manual temporária de apoio aos testes.
                    TextButton(
                        onClick = {
                            atualizarPermissao()
                            viewModel.atualizarMinhaPosicao(roleId)
                        },
                        enabled = !viewModel.carregandoLocalizacao && !viewModel.salvandoLocalizacao,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Atualizar posição")
                    }
                }
            }
        }
    }
}

// (0,0) não é usado como centro inicial útil; os dados de localização não são alterados.
private fun posicaoUtilParaCamera(latitude: Double, longitude: Double): LatLng? {
    if (!latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0 ||
        (latitude == 0.0 && longitude == 0.0)
    ) return null
    return LatLng(latitude, longitude)
}

private fun temPermissaoNotificacoes(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

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
