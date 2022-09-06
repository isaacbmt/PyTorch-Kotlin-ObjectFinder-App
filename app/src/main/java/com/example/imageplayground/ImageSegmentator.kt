package com.example.imageplayground

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor

import kotlinx.coroutines.launch
import org.pytorch.IValue
import org.pytorch.torchvision.TensorImageUtils
import kotlin.math.roundToInt

class ImageSegmentator(private val ioDispatcher: CoroutineDispatcher) {
    lateinit var resultBitmap: Bitmap
    lateinit var inputTensor: Tensor
    lateinit var model: Module

    suspend fun segment(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, imageView: ImageView) {
        withContext(Dispatchers.Main) {
            segment_aux(bitmap, moduleFileAbsoluteFilePath, imageView)
        }
    }

    fun tensor2Bitmap(input: FloatArray, width: Int, height: Int, normMeanRGB: FloatArray, normStdRGB: FloatArray): Bitmap? {
        val pixelsCount = height * width
        val pixels = IntArray(pixelsCount)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val conversion = { v: Float -> v.roundToInt()}

        val offset_g = pixelsCount
        val offset_b = 2 * pixelsCount
        for (i in 0 until pixelsCount) {
            val r = conversion(input[i])
            val g = conversion(input[i + offset_g])
            val b = conversion(input[i + offset_b])
            pixels[i] = 255 shl 24 or (r.toInt() and 0xff shl 16) or (g.toInt() and 0xff shl 8) or (b.toInt() and 0xff)
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun segment_aux(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, imageView: ImageView) {
        model = LiteModuleLoader.load(moduleFileAbsoluteFilePath)
        inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        println("Forward")
        val outputTensor = model.forward(IValue.from(inputTensor))?.toTensor()
        println("...")
        val scores = outputTensor?.dataAsLongArray
        val width = bitmap.width
        val height = bitmap.height
        val length = width * height
        val pixels = IntArray(length)
        val conversion = { v: Long -> v.toInt() }
        if (scores != null) {
            for (i in 0 until length) {
                pixels[i] = conversion(scores[i])
            }
        }

        val bmpSegmentation = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputBitmap = bmpSegmentation.copy(bmpSegmentation.config, true)
        outputBitmap.setPixels(pixels, 0, outputBitmap.width, 0, 0, outputBitmap.width, outputBitmap.height)
        val transferredBitmap: Bitmap = Bitmap.createScaledBitmap(outputBitmap, bitmap.width, bitmap.height, true)
        resultBitmap = transferredBitmap
        imageView.setImageBitmap(transferredBitmap)
        println("Finished")
    }
}
// /data/user/0/com.example.imageplayground/files/deeplabv3_model_optimized_m.ptl
class ImageSegmentatorViewModel(private val imageSegmentator: ImageSegmentator): ViewModel() {
    fun startSegmentation(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, imageView: ImageView) {
        viewModelScope.launch {
            imageSegmentator.segment(bitmap, moduleFileAbsoluteFilePath, imageView)
        }
    }
}