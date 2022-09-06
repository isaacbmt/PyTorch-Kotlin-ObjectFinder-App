package com.example.imageplayground

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

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.activity.result.contract.ActivityResultContracts

import android.view.Menu
import android.view.MenuItem
import java.io.File
import kotlinx.coroutines.Dispatchers


class MainActivity : AppCompatActivity() {
    val imageSegmentator = ImageSegmentator(Dispatchers.IO)
    val imageSegmentatorViewModel = ImageSegmentatorViewModel(imageSegmentator)
    lateinit var imageView: ImageView
    lateinit var pickerButton: Button
    lateinit var segmentButton: Button
    private var imageUri: Uri? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
//    private lateinit var binding: ActivityMainBinding

    var getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            imageView.setImageURI(imageUri)
            if (imageUri != null) {
                segmentButton.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Image playground app"
        imageView = findViewById(R.id.imagePreview)
        pickerButton = findViewById(R.id.photoPicker)
        segmentButton = findViewById(R.id.photoSegment)
        segmentButton.isEnabled = false

        pickerButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            getContent.launch(gallery)
        }
        segmentButton.setOnClickListener {
            val drawable = imageView.drawable as BitmapDrawable
            val bitmapImage: Bitmap = drawable.bitmap
            val moduleFileAbsoluteFilePath: String = File(
                Utils.assetFilePath(this, "deeplabv3_model_optimized_m.ptl")
            ).absolutePath
            print("module path: ")
            println(moduleFileAbsoluteFilePath)
            imageSegmentatorViewModel.startSegmentation(bitmapImage, moduleFileAbsoluteFilePath, imageView)
        }
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK && requestCode == pickImage) {
//            imageUri = data?.data
//            imageView.setImageURI(imageUri)
//        }
//    }
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setSupportActionBar(binding.toolbar)
//
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//
//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAnchorView(R.id.fab)
//                .setAction("Action", null).show()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}