package com.alberto.firebase.presentation.initial

import android.app.Activity
import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alberto.firebase.R
import com.alberto.firebase.ui.theme.BackgroundButton
import com.alberto.firebase.ui.theme.Black
import com.alberto.firebase.ui.theme.Gray
import com.alberto.firebase.ui.theme.ShapeButton
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun InitialScreen(
    auth: FirebaseAuth,
    navigateToLogin: () -> Unit = {},
    navigateToSignUp: () -> Unit = {},
    navigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current

    // 🌟 1. PREPARAMOS EL GESTOR DE RESULTADOS DE FACEBOOK
    val callbackManager = remember { CallbackManager.Factory.create() }
    val registryOwner = LocalActivityResultRegistryOwner.current

    // 🌟 2. ESCUCHAMOS LA RESPUESTA DE FACEBOOK EN SEGUNDO PLANO
    DisposableEffect(Unit) {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // Si el login de Facebook va bien, le pasamos el "ticket" a Firebase
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("Alberto", "LOGIN FACEBOOK OK")
                        navigateToHome()
                    } else {
                        Log.i("Alberto", "LOGIN FACEBOOK KO: ${task.exception?.message}")
                    }
                }
            }
            override fun onCancel() {
                Log.i("Alberto", "LOGIN FACEBOOK CANCELADO")
            }
            override fun onError(error: FacebookException) {
                Log.i("Alberto", "LOGIN FACEBOOK ERROR: ${error.message}")
            }
        })
        onDispose { }
    }

    // GESTOR DE GOOGLE
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Log.i("Alberto", "LOGIN GOOGLE OK")
                        navigateToHome()
                    } else {
                        Log.i("Alberto", "LOGIN GOOGLE KO: ${authTask.exception?.message}")
                    }
                }
            } catch (e: ApiException) {
                Log.i("Alberto", "Error en el selector de Google: ${e.message}")
            }
        }
    }

    Column(modifier= Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)),
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = painterResource(R.drawable.spotify),
            contentDescription = "",
            modifier = Modifier.clip(CircleShape))

        Spacer(modifier = Modifier.weight(1f))

        Text("Millions of songs.", color= Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Free on Spotify", color= Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = {navigateToSignUp()},
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 32.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)){
            Text(text = "Sign up free", color = Color.Black,fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // BOTÓN DE GOOGLE
        CustomButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 32.dp)
                .clip(CircleShape)
                .clickable {
                    @Suppress("DEPRECATION")
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    @Suppress("DEPRECATION")
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                }
                .background(BackgroundButton)
                .border(2.dp, ShapeButton, CircleShape),
            painter = painterResource(R.drawable.google),
            title = "Continue with Google"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🌟 BOTÓN DE FACEBOOK ACTUALIZADO
        CustomButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 32.dp)
                .clip(CircleShape) // Hacemos que el clic sea redondo
                .clickable {
                    // 🌟 LANZAMOS EL LOGIN DE FACEBOOK
                    registryOwner?.let {
                        LoginManager.getInstance().logIn(
                            it,
                            callbackManager,
                            listOf("public_profile")
                        )
                    }
                }
                .background(BackgroundButton)
                .border(2.dp, ShapeButton, CircleShape),
            painter = painterResource(R.drawable.facebook),
            title = "Continue with Facebook"
        )

        Text(text="Log In", color = Color.White, modifier = Modifier
            .padding(24.dp)
            .clickable{navigateToLogin()}, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomButton(modifier: Modifier, painter: Painter, title: String){
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart){
        Image(painter = painter,
            contentDescription = "",
            modifier = Modifier.padding(start=16.dp).size(16.dp))
        Text(text=title,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold)
    }
}