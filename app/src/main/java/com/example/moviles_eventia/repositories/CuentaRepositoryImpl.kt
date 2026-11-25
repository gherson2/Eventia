package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.api.dto.AutenticacionDto
import com.example.moviles_eventia.api.dto.LoginRequest
import com.example.moviles_eventia.api.dto.RegistroRequest
import com.example.moviles_eventia.api.dto.UsuarioDto
import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.session.SesionStore
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class CuentaRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val almacen: SesionStore
) : CuentaRepository {
    override val usuario = almacen.sesion.map { it?.usuario }
    override val expirada = almacen.expirada
    private val version = AtomicLong()
    private val escritura = Mutex()

    override suspend fun iniciarSesion(correo: String, clave: String) =
        autenticar { api.iniciarSesion(LoginRequest(correo.trim(), clave)) }

    override suspend fun registrar(nombre: String, correo: String, clave: String) =
        autenticar { api.registrar(RegistroRequest(nombre.trim(), correo.trim(), clave)) }

    private suspend fun autenticar(solicitar: suspend () -> AutenticacionDto) {
        val intento = version.incrementAndGet()
        val respuesta = try { solicitar() } catch (e: HttpException) { throw e.errorCuenta() }
        val token = respuesta.token?.takeIf { it.isNotBlank() }
            ?: throw IOException("Respuesta de sesión inválida")
        val usuario = respuesta.user?.aUsuario() ?: throw IOException("Usuario no disponible")
        escritura.withLock {
            if (version.get() != intento) throw CancellationException("Intento de sesión sustituido")
            almacen.guardar(Sesion(token, usuario))
        }
    }

    override suspend fun cerrarSesion() {
        version.incrementAndGet()
        escritura.withLock { almacen.cerrar() }
    }

    override suspend fun actualizarUsuario() {
        val actual = almacen.sesion.first() ?: return
        val usuario = api.obtenerUsuario().aUsuario()
        almacen.actualizarUsuarioSiCoincide(actual.token, usuario)
    }

    private fun UsuarioDto.aUsuario(): Usuario {
        if (id <= 0 || email.isNullOrBlank()) throw IOException("Usuario no válido")
        return Usuario(id, name.orEmpty(), email)
    }
}
