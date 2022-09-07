package com.example.imageplayground

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor

import org.pytorch.IValue
import org.pytorch.torchvision.TensorImageUtils
import kotlin.math.roundToInt

class ImageSegmentator(private val ioDispatcher: CoroutineDispatcher) {
    lateinit var resultBitmap: Bitmap
    lateinit var inputTensor: Tensor
    lateinit var outputTensor: Tensor
    lateinit var model: Module

    suspend fun segment(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, app: MainActivity) {
        withContext(Dispatchers.Main) {
            app.segmentButton.isEnabled = false
            app.pickerButton.isEnabled = false
            delay(1)
            segmentAux(bitmap, moduleFileAbsoluteFilePath, app.segmentedView)
            app.segmentButton.isEnabled = true
            app.pickerButton.isEnabled = true
        }
    }

    private fun segmentAux(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, imageView: ImageView) {
        model = LiteModuleLoader.load(moduleFileAbsoluteFilePath)
        inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsLongArray
        val width = bitmap.width
        val height = bitmap.height
        val length = width * height
        val pixels = IntArray(length)
        val conversion = { v: Long -> v.toInt() }

        for (i in 0 until length) {
            pixels[i] = conversion(scores[i])
        }

        val bmpSegmentation = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputBitmap = bmpSegmentation.copy(bmpSegmentation.config, true)
        outputBitmap.setPixels(pixels, 0, outputBitmap.width, 0, 0, outputBitmap.width, outputBitmap.height)
        resultBitmap = outputBitmap
        imageView.setImageBitmap(outputBitmap)
        println("Finished")
    }
}

class ImageSegmentatorViewModel(private val imageSegmentator: ImageSegmentator): ViewModel() {
    fun startSegmentation(bitmap: Bitmap, moduleFileAbsoluteFilePath: String, app: MainActivity) {
        viewModelScope.launch {
            imageSegmentator.segment(bitmap, moduleFileAbsoluteFilePath, app)
        }

    }
}