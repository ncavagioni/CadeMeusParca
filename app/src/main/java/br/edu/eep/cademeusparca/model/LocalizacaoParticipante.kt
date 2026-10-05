package br.edu.eep.cademeusparca.model

import com.google.firebase.Timestamp

data class LocalizacaoParticipante(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val precisao: Double? = null,
    val atualizadoEm: Timestamp? = null
) {
    companion object {
        // Não usar os valores padrão do model como coordenadas de documentos incompletos.
        fun deDocumento(
            documentoId: String,
            dados: Map<String, Any?>
        ): LocalizacaoParticipante? {
            val userId = dados["userId"] as? String ?: return null
            if (userId.isBlank() || userId != documentoId) return null
            val latitude = (dados["latitude"] as? Number)?.toDouble() ?: return null
            val longitude = (dados["longitude"] as? Number)?.toDouble() ?: return null
            if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
                !longitude.isFinite() || longitude !in -180.0..180.0
            ) return null

            val valorPrecisao = dados["precisao"]
            val precisao = if (valorPrecisao == null) null
            else (valorPrecisao as? Number)?.toDouble() ?: return null
            if (precisao != null && (!precisao.isFinite() || precisao < 0)) return null

            val timestamp = dados["atualizadoEm"]
            if (timestamp != null && timestamp !is Timestamp) return null
            return LocalizacaoParticipante(
                userId, latitude, longitude, precisao, timestamp as? Timestamp
            )
        }
    }
}
