# Eventia · avance de catálogo y lugares

Aplicación Android del proyecto final de Programación de Aplicaciones Móviles 1.
Esta etapa adapta únicamente catálogo, detalles y lugares del proyecto de referencia
`25`, conservando el paquete y la configuración de `Pro_final_Moviles`.
No representa la entrega completa ni una calificación.

## Funciones de esta etapa

- Explorar con pestañas **Eventos** y **Lugares**.
- Catálogo de la API con nombre, resumen, fecha local, categoría, imagen, lugar y cupos.
- Imagen sustituta durante la carga, ante un error y cuando no existe imagen.
- Exclusión de eventos pasados o con fecha inválida, y orden por fecha.
- Búsqueda por nombre con espera de 300 ms, categoría combinable y limpieza de filtros.
- Actualización manual y reintento. Una actualización fallida conserva los datos visibles.
- Detalle de evento con descripción, fechas, lugar y cupos.
- Lista y detalle de lugares con horario, descripción y próximos eventos.
- Navegación evento → lugar → evento mediante identificadores.
- Tarjeta de evento compartida entre catálogo y detalle de lugar.
- Indicaciones mediante una aplicación externa, validando coordenadas y mostrando
  un mensaje si no hay una aplicación compatible.
- ViewModel conserva datos durante la rotación; SavedStateHandle guarda los filtros,
  y rememberLazyListState conserva la posición de las listas al volver.
  La comprobación visual de estos escenarios en dispositivo sigue pendiente.

## Configuración

Abre **la carpeta completa `Pro_final_Moviles`** en Android Studio.
Instala SDK Platform 37 y configura `sdk.dir` en tu `local.properties`.
Este archivo es local y no debe publicarse.

El backend predeterminado es el utilizado por el proyecto de referencia:

```text
https://eventia.jmacboy.dev/api/
```

No necesitas desarrollar ni iniciar un backend para usar ese servidor.
Puedes cambiar la URL agregando esta propiedad a `local.properties`, sin borrar `sdk.dir`:

```properties
EVENTIA_BASE_URL=https://eventia.jmacboy.dev/api/
```

La URL debe empezar con `https://` o `http://` y terminar en `/`.
Recompila después de cambiarla. HTTP está permitido solo en la variante debug,
para desarrollo con servidor local; usa HTTPS en release.
En emulador, el host de la PC se alcanza habitualmente mediante `10.0.2.2`,
no mediante `localhost`.

### Herramientas y bibliotecas

Se mantiene la configuración original del proyecto, no la de `25`:

| Componente | Versión/configuración |
|---|---|
| Gradle | 9.5.0 |
| Android Gradle Plugin | 9.3.3 |
| Plugin Kotlin Compose | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Android SDK mínimo / compile / target | 24 / 37 / 37 |
| Java de salida | 11 |
| Daemon Gradle | Java 25, según `gradle/gradle-daemon-jvm.properties` |
| Launcher observado en este equipo | JDK 21.0.10 |
| Lifecycle | 2.11.0 |
| Navigation Compose | 2.10.2 |
| Retrofit / Gson converter | 3.0.0 |
| Coil | 2.7.0 |
| Desugaring de java.time en Android 24–25 | desugar_jdk_libs 2.1.5 |
| Pruebas | JUnit 4.13.2, Coroutines Test 1.10.2, MockWebServer 4.12.0 |

La versión exacta de Android Studio usada por el estudiante queda pendiente de registrar
(Help → About). La validación de esta etapa se realiza con Gradle por consola.
No confundas la versión de Kotlin embebida en Gradle con la del plugin del proyecto.

## Arquitectura

```text
Screen → ViewModel → EventRepository → EventRepositoryImpl → EventiaApi (Retrofit)
             ↓                             ↓
       StateFlow de UI              DTO → modelo de dominio
```

- `api/`: endpoints y DTO de red.
- `models/`: modelos de dominio, validación de coordenadas y fechas.
- `repositories/`: interfaz, consumo de API, conversión y filtrado de próximos eventos.
- `ui/screens/`: pantallas y ViewModel con estado observable.
- `ui/components/`: tarjetas, imágenes, filtros, estados e indicaciones reutilizables.
- `navigation/EventiaApp.kt`: rutas con argumentos y fábricas de ViewModel.
- `AppContainer.kt`: composición manual de las dependencias existentes.

La UI no accede a Retrofit y no recibe DTO.
**Hilt todavía no está implementado**; la composición manual es transitoria y no
satisface por sí sola ese requisito obligatorio de la consigna.

### Endpoints utilizados

```text
GET /events?q=&category=
GET /events/{id}
GET /categories
GET /venues
GET /venues/{id}
GET /venues/{id}/events
```

Las consultas son públicas y no requieren cuenta. Los errores 404 del detalle
tienen mensajes específicos. Los errores de red o servidor permiten reintentar.
Todavía no existen flujos autenticados ni manejo de sesión 401/inscripciones 409.

## Compilar y probar

En PowerShell, desde la raíz:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Si todas las dependencias y el SDK ya están descargados, puede agregarse `--offline`.
Ese indicador afecta a Gradle, no simula que el teléfono esté sin Internet.

Las pruebas JVM usan un servidor HTTP local de prueba, no crean cuentas ni cambian
datos del backend público. Cubren consultas y rutas, mapeo, filtros, fechas,
coordenadas, espera de búsqueda, restauración de filtros, 404 y conservación de
datos ante fallos. No sustituyen las pruebas visuales en Android.

Consulta [docs/PRUEBAS_MANUALES.md](docs/PRUEBAS_MANUALES.md) para los escenarios
pendientes de comprobar en dispositivo.

### Resultado de la verificación (21 de noviembre de 2026)

- Compilación debug completada.
- **24 pruebas JVM aprobadas** (23 de esta etapa y la prueba de ejemplo preexistente).
- `lintDebug` completado, sin errores bloqueantes; conserva advertencias.
- API pública consultada por HTTP sin modificar datos.
- No se instaló ni se ejecutó esta versión en el teléfono. Las comprobaciones visuales,
  modo avión y rotación siguen pendientes.

## Lo que sigue pendiente

- Favoritos Room con Deshacer.
- Caché Room, aviso de antigüedad y funcionamiento sin conexión tras reiniciar.
- Registro, login, logout y sesión persistida en DataStore.
- Inscripción/cancelación, manejo de 401/409 y mis inscripciones.
- Mapa con marcadores, resumen y filtro de categoría.
- Inyección mediante Hilt.
- Validación completa de accesibilidad, tamaños de pantalla y rotación.
- Video final, documentación completa de la entrega y etiqueta `v1.0` cuando corresponda.

Conservar una lista ya cargada ante un fallo **no es una caché persistente**:
en esta etapa, abrir la app sin Internet todavía no puede recuperar el catálogo.
No se han trasladado las funcionalidades restantes ni el historial Git del proyecto `25`.
