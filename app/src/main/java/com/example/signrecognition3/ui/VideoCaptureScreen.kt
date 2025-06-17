package com.example.signrecognition3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCaptureScreen(
    vm: VideoCaptureViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val result by vm.result.collectAsState()
    val isRecording by vm.isRecordingState
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val gestures by vm.gestures.collectAsState()

    LaunchedEffect(showSheet) {
        if (showSheet) sheetState.show() else sheetState.hide()
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState // Removed contentWindowInsets here
        ) {
            Box(
                Modifier
                    .fillMaxHeight(0.8f)
                    .background(Color(0xFFF9F9F9))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                if (gestures.isEmpty()) {
                    Text("No se han detectado gestos.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Historial de Gestos",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        items(gestures) { gesture ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Gesto: ${gesture.gesture}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Fecha: ${
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                .format(Date(gesture.timestamp))
                                        }",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Aquí usas AndroidView para mostrar el TextureView
        AndroidView(
            factory = { vm.startCamera(context, lifecycleOwner) },  // Asegúrate de retornar el TextureView desde startCamera
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.85f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        // Los botones de control de grabación y cámara
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(screenHeight * 0.15f)
                .background(Color(0xFFF9F9F9))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showSheet = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Filled.List, contentDescription = "Historial")  // Fixed icon reference
                    Spacer(Modifier.width(6.dp))
                    Text("Historial")
                }

                IconButton(onClick = { vm.switchCamera(lifecycleOwner) }) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = "Cambiar cámara")
                }

                Button(
                    onClick = { vm.toggleRecording() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFEE4998) else Color(0xFF459FD7),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                        contentDescription = "Grabar/Detener"
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRecording) "Detener" else "Grabar")
                }
            }

            Text(result, style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
        }

        if (result == "Procesando...") {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable(enabled = false) {} ,
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Procesando...", color = Color.White)
                }
            }
        }
    }
}