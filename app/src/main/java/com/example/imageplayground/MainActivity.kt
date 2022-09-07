package com.example.imageplayground

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.os.Bundle
import android.os.Environment

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.activity.result.contract.ActivityResultContracts

import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isInvisible
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.FileOutputStream
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    val imageSegmentator = ImageSegmentator(Dispatchers.IO)
    val imageSegmentatorViewModel = ImageSegmentatorViewModel(imageSegmentator)
    lateinit var imageView: ImageView
    lateinit var segmentedView: ImageView
    lateinit var filteredView: ImageView
    lateinit var pickerButton: Button
    lateinit var segmentButton: Button
    lateinit var saveButton: Button
    private var imageUri: Uri? = null

    private var getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            imageView.setImageURI(imageUri)
            if (imageUri != null) {
                saveButton.isInvisible = true
                segmentButton.isEnabled = true
                segmentedView.setImageDrawable(null)
                filteredView.setImageDrawable(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Image playground app"
        imageView       = findViewById(R.id.imagePreview)
        segmentedView   = findViewById(R.id.segmentedPreview)
        filteredView    = findViewById(R.id.filteredPreview)
        pickerButton    = findViewById(R.id.photoPicker)
        segmentButton   = findViewById(R.id.photoSegment)
        saveButton      = findViewById(R.id.saveButton)

        segmentButton.isEnabled = false
        saveButton.isInvisible = true

        pickerButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            getContent.launch(gallery)
        }
        segmentButton.setOnClickListener {
            runSegmentation()
        }
        saveButton.setOnClickListener {
            saveCroppedImage()
        }
        getClassObject()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun getClassObject() {
        segmentedView.setOnTouchListener { _, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()

            val drawable = segmentedView.drawable as BitmapDrawable
            val bitmap: Bitmap = drawable.bitmap

            val segWidth = segmentedView.width
            val width = bitmap.width
            val height = bitmap.height
            val margin = (segWidth.toFloat() - width.toFloat()) / 2.toFloat()

            if (x > margin && x < margin + width) {
                val xx = x - margin.toInt()
                cropImage(width, height, xx, y)
            }
            true
        }
    }

    private fun cropImage(width: Int, height: Int, x: Int, y: Int) {
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                            (imageView.drawable as BitmapDrawable).bitmap,
                            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                            TensorImageUtils.TORCHVISION_NORM_STD_RGB)

        val mask = imageSegmentator.outputTensor.dataAsLongArray
        val color = mask[y*width + x]

        val outputBitmap = Utils.applyMask(
            inputTensor.dataAsFloatArray,
            mask,
            color, width, height)

        filteredView.setImageBitmap(outputBitmap)
        saveButton.isInvisible = false

    }

    private fun saveCroppedImage() {
        val imageBitmap = (filteredView.drawable as BitmapDrawable).bitmap
        val imgDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val file = File(imgDir, "result_image.png")
        val imgFileOutputStream = FileOutputStream(file)
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, imgFileOutputStream)
        MediaStore.Images.Media.insertImage(this.contentResolver, imageBitmap, "Image title", null)
        Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun runSegmentation() {
        val drawable = imageView.drawable as BitmapDrawable
        val bitmapImage: Bitmap = drawable.bitmap
        val moduleFileAbsoluteFilePath: String = File(
            Utils.assetFilePath(this, "deeplabv3_model_optimized_m.ptl")
        ).absolutePath
        imageSegmentatorViewModel.startSegmentation(bitmapImage, moduleFileAbsoluteFilePath, this)
    }
}

