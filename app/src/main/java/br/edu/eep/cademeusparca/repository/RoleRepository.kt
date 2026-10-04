package br.edu.eep.cademeusparca.repository

import android.util.Log
import br.edu.eep.cademeusparca.model.ParticipanteRole
import br.edu.eep.cademeusparca.model.Role
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    fun buscarRolesDoUsuario(onResult: (List<Role>?, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }

        firestore.collectionGroup("participantes")
            .whereEqualTo("userId", uid)
            .get(Source.SERVER)
            .addOnSuccessListener { participacoes ->
                val referencias = participacoes.documents.mapNotNull { participante ->
                    participante.reference.parent.parent?.takeIf { role ->
                        role.parent.path == "roles" && participante.id == uid
                    }
                }.distinctBy { it.path }

                if (referencias.isEmpty()) {
                    onResult(emptyList(), null)
                    return@addOnSuccessListener
                }

                val consultas = referencias.map { it.get(Source.SERVER) }
                Tasks.whenAllSuccess<DocumentSnapshot>(consultas)
                    .addOnSuccessListener { documentos ->
                        try {
                            val roles = documentos.mapNotNull { documento ->
                                if (documento.exists()) {
                                    documento.toObject(Role::class.java)
                                        ?.copy(roleId = documento.id)
                                } else {
                                    null
                                }
                            }
                            onResult(roles, null)
                        } catch (erro: RuntimeException) {
                            falharListagem(erro, onResult)
                        }
                    }
                    .addOnFailureListener { falharListagem(it, onResult) }
            }
            .addOnFailureListener { falharListagem(it, onResult) }
    }

    private fun falharListagem(
        erro: Exception,
        onResult: (List<Role>?, String?) -> Unit
    ) {
        // O log mantém o diagnóstico, inclusive o link de criação de índice do Firestore.
        Log.w("RoleRepository", "Erro ao carregar os rolês do usuário", erro)
        val mensagem = when ((erro as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Não foi possível carregar seus rolês. Verifique sua internet e tente novamente."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Sua autenticação não está disponível. Abra o aplicativo novamente."
            else -> "Não foi possível carregar seus rolês. Tente novamente."
        }
        onResult(null, mensagem)
    }

    fun buscarRolePorCodigo(codigo: String, onResult: (Role?, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }
        if (!codigo.matches(Regex("[A-Z0-9]{6}"))) {
            onResult(null, "Informe um código de 6 letras ou números.")
            return
        }

        firestore.collection("roles")
            .whereEqualTo("codigo", codigo)
            .limit(1)
            .get(Source.SERVER)
            .addOnSuccessListener { resultado ->
                val documento = resultado.documents.firstOrNull()
                val role = try {
                    documento?.toObject(Role::class.java)?.copy(roleId = documento.id)
                } catch (erro: RuntimeException) {
                    Log.w("RoleRepository", "Dados inválidos na busca por código", erro)
                    onResult(null, "Não foi possível carregar esse rolê. Tente novamente.")
                    return@addOnSuccessListener
                }

                if (documento == null || role == null || role.status != "ativo") {
                    onResult(null, "Rolê não encontrado.")
                    return@addOnSuccessListener
                }

                documento.reference.collection("participantes").document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { participante ->
                        if (participante.exists()) {
                            onResult(null, "Você já participa desse rolê.")
                        } else {
                            onResult(role, null)
                        }
                    }
                    .addOnFailureListener { onResult(null, mensagemErroEntrada(it)) }
            }
            .addOnFailureListener { onResult(null, mensagemErroEntrada(it)) }
    }

    fun entrarNoRole(roleId: String, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }
        if (roleId.isBlank() || roleId.contains('/')) {
            onResult(false, "Rolê não encontrado.")
            return
        }

        val roleRef = firestore.collection("roles").document(roleId)
        val participanteRef = roleRef.collection("participantes").document(uid)

        firestore.runTransaction { transacao ->
            val role = transacao.get(roleRef)
            val participante = transacao.get(participanteRef)

            when {
                participante.exists() -> ResultadoEntrada.JA_PARTICIPA
                !role.exists() || role.getString("status") != "ativo" ->
                    ResultadoEntrada.INDISPONIVEL
                else -> {
                    transacao.set(
                        participanteRef,
                        ParticipanteRole(userId = uid, papel = "participante")
                    )
                    ResultadoEntrada.ENTROU
                }
            }
        }
            .addOnSuccessListener { resultado ->
                when (resultado) {
                    ResultadoEntrada.ENTROU -> onResult(true, null)
                    ResultadoEntrada.JA_PARTICIPA ->
                        onResult(false, "Você já participa desse rolê.")
                    ResultadoEntrada.INDISPONIVEL ->
                        onResult(false, "Esse rolê não está mais disponível para entrada.")
                }
            }
            .addOnFailureListener { onResult(false, mensagemErroEntrada(it)) }
    }

    private fun mensagemErroEntrada(erro: Exception): String {
        Log.w("RoleRepository", "Erro no fluxo de entrada em rolê", erro)
        return when ((erro as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Não foi possível acessar esse rolê. Verifique as permissões e tente novamente."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Sua autenticação não está disponível. Abra o aplicativo novamente."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Não foi possível conectar ao Firestore. Verifique sua internet e tente novamente."
            else -> "Não foi possível concluir a operação. Tente novamente."
        }
    }

    private enum class ResultadoEntrada {
        ENTROU, JA_PARTICIPA, INDISPONIVEL
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
