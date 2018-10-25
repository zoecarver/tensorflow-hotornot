package com.makor.hotornot.classifier.tensorflow

import android.graphics.Bitmap
import android.graphics.RectF
import com.makor.hotornot.classifier.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.lang.Float
import java.util.*

private const val ENABLE_LOG_STATS = false

class ImageClassifier (
        private val inputName: String,
        private val outputName: String,
        private val imageSize: Long,
        private val labels: List<String>,
        private val imageBitmapPixels: IntArray,
        private val imageNormalizedPixels: ByteArray,
        private val results: FloatArray,
        private val tensorFlowInference: TensorFlowInferenceInterface
) : Classifier {

    var predictions = mutableListOf<Prediction>()

    override fun recognizeImage(bitmap: Bitmap): MutableList<Prediction> {
        preprocessImageToNormalizedFloats(bitmap)
        classifyImageToOutputs()
        return predictions
    }

    private fun preprocessImageToNormalizedFloats(bitmap: Bitmap) {
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        val imageMean = 128
        val imageStd = 128.0f
        bitmap.getPixels(imageBitmapPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in imageBitmapPixels.indices) {
            val `val` = imageBitmapPixels[i]
            imageNormalizedPixels[i * 3 + 0] = (((`val` shr 16 and 0xFF) - imageMean) / imageStd).toByte()
            imageNormalizedPixels[i * 3 + 1] = (((`val` shr 8 and 0xFF) - imageMean) / imageStd).toByte()
            imageNormalizedPixels[i * 3 + 2] = (((`val` and 0xFF) - imageMean) / imageStd).toByte()
        }
    }

    private fun classifyImageToOutputs() {
        var outputLocations = FloatArray(MAX_RESULTS * 4)
        var outputScores = FloatArray(MAX_RESULTS)
        var outputClasses = FloatArray(MAX_RESULTS)
        var outputNumDetections = FloatArray(1)

        tensorFlowInference.feed(/*inputName*/ "image_tensor", imageNormalizedPixels, 1, imageSize, imageSize, 3)
        tensorFlowInference.run(OUTPUT_NAMES, ENABLE_LOG_STATS)

        tensorFlowInference.fetch(OUTPUT_NAMES[0], outputLocations)
        tensorFlowInference.fetch(OUTPUT_NAMES[1], outputScores)
        tensorFlowInference.fetch(OUTPUT_NAMES[2], outputClasses)
        tensorFlowInference.fetch(OUTPUT_NAMES[3], outputNumDetections)

        // Find the best detections.
        val pq = PriorityQueue<Prediction>(
            1,
            Comparator<Prediction> { lhs, rhs ->
                // Intentionally reversed to put high confidence at the head of the queue.
                java.lang.Float.compare(rhs.score, lhs.score)
            })

        // Scale them back to the input size.
        for (i in outputScores.indices) {
            val detection = RectF(
                outputLocations[4 * i + 1] * IMAGE_SIZE,
                outputLocations[4 * i] * IMAGE_SIZE,
                outputLocations[4 * i + 3] * IMAGE_SIZE,
                outputLocations[4 * i + 2] * IMAGE_SIZE)
            pq.add(
                Prediction(detection, labels[outputClasses[i].toInt()], outputScores[i]))
        }

        val recognitions = ArrayList<Prediction>()
        for (i in 0 until Math.min(pq.size, MAX_RESULTS)) {
            recognitions.add(pq.poll())
        }

        this.predictions = recognitions.sortedWith(compareBy({ it.score }, { it.score })).takeLast(3).toMutableList()
    }
}