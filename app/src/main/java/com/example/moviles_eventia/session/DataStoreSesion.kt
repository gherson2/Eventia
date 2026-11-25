package com.example.moviles_eventia.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class DataStoreSesion @Inject constructor(
    private val datos: DataStore<Preferences>
) : SesionStore {
    private val tokenKey = stringPreferencesKey("token")
    private val idKey = intPreferencesKey("usuario_id")
    private val nombreKey = stringPreferencesKey("nombre")
    private val correoKey = stringPreferencesKey("correo")
    private val expiraciones = Channel<Unit>(Channel.CONFLATED)
    override val expirada = expiraciones.receiveAsFlow()
    override val sesion = datos.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        val token = prefs[tokenKey]?.takeIf { it.isNotBlank() }
        val id = prefs[idKey]
        if (token == null || id == null || id <= 0) null
        else Sesion(token, Usuario(id, prefs[nombreKey].orEmpty(), prefs[correoKey].orEmpty()))
    }

    override suspend fun guardar(sesion: Sesion) {
        require(sesion.token.isNotBlank() && sesion.usuario.id > 0)
        datos.edit {
            it[tokenKey] = sesion.token
            it[idKey] = sesion.usuario.id
            it[nombreKey] = sesion.usuario.nombre
            it[correoKey] = sesion.usuario.correo
        }
    }

    override suspend fun cerrar() { datos.edit { it.clear() } }

    override suspend fun expirarSiCoincide(token: String) {
        var borrada = false
        datos.edit {
            if (it[tokenKey] == token) {
                it.clear()
                borrada = true
            }
        }
        if (borrada) expiraciones.send(Unit)
    }

    override suspend fun actualizarUsuarioSiCoincide(token: String, usuario: Usuario) {
        datos.edit {
            if (it[tokenKey] == token) {
                it[idKey] = usuario.id
                it[nombreKey] = usuario.nombre
                it[correoKey] = usuario.correo
            }
        }
    }
}
