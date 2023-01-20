package com.example.studyscroller

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.studyscroller.model.Whiteboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StudyScrollerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StudyScrollerUiState())
    val uiState: StateFlow<StudyScrollerUiState> = _uiState.asStateFlow()

    fun changeDirectory(newDirectory: String) {
        //change directory to given directory

        _uiState.update { currentState ->
            currentState.copy(currentDirectory = newDirectory)
        }
    }

    fun lastDirectory() {
        //cuts last folder off of current directory and sends you to the shortened path

        var newDirectory = ""
        var directory = ""
        for (char in _uiState.value.currentDirectory) {
            if (char == '/') {
                newDirectory += directory
                newDirectory += "/"
                directory = ""
            } else {
                directory += char
            }
        }
        newDirectory = newDirectory.removeSuffix("/")

        if (newDirectory != "") {
            _uiState.update { currentState ->
                currentState.copy(currentDirectory = newDirectory)
            }
        }
    }

    fun openOrCloseNewFolderDialog() {
        //open or close dialog that names and creates new folder

        val opposite = !_uiState.value.openNameFolderDialog
        _uiState.update { currentState ->
            currentState.copy(openNameFolderDialog = opposite)
        }
    }

    fun openOrCloseNewWhiteboardDialog() {
        //opens or closes dialog that names and creates new whiteboard

        val opposite = !_uiState.value.openNameWhiteboardDialog
        _uiState.update { currentState ->
            currentState.copy(openNameWhiteboardDialog = opposite)
        }
    }

    fun openOrCloseEditOrDeleteFolderDialog(index: Int?) {
        //opens or closes dialog that edits or deletes folder at given index in current directory
        //EDIT NOT IMPLEMENTED

        _uiState.update { currentState ->
            currentState.copy(openEditOrDeleteFolderDialog = index)
        }
    }

    fun openOrCloseEditOrDeleteWhiteboardDialog(index: Int?) {
        //opens or closes dialog that edits or deletes whiteboard at given index in current directory

        _uiState.update { currentState ->
            currentState.copy(openEditOrDeleteWhiteboardDialog = index)
        }
    }

    fun openOrCloseNameSavedSessionDialog(whiteboards: MutableList<Whiteboard>?) {
        //opens dialog that names and saves a folder containing copies of all currently selected images into Saved Sessions
        _uiState.update { currentState ->
            currentState.copy(openNameSavedSessionDialog = whiteboards)
        }
    }

    fun selectOrDeselectWhiteboard(whiteboard: Whiteboard) {
        //add or remove a whiteboard from selected whiteboards

        if(whiteboard !in _uiState.value.selectedWhiteboards) {
                _uiState.value.selectedWhiteboards.add(whiteboard)
        } else if(whiteboard in _uiState.value.selectedWhiteboards) {
            _uiState.value.selectedWhiteboards.remove(whiteboard)
        }
    }

    fun clearSelectedWhiteboards() {
        //empty the list of selected whiteboards

        _uiState.update { currentState ->
            currentState.copy(selectedWhiteboards = mutableListOf())
        }
    }

    fun revertToMainDirectory() {
        //change current directory to Main

        _uiState.update { currentState ->
            currentState.copy(currentDirectory = "Main")
        }
    }

    fun setPreviewWhiteboard(whiteboard: Whiteboard?) {
        //sets the preview whiteboard to the current whiteboard

        _uiState.update { currentState ->
            currentState.copy(previewWhiteboard = whiteboard)
        }
    }

    fun reload() {
        //band aid for until i figure out how to update state properly
        //just tells creator screen to turn itself off and on again
        _uiState.update { currentState ->
        currentState.copy(reload = !currentState.reload)
        }
    }

    fun reloadWhiteboards() {
        //band aid for until i figure out how to update state properly
        //just tells creator screen to turn itself off and on again
        _uiState.update { currentState ->
            currentState.copy(reloadWhiteboards = !currentState.reloadWhiteboards)
        }
    }
}

data class StudyScrollerUiState(
    val currentDirectory: String = "Main",
    val openNameFolderDialog: Boolean = false,
    val openNameWhiteboardDialog: Boolean = false,
    val openEditOrDeleteWhiteboardDialog: Int? = null,
    val openEditOrDeleteFolderDialog: Int? = null,
    val openNameSavedSessionDialog: MutableList<Whiteboard>? = null,
    val selectedWhiteboards: MutableList<Whiteboard> = mutableListOf(),
    val previewWhiteboard: Whiteboard? = null,
    val reload: Boolean = false,
    val reloadWhiteboards: Boolean = false
)

