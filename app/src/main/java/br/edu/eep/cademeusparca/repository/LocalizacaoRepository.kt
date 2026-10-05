package br.edu.eep.cademeusparca.repository

import android.util.Log
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class LocalizacaoRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun observarLocalizacoesDoRole(
        roleId: String,
        onResult: (List<LocalizacaoParticipante>?, String?) -> Unit
    ): ListenerRegistration? {
        if (auth.currentUser == null) {
            onResult(null, "Usuário não autenticado. Abra o aplicativo novamente.")
            return null
        }
        if (roleId.isBlank() || roleId.contains('/')) {
            onResult(null, "Rolê não encontrado.")
            return null
        }

        return firestore.collection("roles").document(roleId)
            .collection("localizacoes")
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    Log.w("LocalizacaoRepository", "Erro ao observar localizações do rolê", erro)
                    val mensagem = when (erro.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "Não foi possível ler as posições. Verifique sua participação e as regras do Firestore."
                        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                            "Sua autenticação não está disponível. Abra o aplicativo novamente."
                        else -> "Não foi possível carregar as posições dos parças. Reabra o mapa para tentar novamente."
                    }
                    onResult(null, mensagem)
                } else if (snapshot != null) {
                    // Reconstruir a lista também remove marcadores de documentos excluídos.
                    val localizacoes = snapshot.documents.mapNotNull { documento ->
                        LocalizacaoParticipante.deDocumento(documento.id, documento.data.orEmpty())
                    }
                    onResult(localizacoes, null)
                }
            }
    }

    fun salvarMinhaLocalizacao(
        roleId: String,
        latitude: Double,
        longitude: Double,
        precisao: Double?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Usuário não autenticado. Abra o aplicativo novamente.")
            return
        }
        if (roleId.isBlank() || roleId.contains('/')) {
            onResult(false, "Rolê não encontrado.")
            return
        }
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 ||
            (precisao != null && (!precisao.isFinite() || precisao < 0))
        ) {
            onResult(false, "Localização inválida. Tente obter sua posição novamente.")
            return
        }

        val localizacao = LocalizacaoParticipante(uid, latitude, longitude, precisao)
        val dados = mapOf(
            "userId" to localizacao.userId,
            "latitude" to localizacao.latitude,
            "longitude" to localizacao.longitude,
            "precisao" to localizacao.precisao,
            "atualizadoEm" to FieldValue.serverTimestamp()
        )

        // O UID vem da autenticação; o chamador não escolhe o usuário do documento.
        // O sucesso do set só é informado após a confirmação do servidor.
        firestore.collection("roles").document(roleId)
            .collection("localizacoes").document(uid)
            .set(dados)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { erro ->
                Log.w("LocalizacaoRepository", "Erro ao salvar a própria localização", erro)
                val mensagem = when ((erro as? FirebaseFirestoreException)?.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Não foi possível atualizar sua posição. Verifique sua participação e as regras do Firestore."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "Sua autenticação não está disponível. Abra o aplicativo novamente."
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Não foi possível atualizar sua posição. Verifique sua internet e tente novamente."
                    else -> "Não foi possível atualizar sua posição."
                }
                onResult(false, mensagem)
            }
    }
}
