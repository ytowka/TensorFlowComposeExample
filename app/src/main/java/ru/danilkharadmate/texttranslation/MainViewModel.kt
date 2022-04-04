package ru.danilkharadmate.texttranslation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import ru.danilkharadmate.texttranslation.utils.Data
import ru.danilkharadmate.texttranslation.utils.DetectionResult
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext private val context: Context): ViewModel(){

    val calculationFlow = MutableStateFlow<Data<List<DetectionResult>>>(Data.Loading)

    fun calculateImage(bitmap: Bitmap){
        viewModelScope.launch(Dispatchers.IO) {
            val image = TensorImage.fromBitmap(bitmap)

            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(0.5f)
                .build()

            val detector = ObjectDetector.createFromFileAndOptions(context, "object_detection_model.tflite", options)

            val results = detector.detect(image)

            val resultToDisplay = results.map {
                // Get the top-1 category and craft the display text
                val category = it.categories.first()

                Log.i("image", "post detections")

                // Create a data object to display the detection result
                DetectionResult(it.boundingBox, category.label, category.score.times(100).toInt())
            }

            calculationFlow.value = Data.Ok(resultToDisplay)
        }
    }
}