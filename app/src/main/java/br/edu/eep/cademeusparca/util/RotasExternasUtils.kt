package br.edu.eep.cademeusparca.util

import br.edu.eep.cademeusparca.location.DistanciaUtils

object RotasExternasUtils {
    fun uriMaps(latitude: Double, longitude: Double): String? =
        if (DistanciaUtils.coordenadasValidas(latitude, longitude)) {
            "https://www.google.com/maps/dir/?api=1&destination=$latitude%2C$longitude"
        } else null
}
