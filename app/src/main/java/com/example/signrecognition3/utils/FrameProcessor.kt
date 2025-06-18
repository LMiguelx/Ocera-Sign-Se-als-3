package com.example.signrecognition3.utils


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.core.BaseOptions

class FrameProcessor(context: Context) {

    private val poseLandmarker: PoseLandmarker
    private val handLandmarker: HandLandmarker

    init {
        val poseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val poseLandmarkerOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(poseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, poseLandmarkerOptions)

        val handOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val handLandmarkerOptions = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(handOptions)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, handLandmarkerOptions)
    }

    fun extractLandmarks(imageProxy: ImageProxy): List<Float> {
        val bitmap = imageProxy.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = mutableListOf<Float>()

        // Detectar pose
        val poseResult: PoseLandmarkerResult = poseLandmarker.detect(mpImage)
        val poseLandmarks = poseResult.landmarks().firstOrNull()
        if (poseLandmarks != null && poseLandmarks.size >= 4) {
            for (i in 0 until 4) {
                result.add(poseLandmarks[i].x())
                result.add(poseLandmarks[i].y())
                result.add(poseLandmarks[i].z())
            }
        } else {
            repeat(12) { result.add(0f) }  // Asegurarse de rellenar los espacios si no hay detección
        }

        // Detectar manos
        val handResult: HandLandmarkerResult = handLandmarker.detect(mpImage)
        val hands = handResult.landmarks()

        val leftHand = hands.getOrNull(0)
        val rightHand = hands.getOrNull(1)

        if (leftHand != null && leftHand.size == 21) {
            leftHand.forEach {
                result.add(it.x())
                result.add(it.y())
                result.add(it.z())
            }
        } else {
            repeat(63) { result.add(0f) }  // Rellenar con ceros si no se detecta la mano
        }

        if (rightHand != null && rightHand.size == 21) {
            rightHand.forEach {
                result.add(it.x())
                result.add(it.y())
                result.add(it.z())
            }
        } else {
            repeat(63) { result.add(0f) }  // Rellenar con ceros si no se detecta la mano
        }

        imageProxy.close()

        Log.d("FrameProcessor", "Landmarks Detectados: $result")

        // Asegurarse de que siempre haya 134 puntos
        if (result.size != 134) {
            result.clear()
            result.addAll(List(134) { 0f })  // Rellenar con ceros si no hay 134 puntos
        }

        return result
    }
}