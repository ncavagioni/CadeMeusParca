package br.edu.eep.cademeusparca.location

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DistanciaUtils {
    private const val RAIO_MEDIO_TERRA_METROS = 6_371_000.0

    fun calcularMetros(
        latitudeOrigem: Double,
        longitudeOrigem: Double,
        latitudeDestino: Double,
        longitudeDestino: Double
    ): Double? {
        if (!coordenadasValidas(latitudeOrigem, longitudeOrigem) ||
            !coordenadasValidas(latitudeDestino, longitudeDestino)
        ) return null

        val latitude1 = Math.toRadians(latitudeOrigem)
        val latitude2 = Math.toRadians(latitudeDestino)
        val diferencaLatitude = Math.toRadians(latitudeDestino - latitudeOrigem)
        val diferencaLongitude = Math.toRadians(longitudeDestino - longitudeOrigem)

        val haversine = sin(diferencaLatitude / 2).let { it * it } +
            cos(latitude1) * cos(latitude2) *
            sin(diferencaLongitude / 2).let { it * it }
        val anguloCentral = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

        return RAIO_MEDIO_TERRA_METROS * anguloCentral
    }

    fun formatar(metros: Double, locale: Locale = Locale.getDefault()): String? {
        if (!metros.isFinite() || metros < 0) return null
        if (metros < 1_000) return "${metros.roundToInt()} m"

        val formato = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
            isGroupingUsed = false
        }
        val quilometros = BigDecimal.valueOf(metros)
            .divide(BigDecimal.valueOf(1_000))
            .setScale(1, RoundingMode.HALF_UP)
        return "${formato.format(quilometros)} km"
    }

    fun coordenadasValidas(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
}
