package com.example.emotionrecognizer

import android.annotation.SuppressLint
import android.graphics.*
import android.media.ExifInterface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.emotionrecognizer.ml.Model
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection.getClient
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder


private var detectImage:Bitmap? = null

class DetectEmotion : AppCompatActivity(){
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detect_emotion)

        val showPictureInImageView = findViewById<ImageView>(R.id.showPictureImageView)
        val findEmotionButton = findViewById<Button>(R.id.verifyEmotionButton)
        val resultTextView =  findViewById<TextView>(R.id.textView)
        resultTextView.isVisible = false

        val imagePath = intent.getStringExtra("imagePath")
        var takenImage = BitmapFactory.decodeFile(imagePath)

        val exif = ExifInterface(imagePath!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)

        val matrix = Matrix()
        if (orientation == 6) {
            matrix.postRotate(90F)
        } else if (orientation == 3) {
            matrix.postRotate(180F)
        } else if (orientation == 8) {
            matrix.postRotate(270F)
        }

        takenImage = Bitmap.createBitmap(
            takenImage,
            0,
            0,
            takenImage.getWidth(),
            takenImage.getHeight(),
            matrix,
            true
        )

        val image = InputImage.fromBitmap(takenImage, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        val detector = getClient(options)

        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                if (!faces.isEmpty()) {
                    if (faces.size == 1) {
                        val face = faces[0]
                        val bounds = face.boundingBox

                        val x = Math.max(bounds.left, 0)
                        val y = Math.max(bounds.top, 0)

                        val width = bounds.width()
                        val height = bounds.height()

                        bounds.set(
                            bounds.left,
                            bounds.top,
                            bounds.right,
                            bounds.bottom
                        )

                        val crop = Bitmap.createBitmap(
                            takenImage,
                            x,
                            y,
                            if(x + width > takenImage.width) takenImage.width - x else width,
                            if(y + width > takenImage.height) takenImage.height - y else height
                        )
                        showPictureInImageView.setImageBitmap(crop)
                        val resizedBitmap = Bitmap.createScaledBitmap(
                            crop, 48, 48, true
                        )
                        val grayScalePicture = toGrayscale(resizedBitmap)
                        detectImage = grayScalePicture

                    } else {
                        Toast.makeText(this, "Detected more than one face, try again!", Toast.LENGTH_SHORT).show()
                        showPictureInImageView.setImageBitmap(takenImage)
                    }

                } else {
                    Toast.makeText(this, "No face were detected, try again!", Toast.LENGTH_SHORT).show()
                    showPictureInImageView.setImageBitmap(takenImage)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,"An error has ocurred: " + e, Toast.LENGTH_SHORT).show()
            }

        findEmotionButton.setOnClickListener {
            if (detectImage != null) {
                val result = detectEmotions(detectImage!!)
                var iterationNumber = 0

                for (emotionNumber in result) {
                    if (emotionNumber != 0F) {
                        resultTextView.isVisible = true
                        break
                    }
                    iterationNumber++
                }

                when (iterationNumber) {
                    0 -> {
                        resultTextView.setText("Wykryta emocja: złość")
                    }
                    1 -> {
                        resultTextView.setText("Wykryta emocja: zniesmaczenie")
                    }
                    2 -> {
                        resultTextView.setText("Wykryta emocja: strach")
                    }
                    3 -> {
                        resultTextView.setText("Wykryta emocja: radość")
                    }
                    4 -> {
                        resultTextView.setText("Wykryta emocja: emocja neutralna")
                    }
                    5 -> {
                        resultTextView.setText("Wykryta emocja: smutek")
                    }
                    6 -> {
                        resultTextView.setText("Wykryta emocja: zaskoczenie")
                    }
                    else -> {
                        Toast.makeText(this, "Not detected any emotions!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Cannot detect emotions :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toGrayscale(bmpOriginal: Bitmap): Bitmap? {
        val width: Int
        val height: Int
        height = bmpOriginal.height
        width = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val f = ColorMatrixColorFilter(cm)
        paint.setColorFilter(f)
        c.drawBitmap(bmpOriginal, 0F, 0F, paint)
        return bmpGrayscale
    }

    private fun detectEmotions(image: Bitmap) : FloatArray {
        val model = Model.newInstance(this)
        val rows = 48
        val columns = 48

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, rows, columns, 1), DataType.FLOAT32)
        val byteBuffer = ByteBuffer.allocateDirect(4 * rows * columns * 1)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(rows * columns)
        image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

        var pixel = 0
        for (i in 0 until rows)
        {
            for(j in 0 until columns)
            {
                val pixelValue = intValues[pixel++]
                byteBuffer.putFloat((pixelValue and 0xFF) / (1f / 255f))
            }
        }

        inputFeature0.loadBuffer(byteBuffer)
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

        model.close()
        return outputFeature0
    }
}
