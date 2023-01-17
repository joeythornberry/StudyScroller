package com.example.studyscroller

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.studyscroller.data.*
import com.example.studyscroller.model.Whiteboard
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.io.IOException
import java.io.InputStream

var pickedImageBitmap: Bitmap? = null
//stores bitmap of the picture chosen by take or pick image, so that the name whiteboard dialog can use it

class MainActivity : AppCompatActivity() {

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    //variables to keep track of what permissions we have and what permissions we need to get

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            pickedImageBitmap = it
            Log.d(it.toString(),"output_image_pog")
            //val isSavedSuccessfully = when {
               // writePermissionGranted -> savePhotoToExternalStorage(UUID.randomUUID().toString(), it!!)
                //else -> false
            //}
        } //when launched by the take picture button, has the user take a picture, and stores its bitmap in pickedImageBitmap
        //ONLY WORKS TO GET A PREVIEW OF THE IMAGE, TRYING TO STEAL CODE FOR A CUSTOM ActivityResultLauncher TO FIX THIS

        val pickPhoto = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback {
                if (it != null) {
                    val input: InputStream? =
                        it.let { contentResolver.openInputStream(it) }
                    pickedImageBitmap = BitmapFactory.decodeStream(input).copy(Bitmap.Config.ARGB_8888, true)
                }
            },
        ) //when launched by the pick image button, has the user pick an image, and stores its bitmap in pickedImageBitmap

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
        } //see if we have the permissions we need
        updateOrRequestPermissions()
        //ask for any we don't have

        setContent {
            if(File(filesDir, foldersFile).exists()) {
                foldersMap = loadFolders(filesDir)
            } //load the FoldersFile into a dictionary of which folders can be accessed through which
            
            whiteboardsMap = loadPhotosFromInternalStorage()
            //load whiteboards and put them in a dictionary of which whiteboards are in which folder

            //val randomUri = getRandomUri(this@MainActivity, ".jpg")
            StudyScrollerApp(onTakePhoto = { takePhoto.launch() }, onPickPhoto = { pickPhoto.launch("image/*") })
            //launch the app and tell it how to take or pick images
        }
    }

    private fun updateOrRequestPermissions() {
        //find out what permissions are needed and ask for them

        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()
        if(!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadPhotosFromInternalStorage(): MutableMap<String,MutableList<Whiteboard>> {
        //load whiteboards from their own individual files into a map of directory to list of whiteboards

            val map = mutableMapOf<String, MutableList<Whiteboard>>()
            val files = filesDir.listFiles()
            for (file in files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg")}!!) {
                val bytes = file.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                var originalPath = file.name.substringBefore("!")
                var favorited = file.name.substringBefore('?').substringAfter("!")
                val path = switchSeparatorsInString(file.name.substringBefore(',').substringAfter("?"))
                val name = file.name.substringAfter(',').substringBefore(".")
                if (map[path] == null) {
                    map[path] = mutableListOf()
                }
                map[path]!!.add(Whiteboard(name = name, path = path,bitmap =  bmp, filename = file.name, unalteredFilename = file.name, originalPath = if (originalPath == "") path else originalPath,favorited = if (favorited == "favorited") true else false))
            }
        return map
    }


    fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        //save whiteboard bitmap to file with filename containing metadata

        return try {
            openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun deletePhotoFromInternalStorage(filename: String): Boolean {
        //delete whiteboard file

        return try {
            deleteFile(filename)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap?): Boolean {
        //save photo in place where other apps can see it

        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp!!.width)
            put(MediaStore.Images.Media.HEIGHT, bmp!!.height)
        }
        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if(!bmp!!.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

    @Composable
    fun StudyScrollerApp(
                        modifier: Modifier = Modifier,
                        onTakePhoto: () -> Unit,
                        onPickPhoto: () -> Unit,
                        studyScrollerViewModel: StudyScrollerViewModel = viewModel()) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "creator",
            modifier = Modifier
        ) {
            composable(route = "creator") {
                CreatorScreen(
                    viewModel = studyScrollerViewModel,
                    contentResolver = contentResolver,
                    onTakePhoto = { onTakePhoto() },
                    onPickPhoto = { onPickPhoto() },
                    onSessionButtonClicked = { navController.navigate("session") },
                    hoistedSaveFolders = {
                        val fileOutputStream: FileOutputStream =
                            openFileOutput(foldersFile, ComponentActivity.MODE_PRIVATE)
                        saveFolders(fileOutputStream)
                    },
                    savePhotoToInternalStorage = { s,b -> savePhotoToInternalStorage(s,b) },
                    deletePhotoFromInternalStorage = {s -> deletePhotoFromInternalStorage(s)},
                    onReloadCreator = { }
                )
            }

            composable(route = "session") {
                SessionScreen(
                    whiteboards = studyScrollerViewModel.uiState.collectAsState().value.selectedWhiteboards,
                    contentResolver = contentResolver,
                    toCreatorScreen = { navController.navigate("creator") },
                    viewModel = studyScrollerViewModel,
                    getString = { getString(it) }
                )
            }
        }
    }
}

inline fun <T> sdk29AndUp(onSdk29: () -> T): T? {
    //call given function only if sdk level is over 29

    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29()
    } else null
}