package com.example.imageplayground

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt

object Utils {

    fun assetFilePath(context: Context, asset: String): String {
        val file = File(context.filesDir, asset)

        try {
            val inpStream: InputStream = context.assets.open(asset)
            try {
                val outStream = FileOutputStream(file, false)
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (true) {
                    read = inpStream.read(buffer)
                    if (read == -1) {
                        break
                    }
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun applyMask(input: FloatArray, mask: LongArray, color: Long, width: Int, height: Int): Bitmap {
        val length = width * height
        val pixels = IntArray(length)

        val normStdRGB = TensorImageUtils.TORCHVISION_NORM_STD_RGB
        val normMeanRGB = TensorImageUtils.TORCHVISION_NORM_MEAN_RGB
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        var a: Int
        var r: Int
        var g: Int
        var b: Int
        val conversion = { v: Float -> ((v.coerceIn(0.0f, 1.0f))*255.0f).roundToInt() }
        for (i in 0 until length) {
            if (mask[i % length] == color) {
                a = 0xff
                r = conversion(input[i] * normStdRGB[0] + normMeanRGB[0])
                g = conversion(input[i + length] * normStdRGB[1] + normMeanRGB[1])
                b = conversion(input[i + 2 * length] * normStdRGB[2] + normMeanRGB[2])
            } else {
                a = 0 // alpha channel: 0 = invisible
                r = 255
                g = 255
                b = 255
            }
            pixels[i] = a shl 24 or (r.toInt() and 0xff shl 16) or (g.toInt() and 0xff shl 8) or (b.toInt() and 0xff)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

}