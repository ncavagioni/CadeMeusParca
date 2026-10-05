package br.edu.eep.cademeusparca.model

import com.google.firebase.Timestamp

data class LocalizacaoParticipante(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val precisao: Double? = null,
    val atualizadoEm: Timestamp? = null
)
