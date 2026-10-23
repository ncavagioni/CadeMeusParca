package br.edu.eep.cademeusparca.repository

import android.util.Log
import br.edu.eep.cademeusparca.model.ParticipanteRole
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.model.PontoLocal
import br.edu.eep.cademeusparca.model.EdicaoRole
import br.edu.eep.cademeusparca.model.erroAutorizacaoEdicaoRole
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import java.security.SecureRandom

class RoleRepository : EdicaoRoleRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val random = SecureRandom()
    override val userIdAtual: String? get() = auth.currentUser?.uid

    override fun atualizarDadosRole(
        roleId: String,
        edicao: EdicaoRole,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = userIdAtual
        if (uid.isNullOrBlank()) {
            onResult(false, "Sua autenticação não está disponível. Reabra o aplicativo.")
            return
        }
        if (roleId.isBlank() || '/' in roleId || roleId == "." || roleId == "..") {
            onResult(false, "Rolê não encontrado.")
            return
        }
        edicao.erroValidacao()?.let {
            onResult(false, it)
            return
        }
        val campos = edicao.paraAtualizacao()
        val referencia = firestore.collection("roles").document(roleId)
        // A leitura é refeita pelo Firestore em caso de conflito, inclusive encerramento concorrente.
        firestore.runTransaction { transacao ->
            if (userIdAtual != uid) throw EdicaoRoleRecusada("Sua autenticação mudou. Reabra o rolê.")
            val atual = transacao.get(referencia)
            if (!atual.exists()) throw EdicaoRoleRecusada("Rolê não encontrado.")
            erroAutorizacaoEdicaoRole(uid, atual.getString("adminId"), atual.getString("status"))?.let {
                throw EdicaoRoleRecusada(it)
            }
            transacao.update(referencia, campos)
        }.addOnSuccessListener {
            if (userIdAtual == uid) onResult(true, null)
            else onResult(false, "Sua autenticação mudou. Reabra o rolê.")
        }.addOnFailureListener { erro ->
            Log.w("RoleRepository", "Erro ao editar dados do rolê", erro)
            val recusada = (erro as? EdicaoRoleRecusada) ?: (erro.cause as? EdicaoRoleRecusada)
            val mensagem = recusada?.message ?: when ((erro as? FirebaseFirestoreException)?.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Sem permissão para salvar este rolê. Verifique sua administração e as regras do Firestore."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Sua autenticação não está disponível. Reabra o aplicativo."
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Não foi possível salvar. Verifique sua internet e tente novamente."
                else -> "Não foi possível salvar as alterações. Tente novamente."
            }
            onResult(false, mensagem)
        }
    }

    private class EdicaoRoleRecusada(mensagem: String) : Exception(mensagem)

    fun observarParticipantesDoRole(
        roleId: String,
        onResult: (List<ParticipanteRole>?, String?) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null || roleId.isBlank() || '/' in roleId || roleId == "." || roleId == "..") {
            onResult(null, "Não foi possível identificar os participantes deste rolê.")
            return null
        }
        return firestore.collection("roles").document(roleId)
            .collection("participantes")
            .addSnapshotListener { snapshot, erro ->
                if (auth.currentUser?.uid != uid || erro != null) {
                    if (erro != null) Log.w("RoleRepository", "Erro ao observar participantes", erro)
                    onResult(null, "Não foi possível carregar os parças. Verifique sua participação e conexão.")
                } else if (snapshot != null) {
                    val participantes = snapshot.documents.mapNotNull { documento ->
                        val userId = documento.get("userId") as? String
                        if (userId.isNullOrBlank() || userId != documento.id) null
                        else ParticipanteRole(userId, documento.get("papel") as? String ?: "")
                    }
                    onResult(participantes, null)
                }
            }
    }

    fun observarParticipacao(
        roleId: String,
        userId: String,
        onResult: (Boolean, String?) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null || roleId.isBlank() || userId.isBlank() ||
            '/' in roleId || '/' in userId
        ) {
            onResult(false, "Não foi possível verificar a participação neste rolê.")
            return null
        }
        return firestore.collection("roles").document(roleId)
            .collection("participantes").document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { documento, erro ->
                if (auth.currentUser?.uid != uid || erro != null) {
                    onResult(false, "Não foi possível verificar a participação neste rolê.")
                } else if (documento != null && !documento.metadata.isFromCache) {
                    // Autorizar a tela após confirmação do servidor, não apenas por cache antigo.
                    val pertence = documento.exists() && documento.data?.get("userId") == userId
                    onResult(pertence, null)
                }
            }
    }

    fun criarRole(
        nome: String,
        nomeLocal: String,
        endereco: String,
        latitudeLocal: Double? = null,
        longitudeLocal: Double? = null,
        onResult: (Role?, String?) -> Unit
    ) {
        if (nome.isBlank()) {
            onResult(null, "Informe o nome do rolê.")
            return
        }
        if (!PontoLocal.coordenadasOpcionaisValidas(latitudeLocal, longitudeLocal)) {
            onResult(null, "Selecione um ponto válido no mapa ou limpe a seleção.")
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
                    latitudeLocal = latitudeLocal,
                    longitudeLocal = longitudeLocal,
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

    override fun buscarRolePorId(roleId: String, onResult: (Role?, String?) -> Unit) {
        if (auth.currentUser == null) {
            onResult(null, "Usuário não autenticado.")
            return
        }
        if (roleId.isBlank() || roleId.contains('/')) {
            onResult(null, "Rolê não encontrado.")
            return
        }

        firestore.collection("roles").document(roleId)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                val role = try {
                    documento.toObject(Role::class.java)?.copy(roleId = documento.id)
                } catch (erro: RuntimeException) {
                    Log.w("RoleRepository", "Dados inválidos no rolê", erro)
                    onResult(null, "Não foi possível carregar o nome do rolê.")
                    return@addOnSuccessListener
                }
                onResult(role, if (role == null) "Rolê não encontrado." else null)
            }
            .addOnFailureListener {
                onResult(null, "Não foi possível carregar o nome do rolê.")
            }
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

    fun sairDoRole(roleId: String, onResult: (Boolean, String?) -> Unit) {
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
        roleRef.get(Source.SERVER).addOnSuccessListener { doc ->
            val adminId = doc.getString("adminId")
            if (adminId == uid) {
                onResult(false, "O administrador não pode sair do rolê. Para finalizar, encerre o rolê.")
                return@addOnSuccessListener
            }
            val batch = firestore.batch()
            batch.delete(roleRef.collection("participantes").document(uid))
            batch.delete(roleRef.collection("perfisPublicos").document(uid))
            batch.delete(roleRef.collection("localizacoes").document(uid))
            batch.commit()
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { erro ->
                    Log.w("RoleRepository", "Erro ao sair do rolê", erro)
                    onResult(false, "Não foi possível sair do rolê. Tente novamente.")
                }
        }.addOnFailureListener { erro ->
            Log.w("RoleRepository", "Erro ao verificar o rolê para saída", erro)
            onResult(false, "Não foi possível acessar as informações do rolê. Tente novamente.")
        }
    }

    fun removerParticipante(
        roleId: String,
        targetUserId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }
        if (roleId.isBlank() || roleId.contains('/') || targetUserId.isBlank() || targetUserId.contains('/')) {
            onResult(false, "Parâmetros inválidos para remoção.")
            return
        }

        val roleRef = firestore.collection("roles").document(roleId)
        roleRef.get(Source.SERVER).addOnSuccessListener { doc ->
            val adminId = doc.getString("adminId")
            if (adminId != uid) {
                onResult(false, "Apenas o administrador pode remover participantes.")
                return@addOnSuccessListener
            }
            if (targetUserId == uid) {
                onResult(false, "O administrador não pode remover a si mesmo.")
                return@addOnSuccessListener
            }

            val batch = firestore.batch()
            batch.delete(roleRef.collection("participantes").document(targetUserId))
            batch.delete(roleRef.collection("perfisPublicos").document(targetUserId))
            batch.delete(roleRef.collection("localizacoes").document(targetUserId))
            batch.commit()
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { erro ->
                    Log.w("RoleRepository", "Erro ao remover participante", erro)
                    onResult(false, "Não foi possível remover o participante. Tente novamente.")
                }
        }.addOnFailureListener { erro ->
            Log.w("RoleRepository", "Erro ao verificar permissão do administrador", erro)
            onResult(false, "Não foi possível verificar as permissões. Tente novamente.")
        }
    }

    fun encerrarRole(roleId: String, onResult: (Boolean, String?) -> Unit) {
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
        roleRef.get(Source.SERVER).addOnSuccessListener { doc ->
            val adminId = doc.getString("adminId")
            if (adminId != uid) {
                onResult(false, "Apenas o administrador pode encerrar o rolê.")
                return@addOnSuccessListener
            }
            roleRef.update("status", "encerrado")
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { erro ->
                    Log.w("RoleRepository", "Erro ao encerrar o rolê", erro)
                    onResult(false, "Não foi possível encerrar o rolê. Tente novamente.")
                }
        }.addOnFailureListener { erro ->
            Log.w("RoleRepository", "Erro ao verificar o rolê para encerramento", erro)
            onResult(false, "Não foi possível acessar as informações do rolê.")
        }
    }

    fun excluirRoleEncerrado(roleId: String, onResult: (Boolean, String?) -> Unit) {
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
        roleRef.get(Source.SERVER).addOnSuccessListener { doc ->
            val adminId = doc.getString("adminId")
            val status = doc.getString("status")
            if (adminId != uid) {
                onResult(false, "Apenas o administrador pode excluir o rolê.")
                return@addOnSuccessListener
            }
            if (status != "encerrado") {
                onResult(false, "Encerrar o rolê antes de excluir.")
                return@addOnSuccessListener
            }

            val p1 = roleRef.collection("participantes").get(Source.SERVER)
            val p2 = roleRef.collection("perfisPublicos").get(Source.SERVER)
            val p3 = roleRef.collection("localizacoes").get(Source.SERVER)

            Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(p1, p2, p3)
                .addOnSuccessListener { resultados ->
                    val batch = firestore.batch()
                    resultados.forEach { snapshot ->
                        snapshot.documents.forEach { subDoc ->
                            batch.delete(subDoc.reference)
                        }
                    }
                    batch.delete(roleRef)
                    batch.commit()
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { erro ->
                            Log.w("RoleRepository", "Erro ao excluir rolê e subcoleções", erro)
                            onResult(false, "Não foi possível excluir o rolê. Tente novamente.")
                        }
                }
                .addOnFailureListener { erro ->
                    Log.w("RoleRepository", "Erro ao buscar subcoleções para exclusão", erro)
                    onResult(false, "Não foi possível acessar os dados do rolê para exclusão.")
                }
        }.addOnFailureListener { erro ->
            Log.w("RoleRepository", "Erro ao verificar o rolê para exclusão", erro)
            onResult(false, "Não foi possível acessar as informações do rolê.")
        }
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
