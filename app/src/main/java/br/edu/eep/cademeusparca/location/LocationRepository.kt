package br.edu.eep.cademeusparca.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken

class LocationRepository(context: Context) {
    private val context = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(this.context)
    private var locationCallback: LocationCallback? = null

    fun temPermissao(): Boolean {
        return temLocalizacaoPrecisa() || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun temLocalizacaoPrecisa(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun iniciarAtualizacoes(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (!temLocalizacaoPrecisa()) {
            onError("Habilite a localização precisa para compartilhar sua posição automaticamente.")
            return false
        }
        if (!localizacaoDoAparelhoAtivada()) {
            onError("A localização do aparelho está desligada. Ative-a e tente novamente.")
            return false
        }

        pararAtualizacoes()

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            INTERVALO_ATUALIZACAO_MS
        )
            .setMinUpdateIntervalMillis(INTERVALO_ATUALIZACAO_MS)
            .setMaxUpdateDelayMillis(0L)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (locationCallback !== this) return
                if (!temLocalizacaoPrecisa()) {
                    pararAtualizacoes()
                    onError("Habilite a localização precisa para compartilhar sua posição automaticamente.")
                    return
                }
                val location = result.lastLocation
                if (location == null || !localizacaoValida(location)) {
                    Log.d(TAG, "LOCATION_CALLBACK_INVALID timestamp=${System.currentTimeMillis()}")
                    return
                }
                Log.d(
                    TAG,
                    "LOCATION_CALLBACK timestamp=${System.currentTimeMillis()} " +
                        "elapsedRealtimeMs=${SystemClock.elapsedRealtime()} " +
                        "callbackId=${System.identityHashCode(this)} " +
                        "locationElapsedMs=${location.elapsedRealtimeNanos / 1_000_000} " +
                        "accuracy=${if (location.hasAccuracy()) location.accuracy else null}"
                )
                onLocation(Location(location))
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (locationCallback !== this) return
                Log.d(
                    TAG,
                    "LOCATION_AVAILABILITY timestamp=${System.currentTimeMillis()} " +
                        "available=${availability.isLocationAvailable}"
                )
            }
        }
        locationCallback = callback

        return try {
            Log.d(
                TAG,
                "LOCATION_UPDATES_REQUESTED timestamp=${System.currentTimeMillis()} " +
                    "callbackId=${System.identityHashCode(callback)} " +
                    "priority=${request.priority} intervalMillis=${request.intervalMillis} " +
                    "minUpdateIntervalMillis=${request.minUpdateIntervalMillis} " +
                    "configuredMaxUpdateDelayMillis=0 " +
                    "maxUpdateDelayMillis=${request.maxUpdateDelayMillis} " +
                    "granularity=${request.granularity} batched=${request.isBatched} " +
                    "waitForAccurateLocation=${request.isWaitForAccurateLocation}"
            )
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnSuccessListener {
                    if (locationCallback === callback) {
                        Log.d(
                            TAG,
                            "LOCATION_UPDATES_REGISTERED timestamp=${System.currentTimeMillis()} " +
                                "callbackId=${System.identityHashCode(callback)}"
                        )
                    } else {
                        // A tela pode ter pausado enquanto o registro estava pendente.
                        removerCallback(callback)
                    }
                }
                .addOnFailureListener { erro ->
                    if (locationCallback === callback) {
                        pararAtualizacoes()
                        Log.w(
                            TAG,
                            "LOCATION_UPDATES_FAILED timestamp=${System.currentTimeMillis()} " +
                                "type=${erro.javaClass.simpleName}"
                        )
                        onError("Não foi possível iniciar a atualização automática da posição.")
                    }
                }
            true
        } catch (_: SecurityException) {
            pararAtualizacoes()
            Log.w(TAG, "LOCATION_PERMISSION_REMOVED timestamp=${System.currentTimeMillis()}")
            onError("A permissão de localização foi removida. Permita o acesso e tente novamente.")
            false
        }
    }

    fun pararAtualizacoes() {
        val callback = locationCallback ?: return
        locationCallback = null
        removerCallback(callback)
    }

    private fun removerCallback(callback: LocationCallback) {
        Log.d(
            TAG,
            "LOCATION_UPDATES_REMOVE_REQUESTED timestamp=${System.currentTimeMillis()} " +
                "callbackId=${System.identityHashCode(callback)}"
        )
        client.removeLocationUpdates(callback)
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "LOCATION_UPDATES_REMOVED timestamp=${System.currentTimeMillis()} " +
                        "callbackId=${System.identityHashCode(callback)}"
                )
            }
            .addOnFailureListener {
                Log.w(
                    TAG,
                    "LOCATION_UPDATES_REMOVE_FAILED timestamp=${System.currentTimeMillis()} " +
                        "callbackId=${System.identityHashCode(callback)} type=${it.javaClass.simpleName}"
                )
            }
    }

    fun obterLocalizacaoAtual(
        cancellationToken: CancellationToken,
        onResult: (Location?, String?) -> Unit
    ) {
        if (!temPermissao()) {
            onResult(null, "Permita a localização durante o uso para mostrar sua posição no mapa.")
            return
        }
        if (!localizacaoDoAparelhoAtivada()) {
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
                    } else if (location == null || !localizacaoValida(location)) {
                        onResult(null, "Localização temporariamente indisponível. Tente novamente.")
                    } else {
                        onResult(location, null)
                    }
                }
                .addOnFailureListener {
                    onResult(null, "Não foi possível obter sua localização. Tente novamente.")
                }
        } catch (erro: SecurityException) {
            onResult(
                null,
                "A permissão de localização foi removida. Permita o acesso e tente novamente."
            )
        }
    }

    private fun localizacaoDoAparelhoAtivada(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun localizacaoValida(location: Location): Boolean {
        return location.latitude.isFinite() &&
            location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 &&
            location.longitude in -180.0..180.0 &&
            (!location.hasAccuracy() || (location.accuracy.isFinite() && location.accuracy >= 0))
    }

    private companion object {
        const val TAG = "LocationRepository"
        const val INTERVALO_ATUALIZACAO_MS = 10_000L
    }
}
