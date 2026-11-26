package com.example.moviles_eventia.ui.components

import com.example.moviles_eventia.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource

object ConfiguracionMapa {
    val agente = "EventiaAndroid/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID}; Android)"
    val fuente = XYTileSource(
        "EventiaOpenStreetMap", 0, 19, 256, ".png",
        arrayOf("https://tile.openstreetmap.org/"),
        "© OpenStreetMap contributors",
        TileSourcePolicy(
            2,
            TileSourcePolicy.FLAG_NO_BULK or TileSourcePolicy.FLAG_NO_PREVENTIVE or
                TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
        )
    )
}
