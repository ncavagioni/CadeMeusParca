package br.edu.eep.cademeusparca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.edu.eep.cademeusparca.ui.theme.CadeMeusParcaTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CadeMeusParcaTheme {
                FirebaseAuthTest()
            }
        }
    }
}

@Composable
fun FirebaseAuthTest() {
    var status by remember {
        mutableStateOf("Conectando ao Firebase...")
    }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            status = """
                Firebase conectado!

                UID:
                ${auth.currentUser?.uid}
            """.trimIndent()
        } else {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    status = if (task.isSuccessful) {
                        """
                            Firebase conectado!

                            UID:
                            ${auth.currentUser?.uid}
                        """.trimIndent()
                    } else {
                        """
                            Erro ao autenticar:

                            ${task.exception?.message}
                        """.trimIndent()
                    }
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = status)
    }
}