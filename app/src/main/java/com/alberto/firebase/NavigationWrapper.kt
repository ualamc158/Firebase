package com.alberto.firebase

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.firebase.presentation.homescreen.HomeScreen
import com.alberto.firebase.presentation.homescreen.HomeViewmodel
import com.alberto.firebase.presentation.initial.InitialScreen
import com.alberto.firebase.presentation.livechat.LiveChatScreen
import com.alberto.firebase.presentation.livechat.LiveChatViewModel
import com.alberto.firebase.presentation.login.LoginScreen
import com.alberto.firebase.presentation.signup.SignupScreen

import com.alberto.firebase.presentation.favorites.FavoritesScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    homeViewModel: HomeViewmodel
) {
    val startDestination = if (auth.currentUser != null) "home" else "Initial"

    NavHost(navHostController, startDestination = startDestination) {

        composable("Initial") {
            InitialScreen(
                auth = auth,
                navigateToLogin = { navHostController.navigate("Login") },
                navigateToSignUp = { navHostController.navigate("Signup") },
                navigateToHome = {
                    navHostController.navigate("home") {
                        popUpTo("Initial") { inclusive = true }
                    }

                }
            )
        }

        composable("Login") {
            LoginScreen(
                auth = auth,
                navigateToHome = {
                    navHostController.navigate("home") {
                        popUpTo("Login") { inclusive = true }
                        popUpTo("Initial") { inclusive = true }
                    }
                }
            )
        }

        composable("Signup") {
            SignupScreen(auth)
        }

        composable("home") {
            HomeScreen(
                viewmodel = homeViewModel,
                auth = auth,
                navigateToInitial = {
                    navHostController.navigate("Initial") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                navigateToChat = {
                    navHostController.navigate("livechat")
                },
                navigateToRadar = {
                    navHostController.navigate("radar")
                },

                navigateToFavorites = {
                    navHostController.navigate("favorites")
                }
            )
        }

        composable("livechat") {
            val liveViewModel: LiveChatViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LiveChatViewModel(auth) as T
                    }
                }
            )

            LiveChatScreen(
                viewModel = liveViewModel,
                onBack = { navHostController.popBackStack() }
            )
        }

        composable("radar") {
            com.alberto.firebase.presentation.map.SoundRadarScreen(
                onBack = { navHostController.popBackStack() }
            )
        }

        composable("favorites") {
            FavoritesScreen(
                viewModel = homeViewModel,
                onBack = { navHostController.popBackStack() }
            )
        }
    }
}