package ru.danilkharadmate.texttranslation.utils

import android.graphics.*


const val MAX_FONT_SIZE = 52F


fun drawDetectionResult(
    bitmap: Bitmap,
    detectionResults: List<DetectionResult>
): Bitmap {
    val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(outputBitmap)
    val pen = Paint()
    pen.textAlign = Paint.Align.LEFT

    detectionResults.forEach {
        // draw bounding box
        pen.color = Color.RED
        pen.strokeWidth = 4F
        pen.style = Paint.Style.STROKE
        val box = it.boundingBox
        canvas.drawRect(box, pen)


        val tagSize = Rect(0, 0, 0, 0)

        // calculate the right font size
        pen.style = Paint.Style.FILL_AND_STROKE
        pen.color = Color.RED
        pen.strokeWidth = 2F

        pen.textSize = MAX_FONT_SIZE
        pen.getTextBounds(it.label, 0, it.label.length, tagSize)
        val fontSize: Float = pen.textSize * box.width() / tagSize.width()

        // adjust the font size so texts are inside the bounding box
        if (fontSize < pen.textSize) pen.textSize = fontSize

        var margin = (box.width() - tagSize.width()) / 2.0F
        if (margin < 0F) margin = 0F
        canvas.drawText(
            it.label, box.left + margin,
            box.top + tagSize.height().times(1F), pen
        )
    }
    return outputBitmap
}

fun rotateImage(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        source, 0, 0, source.width, source.height,
        matrix, true
    )
}

data class DetectionResult(val boundingBox: RectF, val label: String, val percentage: Int)
