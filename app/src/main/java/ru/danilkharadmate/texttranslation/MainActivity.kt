package ru.danilkharadmate.texttranslation

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.PaintDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.danilkharadmate.texttranslation.ui.theme.AppTheme
import ru.danilkharadmate.texttranslation.utils.Data
import ru.danilkharadmate.texttranslation.utils.DetectionResult
import ru.danilkharadmate.texttranslation.utils.drawDetectionResult
import ru.danilkharadmate.texttranslation.utils.rotateImage
import java.io.File
import java.io.IOException
import java.io.InputStream


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel by viewModels<MainViewModel>()
        setContent {
            val navController = rememberNavController()
            AppTheme {
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(viewModel, getImage = { uri ->
                            var bitmap: Bitmap? = null

                            if (uri != null) {
                                bitmap = getBitmapFormUri(this@MainActivity, uri = uri)
                                val file = getFileFromMediaUri(this@MainActivity, uri = uri)
                                val degree = getBitmapDegree(file?.absolutePath)

                                bitmap = bitmap?.let { rotateImage(it, degree.toFloat()) }
                            }

                            return@MainScreen bitmap
                        },
                            onAboutClick = {
                                navController.navigate("about")
                            }
                        )
                    }
                    composable("about") {
                        AboutScreen()
                    }
                }
            }
        }
    }

    fun getBitmapFormUri(ac: Activity, uri: Uri?): Bitmap? {
        var input: InputStream? = ac.contentResolver.openInputStream(uri!!)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input?.close()
        val originalWidth = onlyBoundsOptions.outWidth
        val originalHeight = onlyBoundsOptions.outHeight
        if (originalWidth == -1 || originalHeight == -1) return null
        //Image resolution is based on 480x800
        val hh = 1280f //The height is set as 800f here
        val ww = 720f //Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        var be = 1 //be=1 means no scaling
        if (originalWidth > originalHeight && originalWidth > ww) { //If the width is large, scale according to the fixed size of the width
            be = (originalWidth / ww).toInt()
        } else if (originalWidth < originalHeight && originalHeight > hh) { //If the height is high, scale according to the fixed size of the width
            be = (originalHeight / hh).toInt()
        }
        if (be <= 0) be = 1
        //Proportional compression
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = be //Set scaling
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        input = ac.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input?.close()
        return bitmap //Mass compression again
    }

    fun getFileFromMediaUri(ac: Context, uri: Uri): File? {
        if (uri.scheme.toString().compareTo("content") == 0) {
            val cr: ContentResolver = ac.contentResolver
            val cursor: Cursor? =
                cr.query(uri, null, null, null, null) // Find from database according to Uri
            if (cursor != null) {
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndex("_data")
                return if (columnIndex >= 0) {
                    val filePath: String = cursor.getString(columnIndex) // Get picture path
                    cursor.close()
                    File(filePath)
                } else {
                    null
                }
            }
        } else if (uri.scheme.toString().compareTo("file") == 0) {
            return File(uri.toString().replace("file://", ""))
        }
        return null
    }

    fun getBitmapDegree(path: String?): Int {
        var degree = 0
        try {
            // Read the picture from the specified path and obtain its EXIF information
            val exifInterface = ExifInterface(path!!)
            // Get rotation information for pictures
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }

}

@Composable
fun MainScreen(viewModel: MainViewModel, getImage: (Uri?) -> Bitmap?, onAboutClick: () -> Unit) {

    val detectionResults = viewModel.calculationFlow.collectAsState()
    var lastImage by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val bitmap = getImage(uri)
            if (bitmap != null) {
                lastImage = bitmap
                viewModel.calculateImage(bitmap = bitmap)
                Log.i("image", "MainScreen: $getImage")
            }
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            modifier = Modifier
                .size(50.dp)
                .align(alignment = Alignment.End),
            onClick = {
                onAboutClick()
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "icon",
            )
        }
        Box(Modifier.weight(1f)) {
            when (detectionResults.value) {
                is Data.Error -> {
                    Text(text = "Произошла проблема")
                }
                Data.Loading -> {
                    if (lastImage != null) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize(),
                            bitmap = lastImage!!.asImageBitmap(),
                            contentDescription = "clear image"
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.placeholder),
                            contentDescription = "",
                            Modifier.fillMaxSize()
                        )
                    }
                }
                is Data.Ok -> {
                    if ((detectionResults.value as Data.Ok<List<DetectionResult>>).data.isEmpty()) {
                        Text(text = "На картинке ничего не найдено")
                        if (lastImage != null) {
                            Image(
                                modifier = Modifier
                                    .fillMaxSize(),
                                bitmap = lastImage!!.asImageBitmap(),
                                contentDescription = "clear image"
                            )
                        }
                    } else if (lastImage != null) {
                        AiVisionImage(
                            modifier = Modifier
                                .fillMaxSize(),
                            image = lastImage!!,
                            objectList = (detectionResults.value as Data.Ok<List<DetectionResult>>).data
                        )
                    }
                }
            }
        }
        if (detectionResults.value is Data.Ok && (detectionResults.value as Data.Ok<List<DetectionResult>>).data.isNotEmpty()) {
            Text(text = "c вероятностью ${(detectionResults.value as Data.Ok<List<DetectionResult>>).data[0].percentage}% на картинке ${(detectionResults.value as Data.Ok<List<DetectionResult>>).data[0].label}!")
        }
        Log.i("image", "recompose $detectionResults")

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),

            onClick = {
                galleryLauncher.launch("image/")
            },
        ) {
            Text(text = "Выбрать картинку", color = colors.onBackground)
        }


    }
}

@Composable
fun AiVisionImage(modifier: Modifier = Modifier, image: Bitmap, objectList: List<DetectionResult>) {
    Image(
        modifier = modifier,
        bitmap = drawDetectionResult(image, objectList).asImageBitmap(),
        contentDescription = ""
    )
}


@Preview
@Composable
fun Preview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        MainScreen(
            MainViewModel(context = LocalContext.current),
            getImage = { uri ->
                return@MainScreen null
            },
            onAboutClick = {
            }
        )
    }
}

