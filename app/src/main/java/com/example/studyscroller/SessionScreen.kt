package com.example.studyscroller
import android.content.ContentResolver
import android.media.ThumbnailUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.studyscroller.model.Whiteboard

@Composable
fun SessionScreen(whiteboards: MutableList<Whiteboard>, contentResolver: ContentResolver, viewModel: StudyScrollerViewModel, toCreatorScreen: () -> Unit, getString: (Int) -> String) {

    Column(modifier = Modifier) {

        Row(modifier = Modifier) {
            EndSessionButton(toCreatorScreen = { toCreatorScreen() }, viewModel = viewModel)
            EditSessionButton(toCreatorScreen = { toCreatorScreen() })
            SaveSessionButton(toCreatorScreen = { toCreatorScreen() }, viewModel = viewModel, path = getString(R.string.saved_sessions_path))
        }

        WhiteboardList(whiteboardList = whiteboards)
    }
}

@Composable
fun TransformableImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    //image, but you can zoom and scroll it

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }

    val transformation_state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        scale = scale.coerceIn(1f,10f)
        rotation += rotationChange
        offsetX += offsetChange.x * scale
        offsetY += offsetChange.y * scale
    }
    Image(
        bitmap,
        "error",
        Modifier
            //.width(1000.dp)
            //.height(1000.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
                detectDragGestures { change, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .transformable(state = transformation_state)
            .fillMaxSize()
    )
}

@Composable
fun WhiteboardCard(whiteboard: Whiteboard, modifier: Modifier = Modifier) {
    //name and transformable image

    Card(modifier = Modifier.padding(8.dp), elevation = 4.dp) {
        Column {
            Text (
                text = whiteboard.name,
                modifier = Modifier.padding(6.dp),
                style = MaterialTheme.typography.h6
            )
            TransformableImage (
                bitmap = whiteboard.bitmap!!.asImageBitmap(),
                )
        }
    }
}

@Composable
fun WhiteboardList(whiteboardList: MutableList<Whiteboard>?, modifier: Modifier = Modifier) {
    //scrolling list of whiteboard cards

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        if (whiteboardList != null) {
            for(whiteboard in whiteboardList) {
                WhiteboardCard(whiteboard = whiteboard)
            }
        }
    }
}

@Composable
fun EndSessionButton(toCreatorScreen: () -> Unit, viewModel: StudyScrollerViewModel) {
    //clears whiteboards
    //changes directory to Main/
    //exits to creator screen

    Button(onClick = {
        viewModel.clearSelectedWhiteboards()
        viewModel.revertToMainDirectory()
        toCreatorScreen()
    }) {
        Text("End Session")
    }
}

@Composable
fun EditSessionButton(toCreatorScreen: () -> Unit) {
    //exits to creator screen, right where you left it

    Button(onClick = {
        toCreatorScreen()
    }) {
        Text("Edit Session")
    }
}

@Composable
fun SaveSessionButton(toCreatorScreen: () -> Unit, viewModel: StudyScrollerViewModel, path: String) {
    //opens the saved session dialog
    //clears whiteboards
    //changes directory to given Saved Sessions path
    //exits to creator screen

    Button(onClick = {
        viewModel.openOrCloseNameSavedSessionDialog(whiteboards = viewModel.uiState.value.selectedWhiteboards)
        viewModel.clearSelectedWhiteboards()
        viewModel.changeDirectory(path)
        toCreatorScreen()
    }) {
        Text("Save Session")
    }
}