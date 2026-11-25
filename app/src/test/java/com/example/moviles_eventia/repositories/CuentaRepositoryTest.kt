package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.AutorizacionInterceptor
import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.api.dto.AutenticacionDto
import com.example.moviles_eventia.api.dto.LoginRequest
import com.example.moviles_eventia.api.dto.UsuarioDto
import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.session.SesionPrueba
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CuentaRepositoryTest {
    private lateinit var servidor: MockWebServer
    private lateinit var almacen: SesionPrueba
    private lateinit var api: EventiaApi
    private lateinit var repo: CuentaRepository
    private val usuario = Usuario(7, "Ana", "ana@example.test")
    private val respuesta = """{"token":"token-prueba","user":{"id":7,"name":"Ana","email":"ana@example.test"}}"""

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        almacen = SesionPrueba()
        val cliente = OkHttpClient.Builder().addInterceptor(AutorizacionInterceptor(almacen)).build()
        api = Retrofit.Builder().baseUrl(servidor.url("/api/")).client(cliente)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EventiaApi::class.java)
        repo = CuentaRepositoryImpl(api, almacen)
    }
    @After fun terminar() { servidor.shutdown() }

    @Test fun loginEnviaCorreoNormalizadoYGuardaTokenYUsuario() = runTest {
        servidor.enqueue(MockResponse().setBody(respuesta))
        repo.iniciarSesion(" ana@example.test ", "clave1234")
        val solicitud = servidor.takeRequest()
        assertEquals("POST", solicitud.method)
        assertEquals("/api/auth/login", solicitud.path)
        assertNull(solicitud.getHeader("Authorization"))
        assertEquals("application/json", solicitud.getHeader("Accept"))
        val cuerpo = JsonParser.parseString(solicitud.body.readUtf8()).asJsonObject
        assertEquals("ana@example.test", cuerpo["email"].asString)
        assertEquals("clave1234", cuerpo["password"].asString)
        assertEquals(Sesion("token-prueba", usuario), almacen.sesion.value)
    }

    @Test fun registroEnviaNombreYNoAdjuntaLaSesionAnterior() = runTest {
        almacen.guardar(Sesion("anterior", usuario))
        servidor.enqueue(MockResponse().setBody(respuesta))
        repo.registrar(" Ana ", " ana@example.test ", "clave1234")
        val solicitud = servidor.takeRequest()
        assertEquals("/api/auth/register", solicitud.path)
        assertNull(solicitud.getHeader("Authorization"))
        assertEquals("Ana", JsonParser.parseString(solicitud.body.readUtf8()).asJsonObject["name"].asString)
        assertEquals("token-prueba", almacen.sesion.value?.token)
    }

    @Test fun meEnviaBearerYRefrescaElPerfil() = runTest {
        almacen.guardar(Sesion("token-prueba", usuario))
        servidor.enqueue(MockResponse().setBody("""{"id":7,"name":"Ana nueva","email":"ana@example.test"}"""))
        repo.actualizarUsuario()
        assertEquals("Bearer token-prueba", servidor.takeRequest().getHeader("Authorization"))
        assertEquals("Ana nueva", almacen.sesion.value?.usuario?.nombre)
    }

    @Test fun un401AutenticadoLimpiaLaSesionYEmiteExpiracion() = runTest {
        almacen.guardar(Sesion("token-prueba", usuario))
        servidor.enqueue(MockResponse().setResponseCode(401))
        try {
            repo.actualizarUsuario()
            fail("Se esperaba 401")
        } catch (e: HttpException) { assertEquals(401, e.code()) }
        assertNull(almacen.sesion.value)
        assertEquals(Unit, almacen.expirada.first())
        assertEquals(1, almacen.expiraciones)
    }

    @Test fun credencialesIncorrectasNoExpiranOtraSesion() = runTest {
        almacen.guardar(Sesion("anterior", usuario))
        servidor.enqueue(MockResponse().setResponseCode(401))
        try {
            repo.iniciarSesion("ana@example.test", "incorrecta")
            fail("Se esperaba error")
        } catch (e: ErrorCuenta) { assertTrue(e.message.contains("incorrectos")) }
        assertEquals("anterior", almacen.sesion.value?.token)
        assertEquals(0, almacen.expiraciones)
    }

    @Test fun errores422SeAsignanAlCampoDelFormulario() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(422).setBody(
            """{"message":"Datos inválidos","fields":{"email":["Correo ya registrado"],"password":"Contraseña inválida"}}"""
        ))
        try {
            repo.registrar("Ana", "ana@example.test", "clave1234")
            fail("Se esperaba validación")
        } catch (e: ErrorCuenta) {
            assertEquals("Datos inválidos", e.message)
            assertEquals("Correo ya registrado", e.campos["email"])
            assertEquals("Contraseña inválida", e.campos["password"])
        }
        assertNull(almacen.sesion.value)
    }

    @Test fun catalogoPublicoNoEnviaTokenAunqueExistaCuenta() = runTest {
        almacen.guardar(Sesion("privado", usuario))
        servidor.enqueue(MockResponse().setBody("[]"))
        api.obtenerEventos()
        assertNull(servidor.takeRequest().getHeader("Authorization"))
    }

    @Test fun un401DeUnaPeticionAntiguaNoCierraLaCuentaNueva() = runTest {
        almacen.guardar(Sesion("antiguo", usuario))
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                runBlocking { almacen.guardar(Sesion("nuevo", usuario.copy(id = 8))) }
                return MockResponse().setResponseCode(401)
            }
        }
        try { repo.actualizarUsuario(); fail("Se esperaba 401") } catch (_: HttpException) { }
        assertEquals("nuevo", almacen.sesion.value?.token)
        assertEquals(0, almacen.expiraciones)
    }

    @Test fun logoutDuranteLoginPendienteNoPermiteRestaurarLaSesion() = runTest {
        val iniciada = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        val apiLenta = object : EventiaApi by api {
            override suspend fun iniciarSesion(solicitud: LoginRequest): AutenticacionDto {
                iniciada.complete(Unit)
                continuar.await()
                return AutenticacionDto("tardio", UsuarioDto(7, "Ana", "ana@example.test"))
            }
        }
        val cuenta = CuentaRepositoryImpl(apiLenta, almacen)
        val intento = async { cuenta.iniciarSesion("ana@example.test", "clave1234") }
        iniciada.await()
        cuenta.cerrarSesion()
        continuar.complete(Unit)
        try { intento.await(); fail("Se esperaba cancelación") } catch (_: CancellationException) { }
        assertNull(almacen.sesion.value)
    }

    @Test fun respuestaSinTokenNoSeGuardaComoSesionValida() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"token":"","user":{"id":7,"email":"ana@example.test"}}"""))
        try { repo.iniciarSesion("ana@example.test", "clave1234"); fail("Respuesta inválida") }
        catch (_: java.io.IOException) { }
        assertNull(almacen.sesion.value)
    }

    @Test fun cerrarSesionEsLocalYNoHacePeticiones() = runTest {
        almacen.guardar(Sesion("token-prueba", usuario))
        repo.cerrarSesion()
        assertNull(almacen.sesion.value)
        assertEquals(0, servidor.requestCount)
    }
}
