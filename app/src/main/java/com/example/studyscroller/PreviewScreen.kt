package com.example.studyscroller

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import com.example.studyscroller.model.Whiteboard

@Composable
fun PreviewScreen (
    previewWhiteboard: Whiteboard,
    toCreatorScreen: () -> Unit
) {
    Column {
        Row() {
            Button(onClick = { toCreatorScreen() }) {
                Text("<-")
            }
            Text(previewWhiteboard.name)
        }
        if (previewWhiteboard.bitmap != null) {
            TransformableImage(bitmap = previewWhiteboard.bitmap!!.asImageBitmap())
        } else {
            Text("Error")
        }
    }
}