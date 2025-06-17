package com.example.signrecognition3.utils


import android.content.Context
import android.graphics.Bitmap
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

        // Pose landmarks
        val poseResult: PoseLandmarkerResult = poseLandmarker.detect(mpImage)
        poseResult.landmarks().firstOrNull()?.forEach { landmark ->
            result.add(landmark.x())
            result.add(landmark.y())
            result.add(landmark.z())
            // landmark.visibility() no se usa aquí, porque no está disponible
        }

        // Hand landmarks
        val handResult: HandLandmarkerResult = handLandmarker.detect(mpImage)
        handResult.landmarks().forEach { hand ->
            hand.forEach { landmark ->
                result.add(landmark.x())
                result.add(landmark.y())
                result.add(landmark.z())
            }
        }

        imageProxy.close()
        return result
    }
}
