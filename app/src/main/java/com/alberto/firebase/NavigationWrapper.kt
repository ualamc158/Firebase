package com.alberto.firebase

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.alberto.firebase.presentation.homescreen.HomeScreen
import com.alberto.firebase.presentation.homescreen.HomeViewmodel
import com.alberto.firebase.presentation.initial.InitialScreen
import com.alberto.firebase.presentation.login.LoginScreen
import com.alberto.firebase.presentation.signup.SignupScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(navHostController: NavHostController, auth: FirebaseAuth, homeViewModel: HomeViewmodel) {
    NavHost(navHostController, startDestination="Initial") {
        composable("Initial"){
            InitialScreen(
                auth = auth, // Añadimos el objeto de autenticación
                navigateToLogin = { navHostController.navigate("Login") },
                navigateToSignUp = { navHostController.navigate("Signup") },
                navigateToHome = { navHostController.navigate("home") } // Añadimos a dónde ir al tener éxito
            )
        }
        composable("Login"){
            LoginScreen(auth, navigateToHome = { navHostController.navigate("home") })
        }
        composable("Signup"){
            SignupScreen(auth)
        }
        composable("home"){
            HomeScreen(homeViewModel)
        }
    }
}