package com.example.signrecognition3.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * ✅ Resultado con landmarks + indicador si se detectó algo útil
 */
data class LandmarkResult(
    val landmarks: List<Float>,
    val isDetected: Boolean
)

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

    /**
     * ✅ Extrae landmarks y dice si se detectó algo significativo
     */
    fun extractLandmarks(imageProxy: ImageProxy): LandmarkResult {
        val bitmap = imageProxy.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val mpImage = BitmapImageBuilder(bitmap).build()

        val result = mutableListOf<Float>()

        var poseDetected = false
        var handsDetected = false

        // ✅ 1️⃣ Pose
        val poseResult: PoseLandmarkerResult = poseLandmarker.detect(mpImage)
        val poseLandmarks = poseResult.landmarks().firstOrNull()
        if (poseLandmarks != null && poseLandmarks.size >= 4) {
            poseDetected = true
            for (i in 0 until 4) {
                result.add(poseLandmarks[i].x())
                result.add(poseLandmarks[i].y())
            }
        } else {
            repeat(8) { result.add(0f) }
        }

        // ✅ 2️⃣ Hands
        val handResult: HandLandmarkerResult = handLandmarker.detect(mpImage)
        val hands = handResult.landmarks()

        val leftHand = hands.getOrNull(0)
        val rightHand = hands.getOrNull(1)

        // Mano izquierda
        if (leftHand != null && leftHand.size == 21) {
            handsDetected = true
            leftHand.forEach {
                result.add(it.x())
                result.add(it.y())
                result.add(it.z())
            }
        } else {
            repeat(63) { result.add(0f) }
        }

        // Mano derecha
        if (rightHand != null && rightHand.size == 21) {
            handsDetected = true
            rightHand.forEach {
                result.add(it.x())
                result.add(it.y())
                result.add(it.z())
            }
        } else {
            repeat(63) { result.add(0f) }
        }

        imageProxy.close()

        // ✅ Garantiza longitud fija y marca si se detectó algo útil
        val isDetected = poseDetected || handsDetected
        return LandmarkResult(
            landmarks = result.take(134),
            isDetected = isDetected
        )
    }
}
