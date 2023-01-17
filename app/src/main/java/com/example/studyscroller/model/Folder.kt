package com.example.studyscroller.model

data class Folder(
    var name: String,
    var path: String
) {

    fun changeName(newName: String) {
        name = newName
    }

    fun givePath(): String {
        return path + "/" + name
    }
}