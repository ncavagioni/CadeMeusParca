package br.edu.eep.cademeusparca.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken

class LocationRepository(context: Context) {
    private val context = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(this.context)

    fun temPermissao(): Boolean {
        return temLocalizacaoPrecisa() || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun temLocalizacaoPrecisa(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun obterLocalizacaoAtual(
        cancellationToken: CancellationToken,
        onResult: (Location?, String?) -> Unit
    ) {
        if (!temPermissao()) {
            onResult(null, "Permita a localização durante o uso para mostrar sua posição no mapa.")
            return
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            onResult(null, "A localização do aparelho está desligada. Ative-a e tente novamente.")
            return
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(
                if (temLocalizacaoPrecisa()) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            )
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(10_000)
            .setDurationMillis(20_000)
            .build()

        try {
            client.getCurrentLocation(request, cancellationToken)
                .addOnSuccessListener { location ->
                    if (!temPermissao()) {
                        onResult(null, "A permissão de localização foi removida.")
                    } else if (location == null ||
                        !location.latitude.isFinite() || !location.longitude.isFinite() ||
                        location.latitude !in -90.0..90.0 ||
                        location.longitude !in -180.0..180.0
                    ) {
                        onResult(null, "Localização temporariamente indisponível. Tente novamente.")
                    } else {
                        onResult(location, null)
                    }
                }
                .addOnFailureListener {
                    onResult(null, "Não foi possível obter sua localização. Tente novamente.")
                }
        } catch (erro: SecurityException) {
            onResult(null, "A permissão de localização foi removida. Permita o acesso e tente novamente.")
        }
    }
}
