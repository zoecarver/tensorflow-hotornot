package com.makor.hotornot

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.makor.hotornot.classifier.*
import com.makor.hotornot.classifier.tensorflow.ImageClassifierFactory
import com.makor.hotornot.utils.getCroppedBitmap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis

private const val REQUEST_PERMISSIONS = 1

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
    }

    private fun checkPermissions() {
        if (arePermissionsAlreadyGranted()) {
            init()
        } else {
            requestPermissions()
        }
    }

    private fun arePermissionsAlreadyGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS && arePermissionGranted(grantResults)) {
            init()
        } else {
            requestPermissions()
        }
    }

    private fun arePermissionGranted(grantResults: IntArray) =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

    private fun init() {
        createClassifier()

        val time = measureTimeMillis {
            for (i in 0 until 100)
                classifyPhoto()
        }
        println("100 tests takes: $time miliseconds")
    }

    private fun createClassifier() {
        classifier = ImageClassifierFactory.create(
                assets,
                GRAPH_FILE_PATH,
                LABELS_FILE_PATH,
                IMAGE_SIZE,
                GRAPH_INPUT_NAME,
                GRAPH_OUTPUT_NAME
        )
    }

    private fun classifyPhoto() {
        val photoBitmap = getBitmapFromAssets(TEST_IMAGE_SRC) // BitmapFactory.decodeFile(file.absolutePath)
        val croppedBitmap = getCroppedBitmap(photoBitmap)

        classifyAndShowResult(croppedBitmap)

        imagePhoto.setImageBitmap(croppedBitmap)
    }

    private fun classifyAndShowResult(croppedBitmap: Bitmap) {
        val results = classifier.recognizeImage(croppedBitmap)
        println("results: $results")

        for (result in results)
            for (x in 0 until 20)
                for (y in 0 until 20)
                    croppedBitmap.setPixel(
                        result.getLocation().centerX().toInt() + x - 10,
                        result.getLocation().centerY().toInt() + y - 10,
                        if (result.id == "0") Color.YELLOW else Color.WHITE)

        results.first()
    }

    private fun getBitmapFromAssets(fileName: String): Bitmap {
        val am = assets
        var inputStream: InputStream? = null

        try {
            inputStream = am.open(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return BitmapFactory.decodeStream(inputStream)
    }
}
