package br.edu.eep.cademeusparca.repository

import br.edu.eep.cademeusparca.model.EdicaoRole
import br.edu.eep.cademeusparca.model.Role

/** Contrato restrito à edição, também usado nos testes sem acesso ao Firebase. */
interface EdicaoRoleRepository {
    val userIdAtual: String?
    fun buscarRolePorId(roleId: String, onResult: (Role?, String?) -> Unit)
    fun atualizarDadosRole(roleId: String, edicao: EdicaoRole, onResult: (Boolean, String?) -> Unit)
}
