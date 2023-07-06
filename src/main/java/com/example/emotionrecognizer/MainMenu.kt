package com.example.emotionrecognizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

private const val FILE_NAME = "photo.jpg"
private const val REQUEST_CODE_TAKE_PICTURE = 10
private const val REQUEST_CODE_CHOOSE_PICTURE = 11
lateinit var photoFile: File


class MainMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        val takePictureButton = findViewById<Button>(R.id.takePictureButton)
        val pickImageFromPhoneMemoryButton = findViewById<Button>(R.id.pickImageFromPhoneMemoryButton)

        takePictureButton.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)

            val fileProvider = FileProvider.getUriForFile(this, "com.example.emotionrecognizer.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            if(takePictureIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE)
            } else {
                Toast.makeText(this, "Unable to open camera.", Toast.LENGTH_SHORT).show()
            }
        }

        pickImageFromPhoneMemoryButton.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissions(permissions, PERMISSION_CODE)
            } else {
                chooseImageGallery();
            }
        }
    }

    companion object {
        private val IMAGE_CHOOSE = REQUEST_CODE_CHOOSE_PICTURE;
        private val PERMISSION_CODE = 1001;
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CHOOSE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    chooseImageGallery()
                }else{
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_CODE_TAKE_PICTURE && resultCode == Activity.RESULT_OK) {

            val intent = Intent(this, DetectEmotion::class.java)
            intent.putExtra("imagePath", photoFile.absolutePath)
            this.startActivity(intent)

        } else if (requestCode == REQUEST_CODE_CHOOSE_PICTURE && resultCode == Activity.RESULT_OK){
            val filePath: String? = data?.data?.let { PathUtil.getPath(this, it) }

            val intent = Intent(this, DetectEmotion::class.java)
            intent.putExtra("imagePath", filePath)
            this.startActivity(intent)

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}