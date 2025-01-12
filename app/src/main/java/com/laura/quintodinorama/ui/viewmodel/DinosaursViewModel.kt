package com.laura.quintodinorama.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DinosaursViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dinosaurs Fragment"
    }
    val text: LiveData<String> = _text
}