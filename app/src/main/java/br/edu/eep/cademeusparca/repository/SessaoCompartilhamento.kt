package br.edu.eep.cademeusparca.repository

// Usada somente na thread principal. A geração invalida posições pendentes de sessões antigas.
internal class SessaoCompartilhamento {
    data class Destino(val roleId: String, val userId: String, val geracao: Long)

    private var geracao = 0L
    var destino: Destino? = null
        private set

    fun selecionar(roleId: String, userId: String): Destino {
        require(roleId.isNotBlank() && !roleId.contains('/'))
        require(userId.isNotBlank())
        destino?.takeIf { it.roleId == roleId && it.userId == userId }?.let { return it }
        return Destino(roleId, userId, ++geracao).also { destino = it }
    }

    fun aceita(pedido: Destino): Boolean = destino == pedido

    fun encerrar() {
        geracao++
        destino = null
    }
}
