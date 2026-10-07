package br.edu.eep.cademeusparca.model

data class PerfilPublicoRole(
    val userId: String = "",
    val parcaname: String = "",
    val telefone: String = ""
) {
    companion object {
        fun deDocumento(documentoId: String, dados: Map<String, Any?>): PerfilPublicoRole? {
            val uid = dados["userId"] as? String ?: return null
            val nome = dados["parcaname"] as? String ?: return null
            // Projeções antigas ainda não possuem telefone.
            val telefone = if ("telefone" in dados) dados["telefone"] as? String ?: return null else ""
            if (uid.isBlank() || uid != documentoId || nome.isBlank() ||
                nome.length > 100 || telefone.length > 30
            ) return null
            return PerfilPublicoRole(uid, nome, telefone)
        }
    }
}
