package br.edu.eep.cademeusparca.repository

import android.util.Log
import br.edu.eep.cademeusparca.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UsuarioRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val userIdAtual: String?
        get() = auth.currentUser?.uid

    private var uidParcanameEmCache: String? = null
    private var parcanameEmCache: String? = null

    fun publicarMeuParcanameNoRole(
        roleId: String,
        onResult: (String?, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null || roleId.isBlank() || roleId.contains('/')) {
            onResult(null, "Não foi possível identificar seu perfil neste rolê.")
            return
        }

        fun publicar(nome: String) {
            if (auth.currentUser?.uid != uid) {
                onResult(null, "Sua autenticação mudou. Reabra o mapa.")
                return
            }
            if (nome.isBlank() || nome.length > 100) {
                onResult(null, "O Parcaname precisa ter entre 1 e 100 caracteres para aparecer no mapa.")
                return
            }
            firestore.collection("roles").document(roleId)
                .collection("perfisPublicos").document(uid)
                .set(mapOf("userId" to uid, "parcaname" to nome))
                .addOnSuccessListener { onResult(nome, null) }
                .addOnFailureListener { erro ->
                    Log.w("UsuarioRepository", "Erro ao publicar Parcaname no rolê", erro)
                    onResult(nome, "Não foi possível compartilhar seu Parcaname. Verifique as regras do Firestore.")
                }
        }

        val nomeEmCache = parcanameEmCache
        if (uidParcanameEmCache == uid && nomeEmCache != null) {
            publicar(nomeEmCache)
            return
        }
        // Somente o próprio documento privado é consultado. Os colegas leem a projeção.
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                val nome = documento.data?.get("parcaname") as? String
                if (nome.isNullOrBlank()) {
                    onResult(null, "Seu Parcaname não está disponível.")
                } else {
                    uidParcanameEmCache = uid
                    parcanameEmCache = nome
                    publicar(nome)
                }
            }
            .addOnFailureListener { erro ->
                Log.w("UsuarioRepository", "Erro ao carregar o próprio Parcaname", erro)
                onResult(null, "Não foi possível carregar seu Parcaname.")
            }
    }

    fun observarParcanamesDoRole(
        roleId: String,
        onResult: (Map<String, String>?, String?) -> Unit
    ): ListenerRegistration? {
        if (auth.currentUser == null || roleId.isBlank() || roleId.contains('/')) {
            onResult(null, "Não foi possível identificar os parças deste rolê.")
            return null
        }
        return firestore.collection("roles").document(roleId)
            .collection("perfisPublicos")
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    Log.w("UsuarioRepository", "Erro ao ler Parcanames do rolê", erro)
                    onResult(null, "Não foi possível ler os Parcanames. Verifique as regras de perfisPublicos.")
                } else if (snapshot != null) {
                    val nomes = snapshot.documents.mapNotNull { documento ->
                        val dados = documento.data.orEmpty()
                        val uid = dados["userId"] as? String
                        val nome = dados["parcaname"] as? String
                        if (uid != documento.id || nome.isNullOrBlank() || nome.length > 100) null
                        else documento.id to nome
                    }.toMap()
                    onResult(nomes, null)
                }
            }
    }

    fun garantirAutenticacao(
        onResult: (Boolean, String?) -> Unit
    ) {
        if (auth.currentUser != null) {
            onResult(true, null)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { erro ->
                onResult(false, erro.message)
            }
    }

    fun salvarPerfil(
        parcaname: String,
        telefone: String,
        contatoEmergencia: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(false, "Usuário não autenticado")
            return
        }

        val usuario = Usuario(
            userId = uid,
            parcaname = parcaname.trim(),
            telefone = telefone.trim(),
            contatoEmergencia = contatoEmergencia.trim()
        )

        firestore
            .collection("usuarios")
            .document(uid)
            .set(usuario)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { erro ->
                onResult(false, erro.message)
            }
    }

    fun buscarPerfil(
        onResult: (Usuario?, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(null, "Usuário não autenticado")
            return
        }

        firestore
            .collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                if (documento.exists()) {
                    val usuario = documento.toObject(Usuario::class.java)
                    onResult(usuario, null)
                } else {
                    onResult(null, null)
                }
            }
            .addOnFailureListener { erro ->
                onResult(null, erro.message)
            }
    }
}