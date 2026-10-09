package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import br.edu.eep.cademeusparca.model.Role
import java.util.Locale

data class DetalhesRoleUi(
    val nome: String,
    val status: String,
    val codigo: String,
    val nomeLocal: String?,
    val endereco: String?,
    val administrador: String,
    val participantes: String
)

fun parametrosDetalhesRoleValidos(roleId: String, uid: String?): Boolean =
    !uid.isNullOrBlank() && roleId.isNotBlank() && '/' !in roleId && roleId != "." && roleId != ".."

fun formatarStatusRole(status: String?): String =
    status?.trim()?.takeIf { it.isNotEmpty() }
        ?.replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) } ?: "Não informado"

fun formatarQuantidadeParcas(quantidade: Int?): String = when {
    quantidade == null -> "Carregando participantes..."
    quantidade < 0 -> "Quantidade indisponível"
    quantidade == 1 -> "1 parça"
    else -> "$quantidade parças"
}

private fun textoOpcionalRole(texto: String?): String? = texto?.trim()?.takeIf { it.isNotEmpty() }

fun comporDetalhesRole(
    role: Role,
    userIdAtual: String,
    perfis: List<PerfilPublicoRole>,
    quantidadeParticipantes: Int?
): DetalhesRoleUi {
    val admin = perfis.firstOrNull { it.userId == role.adminId }
        ?.parcaname?.let(::textoOpcionalRole)
    return DetalhesRoleUi(
        nome = textoOpcionalRole(role.nome) ?: "Rolê",
        status = formatarStatusRole(role.status),
        codigo = role.codigo.orEmpty(),
        nomeLocal = textoOpcionalRole(role.nomeLocal),
        endereco = textoOpcionalRole(role.endereco),
        administrador = if (admin == null) "Nome indisponível"
            else admin + if (role.adminId == userIdAtual) " (Você)" else "",
        participantes = formatarQuantidadeParcas(quantidadeParticipantes)
    )
}
