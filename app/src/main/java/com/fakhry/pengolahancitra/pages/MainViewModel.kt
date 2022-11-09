package com.fakhry.pengolahancitra.pages

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fakhry.pengolahancitra.helpers.pattern_recognition.RgbImageToHsv.manipulateHsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _activeBitmapState = MutableStateFlow<Bitmap?>(null)
    val activeBitmapState = _activeBitmapState.asStateFlow()

    private val _loadingState = MutableSharedFlow<Boolean>()
    val loadingState = _loadingState.asSharedFlow()

    private val _listChangesToUndo = arrayListOf<Bitmap>()
    private val _listChangesToRedo = arrayListOf<Bitmap>()

    val isButtonUndoEnabled = MutableStateFlow(false)
    val isButtonRedoEnabled = MutableStateFlow(false)


    private fun addUndo(bitmap: Bitmap, shouldClearRedo: Boolean = false) {
        if (shouldClearRedo) clearRedoChanges()
        _listChangesToUndo.add(bitmap)
        isButtonUndoEnabled.value = true
    }

    fun undoChanges() {
        val lastUndoBitmap = _listChangesToUndo.lastOrNull()
        val currentActiveBitmap = _activeBitmapState.value
        if (lastUndoBitmap != null) {
            _activeBitmapState.value = lastUndoBitmap

            _listChangesToUndo.removeLast()
        } else {
            _activeBitmapState.value = null
            isButtonUndoEnabled.value = false
        }

        if (currentActiveBitmap != null) addRedo(currentActiveBitmap)
    }

    private fun addRedo(bitmap: Bitmap) {
        _listChangesToRedo.add(bitmap)
        isButtonRedoEnabled.value = true
    }

    fun redoChanges() {
        val lastRedoBitmap = _listChangesToRedo.lastOrNull()
        val currentActiveBitmap = _activeBitmapState.value
        if (lastRedoBitmap != null) {
            _activeBitmapState.value = lastRedoBitmap

            _listChangesToRedo.removeLast()
            isButtonRedoEnabled.value = _listChangesToRedo.isNotEmpty()
        }

        if (currentActiveBitmap != null) addUndo(currentActiveBitmap)
        else isButtonUndoEnabled.value = true
    }

    private fun clearRedoChanges() {
        _listChangesToRedo.clear()
        isButtonRedoEnabled.value = false
    }

    fun setBitmap(bitmap: Bitmap) {
        isButtonUndoEnabled.value = true
        _activeBitmapState.value?.let { addUndo(it) }
        _activeBitmapState.value = bitmap
        clearRedoChanges()
    }

    fun updateBitmapToHsv(usingLibrary: Boolean = false) {
        val currentActiveBitmap = _activeBitmapState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(true)
            addUndo(currentActiveBitmap, true)
            _activeBitmapState.value = currentActiveBitmap.manipulateHsv(usingLibrary)
            _loadingState.emit(false)
        }
    }
}