package com.example.moviles_eventia.api

import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReintentoLecturaInterceptorTest {
    private lateinit var servidor: MockWebServer
    private val clientes = mutableListOf<OkHttpClient>()
    private var intentos = 0

    @Before fun preparar() { servidor = MockWebServer().apply { start() } }
    @After fun terminar() {
        clientes.forEach {
            it.connectionPool.evictAll()
            it.dispatcher.executorService.shutdown()
        }
        servidor.shutdown()
    }

    private fun cliente(fallo: IOException? = null) = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .callTimeout(3, TimeUnit.SECONDS)
        .addInterceptor(ReintentoLecturaInterceptor())
        .addInterceptor { chain ->
            intentos++
            if (intentos == 1 && fallo != null) throw fallo
            chain.proceed(chain.request())
        }.build().also { clientes += it }

    private fun solicitud(metodo: String = "GET") = Request.Builder().url(servidor.url("/me/attendances"))
        .method(metodo, if (metodo == "POST") "".toRequestBody() else null).build()

    private fun fallaSinReintentar(metodo: String, error: IOException) {
        try {
            cliente(error).newCall(solicitud(metodo)).execute().close()
            fail("Debía propagarse el error")
        } catch (recibido: IOException) {
            assertSame(error, recibido)
        }
        assertEquals(1, intentos)
        assertEquals(0, servidor.requestCount)
    }

    @Test fun getRecuperaEofAntesDeRecibirCabecerasConHttpReal() {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(MockResponse().setBody("[]"))
        cliente().newCall(solicitud()).execute().use {
            assertEquals(200, it.code)
            assertEquals("[]", it.body!!.string())
        }
        assertEquals(2, intentos)
        assertEquals(2, servidor.requestCount)
    }

    @Test fun getRecuperaSocketCerradoUnaSolaVez() {
        servidor.enqueue(MockResponse().setBody("[]"))
        cliente(SocketException("Conexión cerrada de prueba")).newCall(solicitud()).execute().use {
            assertEquals(200, it.code)
        }
        assertEquals(2, intentos)
    }

    @Test fun postNoSeRepiteAunqueElCierreSeaEof() =
        fallaSinReintentar("POST", IOException("Cierre", EOFException()))

    @Test fun deleteNoSeRepiteAunqueElCierreSeaEof() =
        fallaSinReintentar("DELETE", IOException("Cierre", EOFException()))

    @Test fun timeoutNoSeReintenta() =
        fallaSinReintentar("GET", SocketTimeoutException("Tiempo agotado"))

    @Test fun cambioDeSesionNoSeReintenta() =
        fallaSinReintentar("GET", IOException("La sesión cambió antes de enviar la solicitud."))

    @Test fun respuestaHttp503NoSeReintenta() {
        servidor.enqueue(MockResponse().setResponseCode(503))
        cliente().newCall(solicitud()).execute().use { assertEquals(503, it.code) }
        assertEquals(1, intentos)
    }

    @Test fun segundoEofSePropagaSinTercerIntento() {
        repeat(2) {
            servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        }
        try {
            cliente().newCall(solicitud()).execute().close()
            fail("El segundo cierre debe fallar")
        } catch (_: IOException) {
            assertEquals(2, intentos)
            assertEquals(2, servidor.requestCount)
        }
    }
}
