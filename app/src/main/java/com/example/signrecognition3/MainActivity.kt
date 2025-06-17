package com.example.signrecognition3
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.signrecognition3.ui.PermissionScreen
import com.example.signrecognition3.ui.SplashScreen
import com.example.signrecognition3.ui.VideoCaptureScreen
import com.example.signrecognition3.ui.VideoCaptureViewModel
import com.example.signrecognition3.ui.theme.SignRecognition3Theme
import com.example.signrecognition3.utils.loadModel
import org.tensorflow.lite.Interpreter

class MainActivity : ComponentActivity() {
    private lateinit var videoCaptureViewModel: VideoCaptureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoCaptureViewModel = ViewModelProvider(this)[VideoCaptureViewModel::class.java]

        setContent {
            SignRecognition3Theme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                var showSplash by remember { mutableStateOf(true) }
                var permissionsGranted by remember {
                    mutableStateOf(
                        checkPermissions(context)
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    permissionsGranted =
                        perms[Manifest.permission.CAMERA] == true &&
                                perms[Manifest.permission.RECORD_AUDIO] == true
                }

                // Solicitar permisos solo después del splash
                LaunchedEffect(showSplash, permissionsGranted) {
                    if (!showSplash && !permissionsGranted) {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }
                }

                when {
                    showSplash -> {
                        SplashScreen(
                            lifecycleOwner = lifecycleOwner,
                            viewModel = videoCaptureViewModel
                        ) {
                            showSplash = false
                        }
                    }
                    !permissionsGranted -> {
                        PermissionScreen { permissionsGranted = true }
                    }
                    else -> {
                        VideoCaptureScreen(vm = videoCaptureViewModel)
                    }
                }
            }
        }
    }

    private fun checkPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

}
