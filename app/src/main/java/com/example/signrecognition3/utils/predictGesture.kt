package com.example.signrecognition3.utils

import org.tensorflow.lite.Interpreter

fun preprocessLandmarks(landmarks: List<Float>, expectedCount: Int = 100): Array<Array<FloatArray>> {
    val padded = if (landmarks.size >= expectedCount * 3) {
        landmarks.take(expectedCount * 3)
    } else {
        landmarks + List(expectedCount * 3 - landmarks.size) { landmarks.lastOrNull() ?: 0f }
    }

    return Array(expectedCount) { i ->
        floatArrayOf(
            padded[i * 3], // Coordenada X
            padded[i * 3 + 1], // Coordenada Y
            padded[i * 3 + 2]  // Coordenada Z
        )
    }.let { arrayOf(it) }
}

fun predictGesture(landmarks: List<Float>, interpreter: Interpreter): String {
    val input = preprocessLandmarks(landmarks)
    val output = Array(1) { FloatArray(6) } // Asumimos que hay 6 clases posibles

    interpreter.run(input, output)

    val probabilities = output[0]
    val gestureIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

    val gestureLabels = listOf(
        "Ausencia de movimientos fetales",
        "Oliguria",
        "Sangrado Vaginal",
        "Taquicardia",
        "Taquipnea",
        "Tinitus u Acufenos"
    )

    return gestureLabels.getOrElse(gestureIndex) { "Ningún gesto reconocido" }
}