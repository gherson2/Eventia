package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.repositories.ConectividadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ConectividadViewModel @Inject constructor(
    repositorio: ConectividadRepository
) : ViewModel() {
    // null significa que aún no se conoce el estado: evita un aviso falso al arrancar.
    val conectado = repositorio.conectado
        .map<Boolean, Boolean?> { it }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
