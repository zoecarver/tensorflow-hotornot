package com.makor.hotornot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.makor.hotornot.classifier.*
import com.makor.hotornot.classifier.tensorflow.ImageClassifierFactory
import com.makor.hotornot.utils.getCroppedBitmap
import com.makor.hotornot.utils.getUriFromFilePath
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.IOException
import java.io.InputStream

private const val REQUEST_PERMISSIONS = 1
private const val REQUEST_TAKE_PICTURE = 2

class MainActivity : AppCompatActivity() {

    private val handler = Handler()
    private lateinit var classifier: Classifier
    private var photoFilePath = ""

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
        classifyPhoto()

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
        /*
            AssetManager
                Provides access to an application's raw asset files.
        */

        /*
            public final AssetManager getAssets ()
                Retrieve underlying AssetManager storage for these resources.
        */
        val am = assets
        var `is`: InputStream? = null
        try {
            /*
                public final InputStream open (String fileName)
                    Open an asset using ACCESS_STREAMING mode. This provides access to files that
                    have been bundled with an application as assets -- that is,
                    files placed in to the "assets" directory.

                    Parameters
                        fileName : The name of the asset to open. This name can be hierarchical.
                    Throws
                        IOException
            */
            `is` = am.open(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        /*
            BitmapFactory
                Creates Bitmap objects from various sources, including files, streams, and byte-arrays.
        */

        /*
            public static Bitmap decodeStream (InputStream is)
                Decode an input stream into a bitmap. If the input stream is null, or cannot
                be used to decode a bitmap, the function returns null. The stream's
                position will be where ever it was after the encoded data was read.

                Parameters
                    is : The input stream that holds the raw data to be decoded into a bitmap.
                Returns
                    The decoded bitmap, or null if the image data could not be decoded.
        */
        return BitmapFactory.decodeStream(`is`)
    }
}
