package com.example.signrecognition3.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.signrecognition3.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    lifecycleOwner: LifecycleOwner,
    viewModel: VideoCaptureViewModel,
    onSplashDone: () -> Unit
) {
    val context = LocalContext.current
    var loadingMessage by remember { mutableStateOf("Inicializando…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // Simula inicialización de 2 segundos
            delay(2000)
            onSplashDone()
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_icon_foreground),
                contentDescription = "Logo App",
                modifier = Modifier.size(360.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage ?: loadingMessage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF4A4A4A),
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFEE4998),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else if (errorMessage != null) {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}
