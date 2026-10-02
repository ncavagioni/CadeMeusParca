package br.edu.eep.cademeusparca.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.eep.cademeusparca.ui.screens.CadastroInicialScreen
import br.edu.eep.cademeusparca.ui.screens.MeusRolesScreen
import br.edu.eep.cademeusparca.ui.screens.SplashScreen
import br.edu.eep.cademeusparca.viewmodel.UsuarioViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val usuarioViewModel: UsuarioViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        composable("splash") {

            LaunchedEffect(Unit) {
                usuarioViewModel.verificarFluxoInicial { possuiPerfil ->

                    val destino = if (possuiPerfil) {
                        "meus_roles"
                    } else {
                        "cadastro"
                    }

                    navController.navigate(destino) {
                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                }
            }

            SplashScreen()
        }

        composable("cadastro") {
            CadastroInicialScreen()
        }

        composable("meus_roles") {
            MeusRolesScreen()
        }
    }
}