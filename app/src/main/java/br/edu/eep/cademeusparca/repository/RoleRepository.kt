package br.edu.eep.cademeusparca.repository

import br.edu.eep.cademeusparca.model.ParticipanteRole
import br.edu.eep.cademeusparca.model.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import java.security.SecureRandom

class RoleRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val random = SecureRandom()

    fun criarRole(
        nome: String,
        nomeLocal: String,
        endereco: String,
        onResult: (Role?, String?) -> Unit
    ) {
        if (nome.isBlank()) {
            onResult(null, "Informe o nome do rolê.")
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }

        buscarCodigoDisponivel(1) { codigo, erro ->
            if (codigo == null) {
                onResult(null, erro)
            } else {
                val documento = firestore.collection("roles").document()
                val role = Role(
                    roleId = documento.id,
                    nome = nome.trim(),
                    nomeLocal = nomeLocal.trim(),
                    endereco = endereco.trim(),
                    codigo = codigo,
                    adminId = uid
                )
                val participante = ParticipanteRole(userId = uid, papel = "admin")
                val batch = firestore.batch()
                batch.set(documento, role)
                batch.set(documento.collection("participantes").document(uid), participante)
                batch.commit()
                    .addOnSuccessListener { onResult(role, null) }
                    .addOnFailureListener { onResult(null, mensagemErro(it)) }
            }
        }
    }

    private fun buscarCodigoDisponivel(
        tentativa: Int,
        onResult: (String?, String?) -> Unit
    ) {
        if (tentativa > MAX_TENTATIVAS) {
            onResult(null, "Não foi possível gerar um código disponível. Tente novamente.")
            return
        }

        val codigo = buildString {
            repeat(6) {
                append(CARACTERES[random.nextInt(CARACTERES.length)])
            }
        }

        // Consultar o servidor evita considerar um código livre só por ausência no cache.
        firestore.collection("roles")
            .whereEqualTo("codigo", codigo)
            .limit(1)
            .get(Source.SERVER)
            .addOnSuccessListener { resultado ->
                if (resultado.isEmpty) {
                    onResult(codigo, null)
                } else {
                    buscarCodigoDisponivel(tentativa + 1, onResult)
                }
            }
            .addOnFailureListener { onResult(null, mensagemErro(it)) }
    }

    private fun mensagemErro(erro: Exception): String {
        return when ((erro as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Sem permissão para criar o rolê. Verifique as regras do Firestore."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Sua autenticação não está disponível. Abra o aplicativo novamente."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Não foi possível conectar ao Firestore. Verifique sua internet e tente novamente."
            else -> "Não foi possível criar o rolê. Tente novamente."
        }
    }

    private companion object {
        const val CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        const val MAX_TENTATIVAS = 5
    }
}
