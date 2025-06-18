package com.example.signrecognition3.ui


import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
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
import com.example.signrecognition3.utils.predictGesture
import com.example.signrecognition3.utils.preprocessLandmarks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val frameProcessor = FrameProcessor(application)
    private val landmarksCaptured = mutableListOf<Float>()   // Usamos landmarks en lugar de imágenes
    private var capturing = false
    private var interpreter: Interpreter? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var preview: Preview? = null  // Añadimos esta declaración
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Control de cámara activa
    private var _cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Estado UI
    private val isRecording = mutableStateOf(false)
    val isRecordingState: State<Boolean> = isRecording

    private val _result = MutableStateFlow("Esperando...")
    val result: StateFlow<String> = _result

    private val dao = GestureDatabase.getInstance(application).gestureDao()
    val gestures: StateFlow<List<GestureEntity>> =
        dao.getAllGesturesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInterpreter(interpreter: Interpreter) {
        this.interpreter = interpreter
    }

    fun toggleRecording() {
        if (isRecording.value) {
            stopCaptureAndPredict(getApplication())
        } else {
            startFrameCapture()
        }
        isRecording.value = !isRecording.value
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner): PreviewView  {
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

        return previewView // Retornamos el TextureView
    }

    private fun setupAnalysis() {
        analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(224, 224))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            if (capturing && landmarksCaptured.size < 100) {
                try {
                    val landmarks = frameProcessor.extractLandmarks(imageProxy)  // Usamos landmarks
                    synchronized(landmarksCaptured) {
                        landmarksCaptured.addAll(landmarks)  // Agregar los elementos de la lista, no la lista completa
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }

            if (landmarksCaptured.size >= 300 && capturing) { // Usamos 300 ya que estamos agregando 3 valores por landmark
                toggleRecording()
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
        landmarksCaptured.clear()
        _result.value = "Grabando..."
    }

    private fun stopCaptureAndPredict(context: Context) {
        capturing = false
        val landmarks = synchronized(landmarksCaptured) { landmarksCaptured.toList() }

        _result.value = "Procesando..."

        viewModelScope.launch(Dispatchers.IO) {
            // Log para verificar el tamaño de los landmarks
            Log.d("VideoCaptureViewModel", "Cantidad de Landmarks: ${landmarks.size}")

            // Verifica que los landmarks sean válidos y tienen el tamaño esperado
            if (landmarks.isEmpty() || landmarks.size < 134) {
                _result.value = "Sin datos suficientes"
                Log.d("VideoCaptureViewModel", "Datos insuficientes de landmarks")
                return@launch
            }

            // Preprocesar los landmarks y hacer la predicción
            val input = preprocessLandmarks(landmarks)

            // Log para verificar la entrada al modelo
            Log.d("VideoCaptureViewModel", "Entrada al modelo: ${input.contentDeepToString()}")

            val output = Array(1) { FloatArray(6) }  // Para 6 clases
            interpreter?.run(input, output)

            // Log para verificar la salida del modelo
            Log.d("VideoCaptureViewModel", "Salida del modelo: ${output.contentDeepToString()}")

            // Obtener la clase con mayor probabilidad
            val probabilities = output[0]
            val gestureIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

            val gestureLabels = listOf(
                "ausencia", "oliguria", "sangrado_vaginal", "taquicardia", "taquipnea", "tinitus"
            )

            // Log para ver la predicción final
            val predictedGesture = if (gestureIndex != -1) gestureLabels[gestureIndex] else "Ningún gesto reconocido"
            Log.d("VideoCaptureViewModel", "Predicción final: $predictedGesture")

            // Guardar en la base de datos
            dao.insertGesture(GestureEntity(gesture = predictedGesture))

            _result.value = "Gesto detectado: $predictedGesture"
            landmarksCaptured.clear()
            System.gc()
        }
    }
}