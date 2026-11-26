package com.example.moviles_eventia.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.moviles_eventia.models.PuntoMapa
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapaEventos(
    puntos: List<PuntoMapa>,
    alSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cargarTeselas: Boolean = true
) {
    val contexto = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val seleccionar by rememberUpdatedState(alSeleccionar)
    val puntosActuales by rememberUpdatedState(puntos)
    var latitud by rememberSaveable { mutableDoubleStateOf(-17.3935) }
    var longitud by rememberSaveable { mutableDoubleStateOf(-66.1570) }
    var zoom by rememberSaveable { mutableDoubleStateOf(13.0) }
    var centrado by rememberSaveable { mutableStateOf(false) }
    var falloTeselas by remember { mutableStateOf(false) }
    val mapa = remember(contexto) {
        Configuration.getInstance().apply {
            userAgentValue = ConfiguracionMapa.agente
            osmdroidBasePath = File(contexto.cacheDir, "mapa-eventia")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        MapView(contexto).apply {
            setTileSource(ConfiguracionMapa.fuente)
            setUseDataConnection(cargarTeselas)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setDestroyMode(false) // Compose controla onDetach explícitamente.
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(latitud, longitud))
            contentDescription = "Mapa de eventos. Toca un marcador o usa Elegir evento para ver su resumen."
        }
    }
    DisposableEffect(mapa, lifecycle) {
        val camara = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                latitud = mapa.mapCenter.latitude
                longitud = mapa.mapCenter.longitude
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean {
                zoom = mapa.zoomLevelDouble
                return false
            }
        }
        val teselas = Handler(Looper.getMainLooper()) { mensaje ->
            if (mensaje.what == MapTileProviderBase.MAPTILE_FAIL_ID) falloTeselas = true
            false
        }
        mapa.addMapListener(camara)
        mapa.tileProvider.tileRequestCompleteHandlers.add(teselas)
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observador)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onResume()
        onDispose {
            latitud = mapa.mapCenter.latitude
            longitud = mapa.mapCenter.longitude
            zoom = mapa.zoomLevelDouble
            lifecycle.removeObserver(observador)
            mapa.removeMapListener(camara)
            mapa.tileProvider.tileRequestCompleteHandlers.remove(teselas)
            teselas.removeCallbacksAndMessages(null)
            mapa.onPause()
            mapa.onDetach()
        }
    }
    LaunchedEffect(mapa, puntos) {
        mapa.overlays.filterIsInstance<Marker>().toList().forEach { it.remove(mapa) }
        puntos.forEach { punto ->
            mapa.overlays.add(Marker(mapa).apply {
                position = GeoPoint(punto.latitud, punto.longitud)
                title = if (punto.eventos.size == 1) punto.eventos.first().title else "${punto.eventos.size} eventos aquí"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> seleccionar(punto.eventos.first().id); true }
            })
        }
        if (!centrado && puntos.isNotEmpty()) {
            mapa.controller.setCenter(GeoPoint(puntos.first().latitud, puntos.first().longitud))
            centrado = true
        }
        mapa.invalidate()
    }
    LaunchedEffect(mapa, cargarTeselas) { mapa.setUseDataConnection(cargarTeselas) }

    Box(modifier.clipToBounds()) {
        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize().testTag("mapa-eventos"))
        Column(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
                Column {
                    IconButton(onClick = { mapa.controller.zoomIn() },
                        modifier = Modifier.semantics { contentDescription = "Acercar mapa" }) { Text("+") }
                    IconButton(onClick = { mapa.controller.zoomOut() },
                        modifier = Modifier.semantics { contentDescription = "Alejar mapa" }) { Text("−") }
                    TextButton(
                        enabled = puntos.isNotEmpty(),
                        onClick = {
                            puntosActuales.firstOrNull()?.let {
                                mapa.controller.animateTo(GeoPoint(it.latitud, it.longitud))
                            }
                        }
                    ) { Text("Centrar") }
                }
            }
        }
        if (falloTeselas && cargarTeselas) {
            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer) {
                TextButton(onClick = {
                    falloTeselas = false
                    mapa.tileProvider.clearTileCache()
                    mapa.invalidate()
                }) { Text("Parte del fondo no cargó · Reintentar") }
            }
        }
        Surface(Modifier.align(Alignment.BottomStart),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
            Text("© OpenStreetMap contributors", Modifier.padding(4.dp),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
