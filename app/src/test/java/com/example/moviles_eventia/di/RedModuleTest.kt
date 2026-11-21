package com.example.moviles_eventia.di

import com.example.moviles_eventia.BuildConfig
import com.example.moviles_eventia.repositories.EventRepositoryImpl
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RedModuleTest {
    @Test fun retrofitConservaLaUrlConfiguradaEnElProyecto() {
        val retrofit = RedModule.proporcionarRetrofit()
        assertEquals(BuildConfig.BASE_URL, retrofit.baseUrl().toString())
    }

    @Test fun proveedorDeApiUsaElRetrofitRecibido() = runTest {
        val servidor = MockWebServer()
        servidor.start()
        try {
            servidor.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""[{"id":3,"name":"Música"}]""")
            )
            val retrofit = Retrofit.Builder()
                .baseUrl(servidor.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = RedModule.proporcionarEventiaApi(retrofit)
            val repositorio = EventRepositoryImpl(api)

            assertEquals("Música", repositorio.obtenerCategorias().single().name)
            assertEquals("/api/categories", servidor.takeRequest().path)
        } finally {
            servidor.shutdown()
        }
    }
}
