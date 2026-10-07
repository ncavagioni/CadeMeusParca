package br.edu.eep.cademeusparca.repository

import android.util.Log
import br.edu.eep.cademeusparca.model.Usuario
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UsuarioRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val userIdAtual: String?
        get() = auth.currentUser?.uid

    private var perfilPublicoEmCache: PerfilPublicoRole? = null

    fun publicarMeuPerfilPublicoNoRole(
        roleId: String,
        onResult: (PerfilPublicoRole?, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null || roleId.isBlank() || roleId.contains('/')) {
            onResult(null, "Não foi possível identificar seu perfil neste rolê.")
            return
        }

        fun publicar(perfil: PerfilPublicoRole) {
            if (auth.currentUser?.uid != uid) {
                onResult(null, "Sua autenticação mudou. Reabra o mapa.")
                return
            }
            firestore.collection("roles").document(roleId)
                .collection("perfisPublicos").document(uid)
                .set(mapOf(
                    "userId" to perfil.userId,
                    "parcaname" to perfil.parcaname,
                    "telefone" to perfil.telefone
                ))
                .addOnSuccessListener { onResult(perfil, null) }
                .addOnFailureListener { erro ->
                    Log.w("UsuarioRepository", "Erro ao publicar perfil público no rolê", erro)
                    onResult(perfil, "Não foi possível compartilhar seu perfil público. Verifique as regras do Firestore.")
                }
        }

        perfilPublicoEmCache?.takeIf { it.userId == uid }?.let {
            publicar(it)
            return
        }
        // Somente o próprio documento privado é consultado. Os colegas leem a projeção.
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                val dados = documento.data.orEmpty()
                val perfil = PerfilPublicoRole.deDocumento(uid, mapOf(
                    "userId" to uid,
                    "parcaname" to dados["parcaname"],
                    "telefone" to if ("telefone" in dados) dados["telefone"] else ""
                ))
                if (perfil == null) {
                    onResult(null, "Perfil inválido: Parcaname até 100 caracteres e telefone até 30.")
                } else if (auth.currentUser?.uid == uid) {
                    perfilPublicoEmCache = perfil
                    publicar(perfil)
                } else {
                    onResult(null, "Sua autenticação mudou. Reabra o mapa.")
                }
            }
            .addOnFailureListener { erro ->
                Log.w("UsuarioRepository", "Erro ao carregar o próprio Parcaname", erro)
                onResult(null, "Não foi possível carregar seu Parcaname.")
            }
    }

    fun observarPerfisPublicosDoRole(
        roleId: String,
        onResult: (List<PerfilPublicoRole>?, String?) -> Unit
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
                    val perfis = snapshot.documents.mapNotNull { documento ->
                        PerfilPublicoRole.deDocumento(documento.id, documento.data.orEmpty())
                    }
                    onResult(perfis, null)
                }
            }
    }

    fun observarPerfilPublicoNoRole(
        roleId: String,
        userId: String,
        onResult: (PerfilPublicoRole?, String?) -> Unit
    ): ListenerRegistration? {
        if (auth.currentUser == null || roleId.isBlank() || userId.isBlank() ||
            '/' in roleId || '/' in userId
        ) {
            onResult(null, "Não foi possível carregar os dados deste parça.")
            return null
        }
        return firestore.collection("roles").document(roleId)
            .collection("perfisPublicos").document(userId)
            .addSnapshotListener { documento, erro ->
                if (erro != null) {
                    Log.w("UsuarioRepository", "Erro ao observar perfil público do parça", erro)
                    onResult(null, "Não foi possível carregar os dados deste parça.")
                } else if (documento != null) {
                    val perfil = PerfilPublicoRole.deDocumento(userId, documento.data.orEmpty())
                    onResult(perfil, if (perfil == null) "Não foi possível carregar os dados deste parça." else null)
                }
            }
    }

    fun observarAutenticacao(onResult: (String?) -> Unit): ListenerRegistration {
        val listener = FirebaseAuth.AuthStateListener { onResult(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        return ListenerRegistration { auth.removeAuthStateListener(listener) }
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
                perfilPublicoEmCache = PerfilPublicoRole.deDocumento(uid, mapOf(
                    "userId" to uid, "parcaname" to usuario.parcaname, "telefone" to usuario.telefone
                ))
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