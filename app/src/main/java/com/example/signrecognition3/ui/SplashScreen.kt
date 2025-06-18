package com.example.signrecognition3.ui


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.signrecognition3.R
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
import com.example.signrecognition3.utils.loadModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.tensorflow.lite.Interpreter

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
    var shouldRetry by remember { mutableStateOf(false) }

    LaunchedEffect(shouldRetry) {
        isLoading = true
        errorMessage = null
        loadingMessage = "Inicializando…"

        try {
            withTimeout(5000) {
                val interpreter = withContext(Dispatchers.IO) {
                    Interpreter(loadModel(context, "gesture_model.tflite"))
                }
                viewModel.setInterpreter(interpreter)
                onSplashDone()
            }
        } catch (e: TimeoutCancellationException) {
            errorMessage = "La inicialización está tardando demasiado."
            Log.e("SplashScreen", "Timeout al inicializar", e)
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            Log.e("SplashScreen", "Fallo al iniciar app", e)
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
                    onClick = { shouldRetry = !shouldRetry },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}