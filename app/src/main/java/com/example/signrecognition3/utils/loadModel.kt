package com.example.signrecognition3.utils

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

fun loadModel(context: Context, modelName: String): MappedByteBuffer {
    val fd = context.assets.openFd(modelName)
    val inputStream = FileInputStream(fd.fileDescriptor)
    val channel = inputStream.channel
    return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
}