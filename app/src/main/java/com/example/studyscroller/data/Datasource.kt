package com.example.studyscroller.data
import com.example.studyscroller.model.Folder
import com.example.studyscroller.model.Whiteboard
import java.io.File
import java.io.FileOutputStream

val foldersFile = "Folders.txt"
//name of the file that keeps track of folders
var foldersMap = mutableMapOf<String,MutableList<Folder>>()
var whiteboardsMap = mutableMapOf<String,MutableList<Whiteboard>>()
//maps that map locations to mutable lists of folders and images



fun saveFolders(fileOutputStream: FileOutputStream) {
    //get string containing information for a folder on each line and it to given output stream

    val data = gatherFoldersToSave()
    fileOutputStream.write(data.toByteArray())
}

fun gatherFoldersToSave(): String {
    //write each folder's information to a line of a string and return it

    var data = ""
    for(location in foldersMap.keys) {
        for(folder in foldersMap[location]!!) {
            var line = ""
            line += folder.path
            line += ","
            line += folder.name
            line += ","
            line += "\n"
            data += line
        }
    }
    return data
}

    fun loadFolders(filesDir: File): MutableMap<String,MutableList<Folder>> {
        //go through folders file and make a Folder object for each line

        val map = mutableMapOf<String,MutableList<Folder>>()
        val f = File(filesDir, foldersFile)
        f.forEachLine {
            val parsedLine = parseCsvLine(it)
            val path = parsedLine[0]
            val name = parsedLine[1]
            if (map[path] == null) {
                map[path] = mutableListOf()
            }
            map[path]!!.add(Folder(name = name, path = path))
        }
        return map
    }

fun parseCsvLine(line: String): MutableList<String> {
    //helper function to parse a csv line into a list

    val list = mutableListOf<String>()
    var value = ""
    for(char in line) {
        if(char == ',') {
            list.add(value)
            value = ""
        } else {
            value += char
        }
    }
    return list
}