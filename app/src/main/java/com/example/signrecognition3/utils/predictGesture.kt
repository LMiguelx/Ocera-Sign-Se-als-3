package com.example.signrecognition3.utils

import android.util.Log
import org.tensorflow.lite.Interpreter

fun preprocessLandmarks(landmarks: List<Float>, expectedCount: Int = 30): Array<Array<FloatArray>> {
    // Asegúrate de que los landmarks tengan la forma correcta
    if (landmarks.isEmpty()) {
        Log.e("Preprocess", "Error: Los landmarks están vacíos.")
        return Array(1) { Array(1) { FloatArray(6) } } // Devuelve un tensor vacío de la forma correcta
    }

    // Asegúrate de que estamos trabajando con 30 secuencias (o la longitud correcta para tu modelo)
    val padded = if (landmarks.size >= expectedCount * 3) {
        landmarks.take(expectedCount * 3)
    } else {
        landmarks + List(expectedCount * 3 - landmarks.size) { 0f }
    }

    return Array(1) {  // Mantener la estructura para que sea un array de 1 elemento
        Array(expectedCount) { i ->
            floatArrayOf(
                padded[i * 3], // Coordenada X
                padded[i * 3 + 1], // Coordenada Y
                padded[i * 3 + 2]  // Coordenada Z
            )
        }
    }
}

fun predictGesture(landmarks: List<Float>, interpreter: Interpreter): String {
    // Preprocesamos los landmarks a la forma (30, 134)
    val input = preprocessLandmarks(landmarks)

    // Log para ver la forma y el contenido del input
    Log.d("TensorFlowLite", "Input shape: ${input.size}, ${input[0].size}, ${input[0][0].size}")
    Log.d("TensorFlowLite", "Input data: ${input.contentDeepToString()}") // Muestra los datos de entrada

    val output = Array(1) { FloatArray(6) } // 6 clases posibles (ajustar según el modelo)

    // Ejecutamos el modelo
    try {
        interpreter.run(input, output)

        // Log para ver la forma y el contenido del output
        Log.d("TensorFlowLite", "Output shape: ${output.size}, ${output[0].size}")
        Log.d("TensorFlowLite", "Output data: ${output.contentDeepToString()}") // Muestra los datos de salida
    } catch (e: Exception) {
        Log.e("TensorFlowLite", "Error al ejecutar el modelo: ${e.message}")
    }

    // Si el output tiene probabilidades, seleccionamos la clase con la mayor probabilidad
    val probabilities = output[0]
    val gestureIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

    // Etiquetas de los gestos
    val gestureLabels = listOf(
        "ausencia",  // 0
        "oliguria",                         // 1
        "sangrado_vaginal",                 // 2
        "taquicardia",                      // 3
        "taquipnea",                        // 4
        "tinitus"                // 5
    )

    // Log para mostrar el índice y el gesto detectado
    Log.d("TensorFlowLite", "Predicción: $gestureIndex - ${gestureLabels.getOrElse(gestureIndex) { "No reconocido" }}")

    return gestureLabels.getOrElse(gestureIndex) { "Gesto no reconocido" }
}
