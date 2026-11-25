package com.example.moviles_eventia.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSesionTest {
    @get:Rule val carpeta = TemporaryFolder()
    private val usuario = Usuario(7, "Ana", "ana@example.test")
    private val sesion = Sesion("token-solo-de-prueba", usuario)

    @Test fun sesionPersisteAlRecrearDataStoreSinGuardarLaContrasena() = runTest {
        val archivo = carpeta.newFile("sesion.preferences_pb")
        val primero = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val datos = PreferenceDataStoreFactory.create(scope = primero, produceFile = { archivo })
            val almacen = DataStoreSesion(datos)
            almacen.guardar(sesion)
            assertEquals(sesion, almacen.sesion.first())
            assertEquals(setOf("token", "usuario_id", "nombre", "correo"),
                datos.data.first().asMap().keys.map { it.name }.toSet())
        } finally {
            primero.cancel()
            primero.coroutineContext.job.join()
        }
        val segundo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val datos = PreferenceDataStoreFactory.create(scope = segundo, produceFile = { archivo })
            assertEquals(sesion, DataStoreSesion(datos).sesion.first())
        } finally {
            segundo.cancel()
            segundo.coroutineContext.job.join()
        }
    }

    @Test fun expiracionAntiguaNoBorraUnaSesionNueva() = runTest {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val archivo = carpeta.newFile("expiracion.preferences_pb")
            val datos = PreferenceDataStoreFactory.create(scope = alcance,
                produceFile = { archivo })
            val almacen = DataStoreSesion(datos)
            almacen.guardar(sesion)
            almacen.guardar(sesion.copy(token = "token-nuevo"))
            almacen.expirarSiCoincide(sesion.token)
            assertEquals("token-nuevo", almacen.sesion.first()?.token)
            almacen.expirarSiCoincide("token-nuevo")
            assertNull(almacen.sesion.first())
            assertEquals(Unit, almacen.expirada.first())
        } finally {
            alcance.cancel()
            alcance.coroutineContext.job.join()
        }
    }

    @Test fun refrescoPosteriorAlLogoutNoRestauraLaSesion() = runTest {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val archivo = carpeta.newFile("logout.preferences_pb")
            val datos = PreferenceDataStoreFactory.create(scope = alcance,
                produceFile = { archivo })
            val almacen = DataStoreSesion(datos)
            almacen.guardar(sesion)
            almacen.cerrar()
            almacen.actualizarUsuarioSiCoincide(sesion.token, usuario.copy(nombre = "Actualizada"))
            assertNull(almacen.sesion.first())
        } finally {
            alcance.cancel()
            alcance.coroutineContext.job.join()
        }
    }

    @Test fun representacionDeSesionNoExponeElToken() {
        assertFalse(sesion.toString().contains(sesion.token))
    }
}
