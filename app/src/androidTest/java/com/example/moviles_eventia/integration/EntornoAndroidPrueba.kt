package com.example.moviles_eventia.integration

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.api.AutorizacionInterceptor
import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.db.EventiaDatabase
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.repositories.ConectividadRepository
import com.example.moviles_eventia.repositories.CuentaRepositoryImpl
import com.example.moviles_eventia.repositories.EventRepositoryImpl
import com.example.moviles_eventia.repositories.FavoritosRepositoryImpl
import com.example.moviles_eventia.repositories.InscripcionRepositoryImpl
import com.example.moviles_eventia.session.DataStoreSesion
import com.google.gson.Gson
import java.io.Closeable
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** No abre eventia.db ni el archivo de sesión de la app. HTTP solo en localhost. */
class EntornoAndroidPrueba : Closeable {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val identificador = UUID.randomUUID().toString()
    private val nombreDb = "verificacion-$identificador.db"
    private val archivoSesion = File(contexto.cacheDir, "verificacion-$identificador.preferences_pb")
    private var trabajoSesion = SupervisorJob()
    private val clientes = mutableListOf<OkHttpClient>()
    val gson = Gson()
    val servidor = MockWebServer().apply { start() }
    val conectado = MutableStateFlow(true)
    val conectividad = object : ConectividadRepository {
        override val conectado = this@EntornoAndroidPrueba.conectado
        override fun tieneConexion() = conectado.value
    }
    var db = abrirDb()
        private set
    var sesiones = abrirSesion()
        private set
    var api = crearApi()
        private set
    val evento = Event(42, "Jazz de prueba", "Concierto", "2099-12-01T20:00:00-04:00",
        Category(3, "Música"), null, Venue(7, "Teatro de prueba", "Calle 7", -17.39, -66.15),
        50, 10, "Descripción completa sin conexión")

    private fun abrirDb() = Room.databaseBuilder(contexto, EventiaDatabase::class.java, nombreDb)
        .addMigrations(EventiaDatabase.MIGRACION_1_2).build()

    private fun abrirSesion() = DataStoreSesion(PreferenceDataStoreFactory.create(
        scope = CoroutineScope(trabajoSesion + Dispatchers.IO),
        produceFile = { archivoSesion }
    ))

    private fun crearApi(): EventiaApi {
        val cliente = OkHttpClient.Builder().retryOnConnectionFailure(false)
            .addInterceptor(AutorizacionInterceptor(sesiones))
            .callTimeout(5, TimeUnit.SECONDS).build()
        clientes += cliente
        return Retrofit.Builder().baseUrl(servidor.url("/api/")).client(cliente)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EventiaApi::class.java)
    }

    fun favoritos() = FavoritosRepositoryImpl(db.favoritosDao(), gson)
    fun eventos() = EventRepositoryImpl(api, favoritos(), conectividad, db.cacheCatalogoDao(), gson)
    fun cuenta() = CuentaRepositoryImpl(api, sesiones)
    fun inscripciones() = InscripcionRepositoryImpl(api, sesiones, conectividad)

    fun responder(valor: Any, codigo: Int = 200) {
        servidor.enqueue(MockResponse().setResponseCode(codigo)
            .setHeader("Content-Type", "application/json").setBody(gson.toJson(valor)))
    }

    fun responderCuenta(token: String) = responder(mapOf(
        "token" to token,
        "user" to mapOf("id" to 7, "name" to "Ana de prueba", "email" to "ana@example.test")
    ))

    fun reabrirBase() {
        db.close()
        db = abrirDb()
    }

    suspend fun reabrirSesion() {
        trabajoSesion.cancelAndJoin()
        trabajoSesion = SupervisorJob()
        sesiones = abrirSesion()
        api = crearApi()
    }

    override fun close() {
        runBlocking { trabajoSesion.cancelAndJoin() }
        db.close()
        contexto.deleteDatabase(nombreDb)
        archivoSesion.delete()
        clientes.forEach {
            it.dispatcher.executorService.shutdown()
            it.connectionPool.evictAll()
        }
        servidor.shutdown()
    }
}
