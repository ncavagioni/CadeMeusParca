package br.edu.eep.cademeusparca.navigation

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.ui.screens.CadastroInicialScreen
import br.edu.eep.cademeusparca.ui.screens.ConfirmarRoleScreen
import br.edu.eep.cademeusparca.ui.screens.CriarRoleScreen
import br.edu.eep.cademeusparca.ui.screens.EntrarRoleScreen
import br.edu.eep.cademeusparca.ui.screens.MapaRoleScreen
import br.edu.eep.cademeusparca.ui.screens.DetalhesParcaScreen
import br.edu.eep.cademeusparca.ui.screens.DetalhesRoleScreen
import br.edu.eep.cademeusparca.ui.screens.MeusRolesScreen
import br.edu.eep.cademeusparca.ui.screens.ParcasRoleScreen
import br.edu.eep.cademeusparca.ui.screens.RoleCriadoScreen
import br.edu.eep.cademeusparca.ui.screens.SplashScreen
import br.edu.eep.cademeusparca.viewmodel.MapaRoleViewModel
import br.edu.eep.cademeusparca.viewmodel.DetalhesParcaViewModel
import br.edu.eep.cademeusparca.viewmodel.DetalhesRoleViewModel
import br.edu.eep.cademeusparca.viewmodel.RoleViewModel
import br.edu.eep.cademeusparca.viewmodel.ParcasRoleViewModel
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

        composable("meus_roles") { backStackEntry ->
            val roleViewModel: RoleViewModel = viewModel()

            DisposableEffect(backStackEntry, roleViewModel) {
                val observer = LifecycleEventObserver { _, evento ->
                    if (evento == Lifecycle.Event.ON_RESUME) {
                        roleViewModel.buscarRolesDoUsuario()
                    }
                }
                backStackEntry.lifecycle.addObserver(observer)
                onDispose {
                    backStackEntry.lifecycle.removeObserver(observer)
                }
            }

            MeusRolesScreen(
                viewModel = roleViewModel,
                onCriarRoleClick = {
                    navController.navigate("criar_role")
                },
                onEntrarRoleClick = {
                    navController.navigate("entrar_role")
                },
                onRoleClick = { roleId ->
                    navController.navigate("mapa_role/${Uri.encode(roleId)}") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "mapa_role/{roleId}",
            arguments = listOf(navArgument("roleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mapaViewModel: MapaRoleViewModel = viewModel()
            val navegacaoInfoWindow = remember(backStackEntry) {
                val handler = Handler(Looper.getMainLooper())
                NavegacaoInfoWindow { acao -> handler.post { acao() } }
            }
            MapaRoleScreen(
                roleId = backStackEntry.arguments?.getString("roleId").orEmpty(),
                viewModel = mapaViewModel,
                onVoltar = { navController.popBackStack() },
                onAbrirRole = {
                    val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate("detalhes_role/${Uri.encode(roleId)}") {
                            launchSingleTop = true
                        }
                    }
                },
                onAbrirParcas = {
                    val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate("parcas_role/${Uri.encode(roleId)}") {
                            launchSingleTop = true
                        }
                    }
                },
                onAbrirParca = { userId ->
                    val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
                    if (userId.isNotBlank() && userId != mapaViewModel.userIdAtual) {
                        // Retornar ao Maps antes de navigate() provocar MapView.onStop().
                        navegacaoInfoWindow.solicitar(
                            podeNavegar = { navController.currentBackStackEntry == backStackEntry },
                            navegar = {
                                navController.navigate(
                                    "detalhes_parca/${Uri.encode(roleId)}/${Uri.encode(userId)}"
                                ) { launchSingleTop = true }
                            }
                        )
                    }
                }
            )
        }

        composable(
            route = "detalhes_role/{roleId}",
            arguments = listOf(navArgument("roleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
            val detalhesRoleViewModel: DetalhesRoleViewModel = viewModel(viewModelStoreOwner = backStackEntry)
            DetalhesRoleScreen(
                roleId = roleId,
                viewModel = detalhesRoleViewModel,
                onVoltar = { navController.popBackStack() },
                onVerParcas = {
                    if (navController.currentBackStackEntry == backStackEntry &&
                        detalhesRoleViewModel.podeVerParcas()
                    ) {
                        navController.navigate("parcas_role/${Uri.encode(roleId)}") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "parcas_role/{roleId}",
            arguments = listOf(navArgument("roleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
            val parcasViewModel: ParcasRoleViewModel = viewModel(viewModelStoreOwner = backStackEntry)
            val mapaEntry = remember(backStackEntry) {
                // Preserva a origem Android também no fluxo Mapa → Rolê → Parças.
                runCatching {
                    navController.getBackStackEntry("mapa_role/${Uri.encode(roleId)}")
                }.getOrNull()
            }
            val mapaViewModel: MapaRoleViewModel? = mapaEntry?.let {
                viewModel(viewModelStoreOwner = it)
            }
            ParcasRoleScreen(
                roleId = roleId,
                viewModel = parcasViewModel,
                posicaoAndroidInicial = mapaViewModel?.localizacao,
                onVoltar = { navController.popBackStack() },
                onAbrirParca = { userId ->
                    if (navController.currentBackStackEntry == backStackEntry &&
                        parcasViewModel.podeAbrirParca(userId)
                    ) {
                        navController.navigate(
                            "detalhes_parca/${Uri.encode(roleId)}/${Uri.encode(userId)}"
                        ) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(
            route = "detalhes_parca/{roleId}/{userId}",
            arguments = listOf(
                navArgument("roleId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getString("roleId").orEmpty()
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
            val detalhesViewModel: DetalhesParcaViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            )
            val mapaEntry = remember(backStackEntry) {
                // O mapa também está na pilha quando os detalhes vêm da lista de parças.
                runCatching {
                    navController.getBackStackEntry("mapa_role/${Uri.encode(roleId)}")
                }.getOrNull()
            }
            val mapaViewModel: MapaRoleViewModel? = mapaEntry?.let {
                viewModel(viewModelStoreOwner = it)
            }
            DetalhesParcaScreen(
                roleId = roleId,
                userId = userId,
                viewModel = detalhesViewModel,
                posicaoAndroidInicial = mapaViewModel?.localizacao,
                onVoltar = { navController.popBackStack() }
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
            val roleViewModel: RoleViewModel = viewModel()
            EntrarRoleScreen(
                viewModel = roleViewModel,
                onVoltar = {
                    navController.popBackStack()
                },
                onRoleEncontrado = { role ->
                    navController.navigate(
                        "confirmar_role/${Uri.encode(role.roleId)}" +
                            "?nome=${Uri.encode(role.nome)}" +
                            "&codigo=${Uri.encode(role.codigo)}" +
                            "&local=${Uri.encode(role.nomeLocal)}" +
                            "&endereco=${Uri.encode(role.endereco)}"
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "confirmar_role/{roleId}?nome={nome}&codigo={codigo}&local={local}&endereco={endereco}",
            arguments = listOf(
                navArgument("roleId") { type = NavType.StringType },
                navArgument("nome") { type = NavType.StringType; defaultValue = "" },
                navArgument("codigo") { type = NavType.StringType; defaultValue = "" },
                navArgument("local") { type = NavType.StringType; defaultValue = "" },
                navArgument("endereco") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val roleViewModel: RoleViewModel = viewModel()
            val role = Role(
                roleId = backStackEntry.arguments?.getString("roleId").orEmpty(),
                nome = backStackEntry.arguments?.getString("nome").orEmpty(),
                codigo = backStackEntry.arguments?.getString("codigo").orEmpty(),
                nomeLocal = backStackEntry.arguments?.getString("local").orEmpty(),
                endereco = backStackEntry.arguments?.getString("endereco").orEmpty()
            )
            ConfirmarRoleScreen(
                role = role,
                viewModel = roleViewModel,
                onCancelar = {
                    navController.popBackStack()
                },
                onVoltarMeusRoles = {
                    navController.popBackStack("meus_roles", inclusive = false)
                }
            )
        }
    }
}
