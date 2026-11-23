package com.example.moviles_eventia.repositories

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class ConectividadRepositoryImpl @Inject constructor(
    @ApplicationContext contexto: Context
) : ConectividadRepository {
    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)

    private fun tieneInternet(capacidades: NetworkCapabilities?): Boolean =
        capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    override fun tieneConexion(): Boolean =
        tieneInternet(gestor.getNetworkCapabilities(gestor.activeNetwork))

    override val conectado = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(red: Network, capacidades: NetworkCapabilities) {
                if (red == gestor.activeNetwork) trySend(tieneInternet(capacidades))
            }

            override fun onLost(red: Network) {
                // No declarar desconexión si Android ya cambió a otra red válida.
                val actual = gestor.activeNetwork
                trySend(actual != null && actual != red &&
                    tieneInternet(gestor.getNetworkCapabilities(actual)))
            }

            override fun onUnavailable() { trySend(false) }
        }
        gestor.registerDefaultNetworkCallback(callback)
        trySend(tieneConexion())
        awaitClose { gestor.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()
}
