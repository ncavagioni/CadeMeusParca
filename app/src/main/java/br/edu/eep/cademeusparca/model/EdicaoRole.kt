package br.edu.eep.cademeusparca.model

fun erroAutorizacaoEdicaoRole(userId: String?, adminId: String?, status: String?): String? = when {
    userId.isNullOrBlank() -> "Sua autenticação não está disponível. Reabra o aplicativo."
    userId != adminId -> "Apenas o administrador pode editar este rolê."
    status != "ativo" -> "Somente rolês ativos podem ser editados."
    else -> null
}

fun podeEditarDadosRole(userId: String?, adminId: String?, status: String?): Boolean =
    erroAutorizacaoEdicaoRole(userId, adminId, status) == null

/** Somente os cinco campos editáveis; identidade e subcoleções não fazem parte da atualização. */
data class EdicaoRole(
    val nome: String,
    val nomeLocal: String = "",
    val endereco: String = "",
    val latitudeLocal: Double? = null,
    val longitudeLocal: Double? = null
) {
    fun erroValidacao(): String? = when {
        nome.isBlank() -> "Informe o nome do rolê."
        !PontoLocal.coordenadasOpcionaisValidas(latitudeLocal, longitudeLocal) ->
            "Selecione um ponto válido no mapa ou limpe a seleção."
        else -> null
    }

    fun paraAtualizacao(): Map<String, Any?> {
        require(erroValidacao() == null)
        return mapOf(
            "nome" to nome.trim(),
            "nomeLocal" to nomeLocal.trim(),
            "endereco" to endereco.trim(),
            "latitudeLocal" to latitudeLocal,
            "longitudeLocal" to longitudeLocal
        )
    }
}
