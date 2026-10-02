package br.edu.eep.cademeusparca.repository

import br.edu.eep.cademeusparca.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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