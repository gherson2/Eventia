package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.api.SesionEsperada
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.session.SesionStore
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class InscripcionRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val almacen: SesionStore,
    private val conectividad: ConectividadRepository
) : InscripcionRepository {
    override suspend fun cambiarInscripcion(id: Int, inscribir: Boolean): Event {
        val sesion = almacen.sesion.first() ?: throw SesionRequeridaException()
        if (!conectividad.tieneConexion()) throw IOException("La inscripción requiere conexión.")
        val esperada = SesionEsperada(sesion.token)
        val respuesta = if (inscribir) api.inscribir(id, esperada) else api.cancelarInscripcion(id, esperada)
        if (almacen.sesion.first()?.token != sesion.token) {
            throw CancellationException("La sesión cambió durante la solicitud")
        }
        // La respuesta exitosa es la fuente de cupos; nunca se resta/suma localmente.
        return respuesta.toEvent().copy(isAttending = respuesta.isAttending ?: inscribir)
    }
}
