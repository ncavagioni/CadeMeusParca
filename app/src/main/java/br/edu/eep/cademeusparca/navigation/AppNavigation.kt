package br.edu.eep.cademeusparca.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.eep.cademeusparca.ui.screens.CadastroInicialScreen
import br.edu.eep.cademeusparca.ui.screens.CriarRoleScreen
import br.edu.eep.cademeusparca.ui.screens.EntrarRoleScreen
import br.edu.eep.cademeusparca.ui.screens.MeusRolesScreen
import br.edu.eep.cademeusparca.ui.screens.RoleCriadoScreen
import br.edu.eep.cademeusparca.ui.screens.SplashScreen
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel
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
            CadastroInicialScreen(
                viewModel = usuarioViewModel,
                onCadastroConcluido = {
                    navController.navigate("meus_roles") {
                        popUpTo("cadastro") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("meus_roles") {
            MeusRolesScreen(
                onCriarRoleClick = {
                    navController.navigate("criar_role")
                },
                onEntrarRoleClick = {
                    navController.navigate("entrar_role")
                }
            )
        }

        composable("criar_role") {
            val roleViewModel: RoleViewModel = viewModel()
            CriarRoleScreen(
                viewModel = roleViewModel,
                onVoltar = {
                    navController.popBackStack()
                },
                onRoleCriado = { nome, codigo ->
                    navController.navigate(
                        "role_criado?nome=${Uri.encode(nome)}&codigo=${Uri.encode(codigo)}"
                    ) {
                        popUpTo("criar_role") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "role_criado?nome={nome}&codigo={codigo}",
            arguments = listOf(
                navArgument("nome") { type = NavType.StringType },
                navArgument("codigo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            RoleCriadoScreen(
                nome = backStackEntry.arguments?.getString("nome").orEmpty(),
                codigo = backStackEntry.arguments?.getString("codigo").orEmpty(),
                onVoltar = {
                    navController.popBackStack("meus_roles", inclusive = false)
                }
            )
        }

        composable("entrar_role") {
            EntrarRoleScreen(
                onVoltar = {
                    navController.popBackStack()
                }
            )
        }
    }
}
