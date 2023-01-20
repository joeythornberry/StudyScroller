package com.example.studyscroller

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyscroller.data.foldersMap
import com.example.studyscroller.data.whiteboardsMap
import com.example.studyscroller.model.Whiteboard
import com.example.studyscroller.model.Folder
import com.example.studyscroller.ui.theme.LongClickButton

/**CREATOR SCREEN**/
//handles and displays file explorer containing whiteboards uploaded by the user
//user selects desired whiteboards, and navigates to session screen, where whiteboards will be displayed

@Composable
fun CreatorScreen(
    modifier: Modifier = Modifier,
    viewModel: StudyScrollerViewModel,
    contentResolver: ContentResolver,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSessionButtonClicked: () -> Unit,
    onReloadCreator: () -> Unit,
    hoistedSaveFolders: () -> Unit,
    savePhotoToInternalStorage: (String,Bitmap) -> Unit,
    deletePhotoFromInternalStorage: (String) -> Unit
) {
    Column(
        modifier = Modifier
    ) {
        //for each possible dialog, check if it should be open, and if so display it
        if (viewModel.uiState.collectAsState().value.openNameFolderDialog) {
            NameAndCreateNewFolderDialog(viewModel = viewModel, hoistedSaveFolders = { hoistedSaveFolders() })
        }
        if (viewModel.uiState.collectAsState().value.openNameWhiteboardDialog) {
            NameAndCreateNewWhiteboardDialog(viewModel = viewModel, savePhotoToInternalStorage = {s,b -> savePhotoToInternalStorage(s,b)})
        }

        if (viewModel.uiState.collectAsState().value.openEditOrDeleteFolderDialog != null) {
            EditOrDeleteFolderDialog(viewModel = viewModel, index = viewModel.uiState.collectAsState().value.openEditOrDeleteFolderDialog!!,saveFolders = { hoistedSaveFolders() })
        }

        if (viewModel.uiState.collectAsState().value.openEditOrDeleteWhiteboardDialog != null) {
            EditOrDeleteWhiteboardDialog(viewModel = viewModel, index = viewModel.uiState.collectAsState().value.openEditOrDeleteWhiteboardDialog!!, savePhotoToInternalStorage = { s,b -> savePhotoToInternalStorage(s,b) }, deletePhotoFromInternalStorage = {filename -> deletePhotoFromInternalStorage(filename)})
        }

        if(viewModel.uiState.collectAsState().value.openNameSavedSessionDialog != null) {
            NameSavedSessionDialog(viewModel = viewModel, whiteboards = viewModel.uiState.collectAsState().value.openNameSavedSessionDialog!!, saveFolders = { hoistedSaveFolders() }, savePhotoToInternalStorage = {s,b -> savePhotoToInternalStorage(s,b)})
        }

        Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            CreateFolderButton(viewModel = viewModel, modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
            TakePhotoButton(onTakePhoto = { onTakePhoto() }, viewModel = viewModel, modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
            PickPhotoButton(onPickPhoto = { onPickPhoto() }, viewModel = viewModel, modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
        }
        ToSessionButton(modifier = Modifier
            .width(50.dp)
            .height(50.dp)
            ,onSessionButtonClicked = { onSessionButtonClicked() })
        Row (modifier = Modifier) {
            LastDirectoryButton(viewModel = viewModel)
            SelectAllButton(viewModel = viewModel)
        }
        SavedSessionsButton(onSavedSessionsButtonClicked = { viewModel.changeDirectory("Main/Saved Sessions") })
        FavoritesButton(onFavoritesButtonClicked = { viewModel.changeDirectory("Main/Favorites") })
        Text(viewModel.uiState.collectAsState().value.currentDirectory)
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.weight(2f)
            ) {
                if (!viewModel.uiState.collectAsState().value.reload) {
                    CreatorWhiteboardAndFolderList(viewModel = viewModel, directory = viewModel.uiState.collectAsState().value.currentDirectory, savePhotoToInternalStorage = {s,b -> savePhotoToInternalStorage(s,b)},
                        deletePhotoFromInternalStorage = {s -> deletePhotoFromInternalStorage(s)}, onReloadCreator = {onReloadCreator()})
                } else {
                    viewModel.reload()
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WhiteboardPreview(previewWhiteboard = viewModel.uiState.collectAsState().value.previewWhiteboard, contentResolver)
            }
        }
    }
}

/**CREATOR SCREEN FILE EXPLORER**/
//scrolling list of all folders and whiteboards in current directory
//each has an edit button that can change its name or delete it
//when folders are clicked they change the current directory
//when whiteboards are clicked they are added to the current session

@Composable
fun CreatorWhiteboardAndFolderList(
    viewModel: StudyScrollerViewModel,
    directory: String,
    savePhotoToInternalStorage: (String, Bitmap) -> Unit,
    deletePhotoFromInternalStorage: (String) -> Unit,
    onReloadCreator: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
) {
    //finds the current directory as recorded by uiState
    //displays scrolling list of all folders and then all whiteboards in current directory

    val directory = viewModel.uiState.collectAsState().value.currentDirectory

    var listOfWhiteboards = mutableListOf<Whiteboard>()

    //if current directory happens to be the favorites directory, just give a list of all the favorited whiteboards
    if (directory == "Main/Favorites") {
        for (list in whiteboardsMap.values) {
            for (whiteboard in list) {
                if (whiteboard.favorited) {
                    listOfWhiteboards.add(whiteboard)
                }
            }
        }
    } else {
        //else give a list of all whiteboards in current directory
        if (whiteboardsMap[directory] != null) {
            if (!viewModel.uiState.collectAsState().value.reloadWhiteboards) {
                listOfWhiteboards = whiteboardsMap[directory]!!
            } else {
                viewModel.reloadWhiteboards()
            }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        if (foldersMap[directory] != null) {
            CreatorFolderList(
                Folders = foldersMap[directory]!!,
                viewModel = viewModel,
                onReloadCreator = { onReloadCreator() }
            )
        }

        CreatorWhiteboardList(listOfWhiteboards, viewModel, savePhotoToInternalStorage = {s,b -> savePhotoToInternalStorage(s,b)},
                deletePhotoFromInternalStorage = {s -> deletePhotoFromInternalStorage(s)})
    }
}

@Composable
fun CreatorFolderList(
    Folders: MutableList<Folder>,
    viewModel: StudyScrollerViewModel,
    onReloadCreator: () -> Unit
) {
    //list of all folders given to it by parent CreatorFolderAndWhiteboardList
    //tells each folder that when clicked on it should turn off the whiteboard preview and change the current directory to itself

    Column(
        modifier = Modifier
    ) {
        for (Folder in Folders) {
            CreatorFolder(
                folder = Folder,
                onDirectoryChange = {
                    viewModel.setPreviewWhiteboard(null)
                    viewModel.changeDirectory(Folder.givePath())
                    onReloadCreator()
                    viewModel.reloadWhiteboards()
                },
                viewModel = viewModel,
            )
        }
    }
}

@Composable
fun CreatorWhiteboardList(
    whiteboards: MutableList<Whiteboard>,
    viewModel: StudyScrollerViewModel,
    savePhotoToInternalStorage: (String, Bitmap) -> Unit,
    deletePhotoFromInternalStorage: (String) -> Unit
) {
    //list of all whiteboards given to it by parent CreatorFolderAndWhiteboardList

    Column(
        modifier = Modifier
    ) {
        for (whiteboard in whiteboards) {
            CreatorWhiteboard(whiteboard, viewModel, savePhotoToInternalStorage = {s,b -> savePhotoToInternalStorage(s,b)}, deletePhotoFromInternalStorage = {s -> deletePhotoFromInternalStorage(s)})
        }
    }
}

@Composable
fun CreatorFolder(
    folder: Folder,
    onDirectoryChange: () -> Unit,
    viewModel: StudyScrollerViewModel
) {
    //represents a folder in the creator screen file explorer
    //edit button tells viewModel to open the folder edit dialog for the folder at this position in the current directory
    //when clicked tells parent CreatorFolderList to change the current directory to itself
    //displays its name

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        CreatorEditButton(onEditButtonClicked = {
            viewModel.openOrCloseEditOrDeleteFolderDialog(foldersMap[viewModel.uiState.value.currentDirectory]?.indexOf(folder))
        })

        Button(
            onClick = {
                onDirectoryChange()
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(folder.name + "/")
            }
        }
    }
}

@Composable
fun CreatorWhiteboard(whiteboard: Whiteboard, viewModel: StudyScrollerViewModel, savePhotoToInternalStorage: (String, Bitmap) -> Unit,deletePhotoFromInternalStorage: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        //represents a whiteboard in the creator screen file explorer
        //edit button tells viewModel to open the whiteboard edit dialog for the whiteboard at this position in the current directory
        //check box shows if this whiteboard is selected for current session
        //when short clicked adds or removes whiteboard from current session, and updates check box accordingly
        //border is gold if whiteboard is currently marked as a favorite, and black if it isn't
        //displays its name, preceded by its path if the directory it's in isn't the one it was born in
        //when clicked or long clicked sets preview whiteboard to itself

        CreatorEditButton(onEditButtonClicked = {
            viewModel.openOrCloseEditOrDeleteWhiteboardDialog(whiteboardsMap[viewModel.uiState.value.currentDirectory]?.indexOf(whiteboard))
        })

        var checked by remember { mutableStateOf(whiteboard in viewModel.uiState.value.selectedWhiteboards) }

        var favorited by remember { mutableStateOf(whiteboard.favorited) }

        LongClickButton(
            onClick = {
                viewModel.selectOrDeselectWhiteboard(whiteboard)
                checked = whiteboard in viewModel.uiState.value.selectedWhiteboards
                viewModel.setPreviewWhiteboard(whiteboard)
            },
            onLongClick = {
                whiteboard.favorited = !whiteboard.favorited
                favorited = whiteboard.favorited

                deletePhotoFromInternalStorage(whiteboard.unalteredFilename!!)
                val filename = whiteboard.generateFilename()
                whiteboard.unalteredFilename = filename + ".jpg"
                whiteboard.bitmap?.let {
                    savePhotoToInternalStorage(
                        filename,
                        it
                    )
                }
                viewModel.setPreviewWhiteboard(whiteboard)
            },
            modifier = Modifier
                .border(width = 1.dp, color = if (favorited) Color.Yellow else Color.Black)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {


                val maybePath = if (whiteboard.path != viewModel.uiState.value.currentDirectory) whiteboard.path + "/" else if (whiteboard.originalPath != "" && whiteboard.originalPath != viewModel.uiState.value.currentDirectory) whiteboard.originalPath + "/" else ""
                Text(
                    text = maybePath + whiteboard.name,
                    fontSize = 20.0.sp,
                    modifier = Modifier
                )

                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        viewModel.selectOrDeselectWhiteboard(whiteboard)
                        checked = !checked
                    },
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun CreatorEditButton(
    modifier: Modifier = Modifier,
    onEditButtonClicked: () -> Unit
) {
    //each folder or whiteboard gets one of these
    //they tell it what to do when clicked
    //labelled "Edit"
    Button(onClick = { onEditButtonClicked() }) {
        Text("Edit")
    }
}
/**END OF CREATOR SCREEN FILE EXPLORER**/


/**DIALOGS**/
//pop ups that ask for user input
//uiState has a boolean for each one that tells it if it should show up or not
//composables use viewModel functions to turn them on
//they'll turn themselves off when the user is done with them
//DO NOT ACCOUNT FOR USER INPUT CONTAINING DISALLOWED CHARACTERS

@Composable
fun NameAndCreateNewFolderDialog(viewModel: StudyScrollerViewModel, hoistedSaveFolders: () -> Unit) {
    //create folder turns it on
    //asks for a name for the new folder
    //creates a folder with that name in the current directory
    //updates file that stores the names and locations of folders

    var name by remember { mutableStateOf("") }
    val directory = viewModel.uiState.collectAsState().value.currentDirectory

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Name Folder")
        },
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        if (!validateFileName(name, context)) {
                        } else {
                            if (foldersMap[directory] == null) {
                                foldersMap[directory] = mutableListOf()
                            }
                            foldersMap[directory]!!.add(
                                Folder(
                                    name = name.trim(),
                                    path = directory
                                )
                            )
                            hoistedSaveFolders()
                            viewModel.openOrCloseNewFolderDialog()
                        }
                    }
                ) {
                    Text("Create Folder")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        viewModel.openOrCloseNewFolderDialog()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun NameAndCreateNewWhiteboardDialog(viewModel: StudyScrollerViewModel,
                                   savePhotoToInternalStorage: (String,Bitmap) -> Unit
) {
    //pickImage turns it on
    //asks for a name for the new whiteboard
    //once the pickImage system produces a bitmap, this creates a whiteboard with that bitmap and the chosen name
    //saves whiteboard as file with bitmap as body and path,name as filename

    var name by remember { mutableStateOf("") }
    val directory = viewModel.uiState.collectAsState().value.currentDirectory

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Name Image")
        },
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        if(!validateFileName(name,context)) {
                        } else if (pickedImageBitmap != null) {
                            if (whiteboardsMap[directory] == null) {
                                whiteboardsMap[directory] = mutableListOf()
                            }
                            val whiteboard = Whiteboard(
                                name = name,
                                bitmap = pickedImageBitmap,
                                path = generateDirectoryPath(viewModel),
                                originalPath = generateDirectoryPath(viewModel),
                                favorited = false,
                            )
                            whiteboardsMap[directory]!!.add(
                                whiteboard
                            )
                            val filename = whiteboard.generateFilename()
                            whiteboard.unalteredFilename = filename + ".jpg"
                            savePhotoToInternalStorage(
                                filename,
                                whiteboard.bitmap!!
                            )
                            pickedImageBitmap = null
                            viewModel.openOrCloseNewWhiteboardDialog()
                        }
                    }
                ) {
                    Text("Save Image")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        viewModel.openOrCloseNewWhiteboardDialog()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun EditOrDeleteFolderDialog(viewModel: StudyScrollerViewModel, index: Int, saveFolders: () -> Unit) {
    //folder's edit button turns it on
    //renames or deletes folder as directed by user
    //updates file with list of folders
    //RENAMING NOT IMPLEMENTED BC REFACTORING ALL CONTENTS WOULD BE ABSURDLY INVOLVED

    var name by remember { mutableStateOf("") }
    val directory = viewModel.uiState.collectAsState().value.currentDirectory

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = name)
        },
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        //this would require refactoring the entire directory system aaaaaa
                        /**
                        foldersMap[directory]?.get(index)?.changeName(name)

                        saveFolders()
                        viewModel.openOrCloseEditOrDeleteFolderDialog(null)
                        **/
                    }
                ) {
                    Text("Rename Folder")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        viewModel.openOrCloseEditOrDeleteFolderDialog(null)
                    }
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        foldersMap[directory]?.removeAt(index)
                        saveFolders()
                        viewModel.openOrCloseEditOrDeleteFolderDialog(null)
                    },
                    modifier = Modifier
                ) {
                    Text("Delete Folder")
                }
            }
        }
    )
}

@Composable
fun EditOrDeleteWhiteboardDialog(viewModel: StudyScrollerViewModel, index: Int, savePhotoToInternalStorage: (String, Bitmap) -> Unit, deletePhotoFromInternalStorage: (String) -> Unit) {
    //whiteboard's edit button turns it on
    //button to delete whiteboard
    //change name button deletes whiteboard and creates new one with new name

    var name by remember { mutableStateOf("") }
    val directory = viewModel.uiState.collectAsState().value.currentDirectory

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = whiteboardsMap[directory]?.get(index)?.name!!)
        },
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        if (!validateFileName(name, context)) {
                        } else {
                            val unalteredFilename =
                                whiteboardsMap[directory]?.get(index)?.unalteredFilename
                            if (unalteredFilename != null) {
                                deletePhotoFromInternalStorage(unalteredFilename)
                            }
                            whiteboardsMap[directory]?.get(index)?.changeName(name)
                            val whiteboard = whiteboardsMap[directory]?.get(index)
                            if (whiteboard != null) {
                                //let call is because whiteboard.bitmap is non null and it needs to not be
                                //don't ask me how it helps
                                val filename = whiteboard.generateFilename()
                                whiteboard.unalteredFilename = filename + ".jpg"
                                whiteboard.bitmap?.let {
                                    savePhotoToInternalStorage(
                                        filename,
                                        it
                                    )
                                }
                            }
                            viewModel.openOrCloseEditOrDeleteWhiteboardDialog(null)
                        }
                    }
                ) {
                    Text("Rename Image")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        viewModel.openOrCloseEditOrDeleteWhiteboardDialog(null)
                    }
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                            val unalteredFilename =
                                whiteboardsMap[directory]?.get(index)?.unalteredFilename
                            if (unalteredFilename != null) {
                                deletePhotoFromInternalStorage(unalteredFilename)
                            }
                            whiteboardsMap[directory]?.removeAt(index)
                            viewModel.openOrCloseEditOrDeleteWhiteboardDialog(null)
                              },
                    modifier = Modifier
                ) {
                    Text("Delete Image")
                }
            }
        }
    )
}

@Composable
fun NameSavedSessionDialog(viewModel: StudyScrollerViewModel, whiteboards: MutableList<Whiteboard>, saveFolders: () -> Unit, savePhotoToInternalStorage: (String, Bitmap) -> Unit) {
    //save session button on session screen calls it
    //asks for a name for the new saved session
    //creates folder in Main/Saved Sessions with that name and containing all images in current session
    //updates file with list of folders

    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Name Saved Session")
        },
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        val savedSessionsPath = "Main/Saved Sessions"

                        if (foldersMap[savedSessionsPath] == null) {
                            foldersMap[savedSessionsPath] = mutableListOf()
                        }
                        foldersMap[savedSessionsPath]!!.add(Folder(name = name, path = savedSessionsPath))

                        val directory = savedSessionsPath + "/" + name.trim()

                        whiteboardsMap[directory] = mutableListOf()
                        for (whiteboard in whiteboards) {
                            whiteboardsMap[directory]!!.add(Whiteboard(name = whiteboard.name, originalPath = whiteboard.path, path = directory, bitmap = whiteboard.bitmap))
                            savePhotoToInternalStorage(switchSeparatorsInString(whiteboard.path) + "!" + "unfavorited" + "?" + switchSeparatorsInString(directory) + "," + whiteboard.name, whiteboard.bitmap!!)
                        }
                       saveFolders()
                        viewModel.openOrCloseNameSavedSessionDialog(whiteboards = null)
                    }
                ) {
                    Text("Save Session")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        viewModel.openOrCloseNameSavedSessionDialog(whiteboards = null)
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**END OF DIALOGS**/


/**CREATOR SCREEN BUTTONS**/

@Composable
fun TakePhotoButton(onTakePhoto: () -> Unit, viewModel: StudyScrollerViewModel,modifier: Modifier = Modifier) {
    Row(modifier = Modifier, horizontalArrangement = Arrangement.Center) {
        Button(onClick = {
            onTakePhoto()
            viewModel.openOrCloseNewWhiteboardDialog()
        }) {
            Text("Take Photo")
        }
    }
}

@Composable
fun PickPhotoButton(onPickPhoto: () -> Unit, viewModel: StudyScrollerViewModel,modifier: Modifier = Modifier) {
    Button(modifier = Modifier, onClick = { onPickPhoto()
        viewModel.openOrCloseNewWhiteboardDialog() }) {
        Text("Pick Photo")
    }
}

@Composable
fun CreateFolderButton(viewModel: StudyScrollerViewModel,modifier: Modifier = Modifier) {
    //tells viewModel to open new folder dialog

    Button(modifier = Modifier,onClick = {
        viewModel.openOrCloseNewFolderDialog()
    }) {
        Text("create folder")
    }
}

@Composable
fun ToSessionButton(
    onSessionButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    //tells parent CreatorScreen to navigate to session screen
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Button(onClick = { onSessionButtonClicked() }) {
            Text("to session")
        }
    }
}

@Composable
fun LastDirectoryButton(viewModel: StudyScrollerViewModel,modifier: Modifier = Modifier) {
    //tells viewModel to change current directory to the parent directory of the current directory

    Button(onClick = {
        viewModel.lastDirectory()
        viewModel.reloadWhiteboards()
    }) {
        Text("<-")
    }
}

@Composable
fun FavoritesButton(onFavoritesButtonClicked: () -> Unit, modifier: Modifier = Modifier) {
    //tells parent CreatorScreen to change current directory to the favorite whiteboards directory

    Button(onClick = { onFavoritesButtonClicked() }) {
        Text("Favorite Images")
    }
}

@Composable
fun SavedSessionsButton(onSavedSessionsButtonClicked: () -> Unit, modifier: Modifier = Modifier) {
    //tells parent CreatorScreen to change current directory to the saved sessions directory

    Button(onClick = { onSavedSessionsButtonClicked() }) {
        Text("Saved Sessions")
    }
}

@Composable
fun SelectAllButton(viewModel: StudyScrollerViewModel, modifier: Modifier = Modifier) {
    //selects or deselects each whiteboard in current directory
    //RELOADS THE SCREEN TO GET CHECK BOXES TO UPDATE (need to learn more about state updates)
    //displays "Select All" or "Deselect All"

    var select by remember { mutableStateOf(true) }
    Button( onClick = {
        if (select) {
            if (whiteboardsMap[viewModel.uiState.value.currentDirectory] != null) {
                for (whiteboard in whiteboardsMap[viewModel.uiState.value.currentDirectory]!!) {
                    if (whiteboard !in viewModel.uiState.value.selectedWhiteboards) {
                        viewModel.selectOrDeselectWhiteboard(whiteboard)
                    }
                }
            }
            select = false
        } else if (!select) {
            if (whiteboardsMap[viewModel.uiState.value.currentDirectory] != null) {
                for (whiteboard in whiteboardsMap[viewModel.uiState.value.currentDirectory]!!) {
                    if (whiteboard in viewModel.uiState.value.selectedWhiteboards) {
                        viewModel.selectOrDeselectWhiteboard(whiteboard)
                    }
                }
            }
            select = true
        }
        viewModel.reload()
    }) {
        if (select) {
            Text("Select All")
        } else if (!select) {
            Text("Deselect All")
        }
    }
}
/**END OF CREATOR SCREEN BUTTONS**/

/**CREATOR SCREEN PREVIEW**/
//if a whiteboard has been clicked since current directory has changed, display that whiteboard
//if not, display a random hint (like on the Minecraft loading screen) TO DO

@Composable
fun WhiteboardPreview(previewWhiteboard: Whiteboard?, contentResolver: ContentResolver) {
    if(previewWhiteboard != null) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(previewWhiteboard.name)
            if (previewWhiteboard.bitmap != null) {
                val thumbnail =
                    ThumbnailUtils.extractThumbnail(previewWhiteboard.bitmap!!, 500, 500)
                Image(thumbnail.asImageBitmap(), "preview")
            }
        }
    } else {
        Text("helpful tip")
    }
}
/**END OF CREATOR SCREEN PREVIEW**/

/**HELPER FUNCTIONS**/

fun generateDirectoryPath(viewModel: StudyScrollerViewModel): String {
//return current directory

    return viewModel.uiState.value.currentDirectory
}

fun switchSeparatorsInString(original: String): String {
//switch all /s with |, or vice versa
//(because filenames can't contain /s)
    var s = ""
    for (char in original) {
        if (char == '/') {
            s += "|"
        } else if (char == '|') {
            s += "/"
        }else {
            s += char
        }
    }
    return s
}

fun validateFileName (name: String, context: Context): Boolean {
    var illegalChar: Char? = null
    val illegalChars = listOf<Char>('/','\\','.','?','!',',','<','>','|','"','*')
    for (char in illegalChars) {
        if (char in name) {
            illegalChar = char
        }
    }
    if (name == "") {
        Toast.makeText(context,"Error: no name given",Toast.LENGTH_SHORT).show()
        return false
    } else if(illegalChar != null) {
        Toast.makeText(context,"Error: " + illegalChar + " is not allowed in a file name",Toast.LENGTH_SHORT).show()
        return false
    } else {
        return true
    }
}

/**
@Preview
@Composable
fun CreatorScreenPreview() {
     CreatorScreen(
         viewModel = StudyScrollerViewModel(),
         onTakePhoto = { /*TODO*/ },
         onPickPhoto = { /*TODO*/ },
         onSessionButtonClicked = { /*TODO*/ },
         onReloadCreator = { /*TODO*/ },
         hoistedSaveFolders = { /*TODO*/ },
         savePhotoToInternalStorage = {s,b -> },
         deletePhotoFromInternalStorage = {s ->}
     )
}
        **/