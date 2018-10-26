package com.makor.hotornot.classifier.tensorflow

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Trace
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

    override fun recognizeImage(bitmap: Bitmap): List<Recognition> {
        var intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        var byteValues = ByteArray(IMAGE_SIZE * IMAGE_SIZE * 3)

        var outputLocations = FloatArray(MAX_RESULTS * 4)
        var outputScores = FloatArray(MAX_RESULTS)
        var outputClasses = FloatArray(MAX_RESULTS)
        var outputNumDetections = FloatArray(1)

        val inputName = "image_tensor"

        // Preprocess the image data to extract R, G and B bytes from int of form 0x00RRGGBB
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in 0 until intValues.count()) {
            byteValues[i * 3 + 2] = (intValues[i] and 0xFF).toByte()
            byteValues[i * 3 + 1] = (intValues[i] shr 8 and 0xFF).toByte()
            byteValues[i * 3 + 0] = (intValues[i] shr 16 and 0xFF).toByte()
        }

        // Copy the input data into TensorFlow.
        println(byteValues)
        tensorFlowInference.feed(inputName, byteValues, 1, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong(), 3)

        // Run the inference call.
        tensorFlowInference.run(OUTPUT_NAMES, ENABLE_LOG_STATS)

        // Copy the output Tensor back into the output array.
        tensorFlowInference.fetch(OUTPUT_NAMES[0], outputLocations)
        tensorFlowInference.fetch(OUTPUT_NAMES[1], outputScores)
        tensorFlowInference.fetch(OUTPUT_NAMES[2], outputClasses)
        tensorFlowInference.fetch(OUTPUT_NAMES[3], outputNumDetections)
        Trace.endSection()

        // Find the best detections.
        val pq = PriorityQueue<Recognition>(
            1,
            Comparator<Recognition> { lhs, rhs ->
                // Intentionally reversed to put high confidence at the head of the queue.
                java.lang.Float.compare(rhs.confidence, lhs.confidence)
            })

        // Scale them back to the input size.
        for (i in outputScores.indices) {
            val detection = RectF(
                outputLocations[4 * i + 1] * IMAGE_SIZE,
                outputLocations[4 * i] * IMAGE_SIZE,
                outputLocations[4 * i + 3] * IMAGE_SIZE,
                outputLocations[4 * i + 2] * IMAGE_SIZE)
            pq.add(
                Recognition("" + i, labels[outputClasses[i].toInt()], outputScores[i], detection))
        }

        val recognitions = ArrayList<Recognition>()
        for (i in 0 until Math.min(pq.size, MAX_RESULTS)) {
            recognitions.add(pq.poll())
        }
        Trace.endSection() // "recognizeImage"
        return recognitions.filter { it.confidence > 0.5 }
    }
}