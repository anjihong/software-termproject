package com.example.gittest.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

object TFLiteHelper {
    private lateinit var interpreter: Interpreter

    fun initialize(context: Context) {
        val model = FileUtil.loadMappedFile(context, "mobilenet_v2.tflite")
        interpreter = Interpreter(model)
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val input = convertBitmapToInputArray(bitmap)
        val output = Array(1) { FloatArray(1000) } // MobileNetV2는 보통 1000차원
        interpreter.run(input, output)
        return output[0]
    }

    private fun convertBitmapToInputArray(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)
                input[0][y][x][0] = (Color.red(pixel) - 127.5f) / 127.5f
                input[0][y][x][1] = (Color.green(pixel) - 127.5f) / 127.5f
                input[0][y][x][2] = (Color.blue(pixel) - 127.5f) / 127.5f
            }
        }
        return input
    }
}
