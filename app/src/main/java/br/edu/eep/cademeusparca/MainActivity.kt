package br.edu.eep.cademeusparca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.edu.eep.cademeusparca.navigation.AppNavigation
import br.edu.eep.cademeusparca.ui.theme.CadeMeusParcaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CadeMeusParcaTheme {
                AppNavigation()
            }
        }
    }
}