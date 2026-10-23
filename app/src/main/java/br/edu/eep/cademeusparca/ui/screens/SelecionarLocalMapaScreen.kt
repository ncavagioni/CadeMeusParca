package br.edu.eep.cademeusparca.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.unit.dp
import br.edu.eep.cademeusparca.R
import br.edu.eep.cademeusparca.model.PontoLocal
import br.edu.eep.cademeusparca.viewmodel.SelecionarLocalMapaViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelecionarLocalMapaScreen(
    ponto: PontoLocal?,
    viewModel: SelecionarLocalMapaViewModel,
    onSelecionar: (Double, Double) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    BackHandler(onBack = onCancelar)
    val context = LocalContext.current
    val chaveConfigurada = booleanResource(R.bool.maps_key_configured)
    val servicosDisponiveis = remember(context) {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            ConnectionResult.SUCCESS
    }
    val mapaDisponivel = chaveConfigurada && servicosDisponiveis
    var tentativa by remember { mutableIntStateOf(0) }
    var mapaCarregado by remember(tentativa) { mutableStateOf(false) }
    var carregamentoDemorado by remember(tentativa) { mutableStateOf(false) }
    // Visão geral do Brasil é somente referência de câmera; nunca vira um ponto selecionado.
    val pontoInicial = remember { ponto }
    val camera = rememberCameraPositionState {
        position = pontoInicial?.let {
            CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
        } ?: CameraPosition.fromLatLngZoom(LatLng(-14.235, -51.9253), 3f)
    }
    var interagiu by remember { mutableStateOf(false) }
    LaunchedEffect(camera) {
        snapshotFlow { camera.cameraMoveStartedReason }.collect {
            if (it == CameraMoveStartedReason.GESTURE) interagiu = true
        }
    }
    DisposableEffect(viewModel, mapaDisponivel) {
        if (mapaDisponivel && pontoInicial == null) viewModel.buscarPosicaoInicial()
        onDispose { viewModel.cancelarBusca() }
    }
    LaunchedEffect(viewModel.posicaoAtual) {
        val atual = viewModel.posicaoAtual
        if (pontoInicial == null && ponto == null && !interagiu && atual != null) {
            camera.position = CameraPosition.fromLatLngZoom(
                LatLng(atual.latitude, atual.longitude), 16f
            )
        }
    }
    LaunchedEffect(tentativa, mapaCarregado, mapaDisponivel) {
        if (mapaDisponivel && !mapaCarregado) {
            delay(15_000)
            carregamentoDemorado = true
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Selecionar local no mapa") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Toque no mapa para escolher ou mudar o ponto. O endereço continua manual.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (mapaDisponivel) {
                    key(tentativa) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = camera,
                            uiSettings = MapUiSettings(
                                mapToolbarEnabled = false,
                                myLocationButtonEnabled = false
                            ),
                            onMapLoaded = {
                                mapaCarregado = true
                                carregamentoDemorado = false
                            },
                            onMapClick = {
                                if (mapaCarregado) {
                                    interagiu = true
                                    onSelecionar(it.latitude, it.longitude)
                                }
                            },
                            onPOIClick = {
                                if (mapaCarregado) {
                                    interagiu = true
                                    onSelecionar(it.latLng.latitude, it.latLng.longitude)
                                }
                            }
                        ) {
                            ponto?.let {
                                Marker(
                                    state = rememberUpdatedMarkerState(
                                        position = LatLng(it.latitude, it.longitude)
                                    ),
                                    title = "Local selecionado",
                                    snippet = "${it.latitude}, ${it.longitude}"
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        if (!chaveConfigurada) "Mapa indisponível: chave do Google Maps não configurada."
                        else "Mapa indisponível: verifique o Google Play Services do aparelho.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (carregamentoDemorado) {
                    Text(
                        "O mapa ainda não carregou. Verifique a internet e a configuração do Maps. " +
                            "Você pode cancelar e manter o preenchimento manual.",
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { tentativa++ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tentar carregar mapa novamente")
                    }
                } else if (mapaDisponivel && !mapaCarregado) {
                    Text("Carregando mapa...")
                }
                ponto?.let {
                    Text("Ponto selecionado: ${it.latitude}, ${it.longitude}")
                }
                Button(
                    onClick = onConfirmar,
                    enabled = mapaCarregado && ponto != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirmar local") }
                OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar")
                }
            }
        }
    }
}
