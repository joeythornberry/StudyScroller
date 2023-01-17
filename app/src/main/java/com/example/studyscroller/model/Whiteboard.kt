package com.example.studyscroller.model

import android.graphics.Bitmap
import com.example.studyscroller.switchSeparatorsInString

data class Whiteboard(
    var name: String,
    var path: String = "",
    var originalPath: String = "",
    var filename: String = "",
    var unalteredFilename: String = "",
    var bitmap: Bitmap? = null,
    var favorited: Boolean = false
) {

    fun changeName(newName: String) {
        name = newName
    }

    fun generateFilename(): String {
        var f = (if (originalPath == path) "" else switchSeparatorsInString(originalPath)) + "!" + (if (favorited) "favorited" else "unfavorited") + "?" + switchSeparatorsInString(path) + "," + name
        filename = f
        return f
    }
}