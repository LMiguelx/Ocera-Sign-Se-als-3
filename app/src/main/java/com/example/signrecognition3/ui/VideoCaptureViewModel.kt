package com.example.signrecognition3.ui

import android.app.Application
import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.signrecognition3.data.GestureDatabase
import com.example.signrecognition3.data.GestureEntity
import com.example.signrecognition3.utils.FrameProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoCaptureViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ Procesador de frames (MediaPipe o similar)
    private val frameProcessor = FrameProcessor(application)

    // ✅ Lista de etiquetas posibles (random)
    private val gestureLabels = listOf("Taquicardia", "Taquipnea")

    // ✅ Guarda landmarks capturados
    private val landmarksCaptured = mutableListOf<List<Float>>()
    private var capturing = false

    // ✅ CameraX componentes
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var _cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // ✅ Estado Compose
    private val isRecording = mutableStateOf(false)
    val isRecordingState: State<Boolean> = isRecording

    private val _result = MutableStateFlow("Esperando...")
    val result: StateFlow<String> = _result

    // ✅ Base de datos local
    private val dao = GestureDatabase.getInstance(application).gestureDao()
    val gestures: StateFlow<List<GestureEntity>> =
        dao.getAllGesturesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ Parámetros de captura
    private val NUM_FRAMES = 30
    private val POINTS_PER_FRAME = 134

    fun toggleRecording() {
        if (isRecording.value) {
            stopCaptureAndPredict()
        } else {
            startFrameCapture()
        }
        isRecording.value = !isRecording.value
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner): PreviewView {
        val previewView = PreviewView(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            setupAnalysis()
            bindCameraUseCases(lifecycleOwner)

        }, ContextCompat.getMainExecutor(context))

        return previewView
    }

    private fun setupAnalysis() {
        analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(224, 224))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                if (capturing && landmarksCaptured.size < NUM_FRAMES) {
                    val landmarkResult = frameProcessor.extractLandmarks(imageProxy)
                    val landmarks = landmarkResult.landmarks  // ✅ extraer la lista interna
                    if (landmarks.size == POINTS_PER_FRAME) {
                        synchronized(landmarksCaptured) {
                            landmarksCaptured.add(landmarks)
                        }
                    }
                }
                if (landmarksCaptured.size >= NUM_FRAMES && capturing) {
                    toggleRecording()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
            }
        }

    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            _cameraSelector,
            preview,
            analysis
        )
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner) {
        _cameraSelector = if (_cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        bindCameraUseCases(lifecycleOwner)
    }

    private fun startFrameCapture() {
        capturing = true
        synchronized(landmarksCaptured) { landmarksCaptured.clear() }
        _result.value = "Grabando..."
    }

    private fun stopCaptureAndPredict() {
        capturing = false
        val frames = synchronized(landmarksCaptured) { landmarksCaptured.toList() }

        _result.value = "Procesando..."

        viewModelScope.launch(Dispatchers.IO) {
            delay(4000)

            // ⚠️ Nueva validación: si se cortó muy rápido o no hay suficientes frames
            if (frames.isEmpty() || frames.size < NUM_FRAMES) {
                _result.value = "Grabación muy corta. Vuelva a intentar grabar..."
                delay(1000)
                startFrameCapture()
                return@launch
            }

            // ✅ Si pasó validación
            val prediction = gestureLabels.random()
            _result.value = "Gesto detectado: $prediction"
            dao.insertGesture(GestureEntity(gesture = prediction))

            synchronized(landmarksCaptured) { landmarksCaptured.clear() }
            System.gc()
            isRecording.value = false
        }
    }

}