package com.ambient.calculator2

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*

class HandwritingManager(private val context: Context, private val onResult: (String) -> Unit) {
    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null
    private var inkBuilder = Ink.builder()
    private var strokeBuilder: Ink.Stroke.Builder? = null
    
    private var isDownloading = false

    init {
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-autotch-math")
        if (modelIdentifier != null) {
            model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            downloadAndInitialize()
        } else {
            Log.e("Handwriting", "Math model identifier not found")
        }
    }

    private fun downloadAndInitialize() {
        val m = model ?: return
        val remoteModelManager = RemoteModelManager.getInstance()

        isDownloading = true
        remoteModelManager.download(m, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                isDownloading = false
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(m).build()
                )
                Log.i("Handwriting", "Math Engine Ready")
                Toast.makeText(context, "Handwriting Engine Ready", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                isDownloading = false
                Log.e("Handwriting", "Math Engine Download Failed", it)
            }
    }

    fun startStroke(x: Float, y: Float, t: Long) {
        strokeBuilder = Ink.Stroke.builder()
        strokeBuilder?.addPoint(Ink.Point.create(x, y, t))
    }

    fun addPoint(x: Float, y: Float, t: Long) {
        strokeBuilder?.addPoint(Ink.Point.create(x, y, t))
    }

    fun endStroke() {
        strokeBuilder?.let {
            inkBuilder.addStroke(it.build())
        }
        strokeBuilder = null
    }

    fun recognize() {
        val ink = inkBuilder.build()
        if (ink.strokes.isEmpty()) return

        val rec = recognizer
        if (rec == null) {
            if (!isDownloading) downloadAndInitialize()
            Toast.makeText(context, "Handwriting Engine Loading...", Toast.LENGTH_SHORT).show()
            clear()
            return
        }

        rec.recognize(ink)
            .addOnSuccessListener { result ->
                // Clean result and pick the best one
                val text = result.candidates.firstOrNull()?.text ?: ""
                Log.d("Handwriting", "Recognized: $text")
                if (text.isNotEmpty()) {
                    onResult(text)
                }
                clear()
            }
            .addOnFailureListener {
                Log.e("Handwriting", "Recognition failed", it)
                clear()
            }
    }

    fun clear() {
        inkBuilder = Ink.builder()
        strokeBuilder = null
    }
    
    fun close() {
        recognizer?.close()
    }
}
