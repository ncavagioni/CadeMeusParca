package br.edu.eep.cademeusparca.model

/** Coordenadas geográficas válidas; nunca representam uma posição obtida por suposição. */
data class PontoLocal(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    companion object {
        fun deCoordenadas(latitude: Double?, longitude: Double?): PontoLocal? {
            if (latitude == null || longitude == null ||
                !latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) return null
            return PontoLocal(latitude, longitude)
        }

        fun coordenadasOpcionaisValidas(latitude: Double?, longitude: Double?): Boolean =
            (latitude == null && longitude == null) || deCoordenadas(latitude, longitude) != null
    }
}

/** Projeção de apresentação separada do modelo serializado pelo Firestore. */
data class MarcadorEvento(val ponto: PontoLocal, val titulo: String)

fun Role.marcadorEvento(): MarcadorEvento? =
    PontoLocal.deCoordenadas(latitudeLocal, longitudeLocal)?.let {
        MarcadorEvento(it, nomeLocal.ifBlank { nome })
    }
