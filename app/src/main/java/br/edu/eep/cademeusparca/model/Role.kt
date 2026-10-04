package br.edu.eep.cademeusparca.model

data class Role(
    val roleId: String = "",
    val nome: String = "",
    val fotoUrl: String = "",
    val nomeLocal: String = "",
    val endereco: String = "",
    val latitudeLocal: Double? = null,
    val longitudeLocal: Double? = null,
    val codigo: String = "",
    val adminId: String = "",
    val status: String = "ativo"
)
