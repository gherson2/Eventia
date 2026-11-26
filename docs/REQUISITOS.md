# Matriz de requisitos

Implementación no equivale a verificación completa. Los resultados de dispositivo
se registran por separado en [PRUEBAS_MANUALES.md](PRUEBAS_MANUALES.md).

| Requisito | Implementación principal | Evidencia automática / comprobación final |
|---|---|---|
| RF-01 Catálogo | `ExplorarScreen`, `ExplorarViewModel`, `EventRepositoryImpl` | Filtrado de próximos, fechas y DTO en JVM; datos e imágenes reales en teléfono. |
| RF-02 Detalle | `DetalleEventoScreen`, `DetalleEventoViewModel` | Fuentes y estados en JVM; regreso conservando lista en teléfono. |
| RF-03 Búsqueda y filtros | `ExplorarViewModel`, `filtrarEventos` | Debounce, combinación y limpieza en JVM; interacción en teléfono. |
| RF-04 Favoritos | `FavoritosRepositoryImpl`, `FavoritosViewModel`, Room | JVM y `FavoritosPersistenciaTest`; cierre forzado y Deshacer en app real. |
| RF-05 Sin conexión | `ConectividadViewModel`, detalle local de favoritos | Fallback en JVM; modo avión y reapertura en teléfono. |
| RF-06 Actualización | `observarCatalogo`, `ExplorarViewModel` | Fallos conservan datos en JVM; reintento en app real. |
| RF-07 Mapa | `PuntosMapa`, `MapaEventos`, `MapaScreen` | Coordenadas/categoría/agrupación en JVM y marcadores en `MapaPantallaTest`. |
| RF-08 Selección | Marcador → resumen → ruta `evento/{eventoId}` | `MapaPantallaTest`; toque sobre marcador y navegación real en teléfono. |
| RF-09 Indicaciones | `Indicaciones.kt`, detalles de evento y lugar | Validación de coordenadas; probar aplicación externa y ausencia de receptor. |
| RF-10 Cercanos | No implementado (opcional) | No requiere permiso de ubicación para los requisitos obligatorios. |
| RF-11 Cuenta | `CuentaRepositoryImpl`, `DataStoreSesion`, `AutorizacionInterceptor` | Formularios, persistencia y 401 en JVM; acceso/registro/sesión real pendientes de comprobación. |
| RF-12 Inscripciones | `InscripcionRepositoryImpl`, detalle, `MisInscripcionesViewModel`, Perfil | JVM con MockWebServer, `InscripcionPantallaTest` y `MisInscripcionesPantallaTest`; escritura real solo con cuenta autorizada. |
| RF-13 Lugares | Pestaña Lugares de `ExplorarScreen`, detalle y repositorio | Estados y próximos eventos en JVM; navegación bidireccional en teléfono. |
| RF-14 Caché | `CacheCatalogoDao`, `EventRepositoryImpl`, migración Room 1→2 | JVM y `CatalogoMigracionTest`; reapertura offline y recuperación en teléfono. |

## Decisiones y límites

- DTO de Retrofit se transforman a modelos de dominio antes de llegar a la UI.
- ViewModel expone `StateFlow` y consume interfaces de repositorios.
- Hilt construye API, Room, DataStore, repositorios y ViewModel.
- Catálogo compartido entre Explorar y Mapa; detalles conservan su ámbito de navegación.
- Favoritos/caché pública no conservan el estado personal de inscripción.
- Próximas/pasadas del Perfil se agrupan por fecha de inicio del evento.
- Coordenadas idénticas se agrupan; “Otro evento aquí” permite acceder a todos.
- No hay mapa offline garantizado, ni localización actual (RF-10 opcional).
- La biblioteca de mapas debe estar definida por la asignatura o aprobada por el docente.
- Video, versión `v1.0` y defensa individual son entregables, no funciones certificables con una compilación.
