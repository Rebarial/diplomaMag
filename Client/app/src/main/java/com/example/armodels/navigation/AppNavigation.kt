package com.example.armodels.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.armodels.R
import com.example.armodels.network.ApiClient
import com.example.armodels.repository.AuthRepository
import com.example.armodels.repository.GenerationRepository
import com.example.armodels.repository.ModelRepository
import com.example.armodels.storage.SettingsStorage
import com.example.armodels.storage.TokenStorage
import com.example.armodels.ui.ar.ARScreen
import com.example.armodels.ui.ar.ARViewModel
import com.example.armodels.ui.generate.GenerateScreen
import com.example.armodels.ui.generate.GenerateViewModel
import com.example.armodels.ui.library.LibraryScreen
import com.example.armodels.ui.library.LibraryViewModel
import com.example.armodels.ui.login.LoginScreen
import com.example.armodels.ui.login.LoginViewModel
import com.example.armodels.ui.profile.ProfileScreen
import com.example.armodels.ui.profile.ProfileViewModel
import com.example.armodels.ui.settings.SettingsScreen
import com.example.armodels.ui.settings.SettingsViewModel

private fun arViewModelFactory(modelRepository: ModelRepository): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ARViewModel(modelRepository = modelRepository) as T
        }
    }

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Library : Screen("library")
    object Generate : Screen("generate")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AR : Screen("ar/{modelId}") {
        fun createRoute(modelId: Int) = "ar/$modelId"
    }
    object ARLocal : Screen("ar_local/{localId}") {
        fun createRoute(localId: Int) = "ar_local/$localId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStorage = TokenStorage(context)

    val defaultServerUrl = stringResource(R.string.server_base_url)
    val settingsStorage = SettingsStorage(context, defaultServerUrl)
    val settingsViewModel = SettingsViewModel(settingsStorage)

    ApiClient.init(provider = { tokenStorage.getToken() })

    LaunchedEffect(Unit) {
        settingsStorage.serverUrl.collect { url ->
            ApiClient.setBaseUrl(url)
        }
    }

    val authRepository = AuthRepository(tokenStorage)
    val modelRepository = ModelRepository(context)
    val generationRepository = GenerationRepository(context)

    NavHost(navController = navController, startDestination = Screen.Library.route) {

        composable(Screen.Login.route) {
            val viewModel = LoginViewModel(authRepository = authRepository)
            LoginScreen(navController = navController, viewModel = viewModel)
        }

        composable(Screen.Library.route) {
            val viewModel = LibraryViewModel(
                modelRepository = modelRepository,
                tokenStorage = tokenStorage
            )
            LibraryScreen(navController = navController, viewModel = viewModel)
        }

        composable(Screen.Generate.route) {
            val viewModel = GenerateViewModel(
                generationRepository = generationRepository,
                modelRepository = modelRepository,
                tokenStorage = tokenStorage,
                initialGenerationMode = settingsViewModel.defaultGenerationMode.value
            )
            GenerateScreen(navController = navController, viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController, viewModel = settingsViewModel)
        }

        composable(Screen.Profile.route) {
            val viewModel = ProfileViewModel(modelRepository = modelRepository)
            ProfileScreen(navController = navController, viewModel = viewModel)
        }

        composable(
            route = Screen.AR.route,
            arguments = listOf(navArgument("modelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getInt("modelId") ?: return@composable
            val arViewModel: ARViewModel = viewModel(factory = arViewModelFactory(modelRepository))
            val lightingMode by settingsViewModel.lightingMode.collectAsState()
            ARScreen(
                navController = navController,
                viewModel = arViewModel,
                modelId = modelId,
                lightingMode = lightingMode
            )
        }

        composable(
            route = Screen.ARLocal.route,
            arguments = listOf(navArgument("localId") { type = NavType.IntType })
        ) { backStackEntry ->
            val localId = backStackEntry.arguments?.getInt("localId") ?: return@composable
            val arViewModel: ARViewModel = viewModel(factory = arViewModelFactory(modelRepository))
            val lightingMode by settingsViewModel.lightingMode.collectAsState()
            ARScreen(
                navController = navController,
                viewModel = arViewModel,
                modelId = localId,
                isLocal = true,
                lightingMode = lightingMode
            )
        }
    }
}