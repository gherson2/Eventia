
# Eventia

Aplicación Android del proyecto final de Programación de Aplicaciones Móviles 1.
Se adaptaron catálogo, detalles y lugares del proyecto de referencia `25`,
conservando el paquete y la configuración de `Pro_final_Moviles`. En el siguiente
avance se integró Hilt para gestionar las dependencias existentes. Se añadieron
favoritos, caché, cuenta, inscripciones y mapa en este proyecto.
No representa la entrega completa ni una calificación.

## Video de demostración

[Ver el video en Google Drive](https://drive.google.com/file/d/1YLYNH6o98RzUyPDxGXpd3Ad3Npgp5rT1/view?usp=sharing)

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
- Tarjeta de evento compartida entre catálogo, favoritos y detalle de lugar.
- Indicaciones mediante una aplicación externa, validando coordenadas y mostrando
  un mensaje si no hay una aplicación compatible.
- ViewModel conserva datos durante la rotación; SavedStateHandle guarda los filtros,
  y rememberLazyListState conserva la posición de las listas al volver.
  La comprobación visual de estos escenarios en dispositivo sigue pendiente.
- Inyección con Hilt: API, repositorios y Room singleton; ViewModel con
  `@HiltViewModel` y creación mediante `hiltViewModel()`.
- Favoritos persistentes: guardar/quitar desde catálogo, detalle y eventos de un
  lugar. La pestaña Favoritos muestra las copias locales y ofrece **Deshacer**
  al quitar. No necesita cuenta ni solicitudes de red para guardar o listar.
- Detalle básico de favoritos sin conexión, con identificación de la copia local
  y advertencia de que los cupos pueden haber cambiado.
- Aviso global de conexión mediante las capacidades de red informadas por Android.
- Catálogo y categorías en caché Room para consultar los últimos datos descargados,
  con antigüedad visible, filtros locales y actualización manual.
- Cuenta opcional: registro, acceso, cierre de sesión y perfil de usuario.
- Sesión persistente con DataStore, autorización Bearer en solicitudes que admiten
  cuenta y redirección a acceso cuando una sesión autenticada recibe 401.
- Inscripción y cancelación desde el detalle, con cupos actualizados y manejo de
  conflictos de disponibilidad.
- “Mis inscripciones” en Perfil, con próximas y pasadas, acceso al detalle y
  actualización al regresar de una inscripción o cancelación.
- Mapa OpenStreetMap con marcadores, filtro de categoría compartido con Explorar,
  selección, resumen y acceso al detalle. Los eventos en el mismo punto pueden
  recorrerse mediante “Otro evento aquí”.

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
| Android Studio instalado en este equipo | 2026.1.3, build AI-261.26222.65.2613.16025427 |
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
| Hilt / Hilt Navigation Compose | 2.59.2 / 1.2.0 |
| KSP | 2.3.4 |
| Room | 2.7.2 |
| DataStore Preferences / OkHttp | 1.1.4 / 4.12.0 |
| Mapas | osmdroid 6.1.20 / OpenStreetMap |
| Desugaring de java.time en Android 24–25 | desugar_jdk_libs 2.1.5 |
| Pruebas | JUnit 4.13.2, Coroutines Test 1.10.2, MockWebServer 4.12.0 |

La versión instalada de Android Studio se obtuvo de su `product-info.json`;
si se usa otra instalación, actualizarla con Help → About.
La validación de esta etapa se realiza con Gradle por consola.
No confundas la versión de Kotlin embebida en Gradle con la del plugin del proyecto.

## Arquitectura

```text
Screen → ViewModel → EventRepository → EventRepositoryImpl → EventiaApi (Retrofit)
             ↓                             ↓
       StateFlow de UI              DTO → modelo de dominio
```

- `api/`: endpoints y DTO de red.
- `models/`: modelos de dominio, validación de coordenadas y fechas.
- `repositories/`: interfaces, consumo de API, conversión y filtrado de próximos
  eventos; `FavoritosRepository` encapsula el almacenamiento local y
  `ConectividadRepository` observa el estado de red.
- `db/`: base Room, entidad y DAO de favoritos.
- `ui/screens/`: pantallas y ViewModel con estado observable.
- `ui/components/`: tarjetas, imágenes, filtros, estados e indicaciones reutilizables.
- `navigation/EventiaApp.kt`: rutas con argumentos y creación de ViewModel con Hilt.
- `EventiaApplication.kt`: inicio del grafo de Hilt con `@HiltAndroidApp`.
- `di/RedModule.kt`: construcción de Retrofit y la API con `@Provides` y `@Singleton`.
- `di/RepositoriosModule.kt`: vínculo de la interfaz del repositorio con su
  implementación mediante `@Binds` y `@Singleton`.
- `di/PersistenciaModule.kt`: base de datos Room, DAO y serializador de favoritos.
- `session/`: interfaz de almacenamiento de sesión e implementación DataStore.
- `di/SesionModule.kt`: instancia única de DataStore para la aplicación.
- `repositories/CuentaRepositoryImpl.kt`: cuenta y actualización del usuario.
- `api/AutorizacionInterceptor.kt`: cabecera Bearer y expiración 401.

La UI no accede a Retrofit y no recibe DTO.
Hilt reemplaza `AppContainer`, `RetrofitClient` y las fábricas manuales de ViewModel.
La actividad usa `@AndroidEntryPoint`. Los ViewModel reciben la interfaz
`EventRepository` o `FavoritosRepository` mediante su constructor `@Inject`;
los que necesitan filtros o identificadores también reciben `SavedStateHandle`.

Retrofit, API y repositorio se comparten durante la vida de la aplicación.
El ViewModel de catálogo se comparte en la actividad entre Explorar y Mapa;
los ViewModel de detalles pertenecen a su destino de navegación.
`FavoritosViewModel` se comparte en la actividad para sincronizar los corazones
de todas las pantallas. Ningún ViewModel es singleton. Hilt conserva la integración
con los argumentos `eventoId` y `lugarId`.
Las pruebas JVM siguen construyendo los ViewModel con un repositorio de prueba:
no necesitan iniciar Android ni hacer solicitudes al servidor real.
Consulta [docs/INYECCION_HILT.md](docs/INYECCION_HILT.md) para una explicación breve.

### Persistencia de favoritos

Room utiliza `eventia.db`, versión 2, con la tabla `favoritos` y el id del evento
como clave primaria. Cada fila conserva una copia JSON completa del modelo de
dominio (descripción, fechas, categoría, lugar y cupos). Guardar otra vez el mismo
id reemplaza su copia y no lo duplica.

El repositorio expone un Flow y nunca llama a la API. Room ejecuta sus operaciones
asíncronas; la conversión JSON trabaja en `Dispatchers.IO`. Los cambios se reflejan
en las pantallas sin tener que recargar el catálogo.
El esquema se exporta en `app/schemas/` para preparar futuras migraciones;
no se habilitó una recreación destructiva de la base.

### Caché del catálogo

`EventRepository.observarCatalogo` decide entre red y datos locales y emite un
estado observable. La pantalla no accede al DAO ni a Retrofit:

- Muestra primero una copia disponible mientras solicita la actualización.
- Sin conexión, conserva los últimos eventos y muestra cuándo se guardaron.
- Si falla la solicitud, conserva los datos y muestra un error no bloqueante.
- Una respuesta remota válida reemplaza su copia, incluso si la lista está vacía.
- Si no hay datos guardados, informa el error; no inventa una lista exitosa.
- Las búsquedas combinadas con categoría tienen claves separadas y nunca
  sustituyen el catálogo completo. Para la consulta local se elige la copia
  aplicable más reciente y se filtran nombre, categoría y fechas.
- Las categorías también se guardan para permitir el filtrado sin conexión.
- Si el guardado falla, la respuesta de red se muestra igualmente con un aviso.

La tabla `cache_catalogo` guarda clave, JSON y fecha en milisegundos. La conversión
y lectura del catálogo se ejecutan en `Dispatchers.IO`. La antigüedad se comunica
en minutos, horas o días al representar el estado.

Se incluye una migración explícita de Room 1 a 2 que agrega la tabla de caché
**sin borrar los favoritos existentes**. Se conservan ambos esquemas en
`app/schemas/`. No se usa migración destructiva.

### Detalle sin conexión y conectividad

`EventRepository.obtenerDetalleEvento` decide la fuente del detalle:

- Sin conexión validada por Android, busca el favorito por id en Room, sin
  solicitar el detalle al servidor.
- Con conexión, prefiere la API. Ante un fallo de red o respuesta 5xx utiliza
  el favorito guardado, si existe.
- Un 404 se informa como evento no disponible; no se oculta con datos antiguos
  ni elimina automáticamente el favorito.
- Si no existe una copia, se muestra el error y la opción de reintentar.

La pantalla identifica la copia local y sus cupos como datos guardados, no como
disponibilidad actual. Al recuperar Internet se puede actualizar manualmente
para consultar el detalle remoto y retirar esa marca.

`ConectividadViewModel` alimenta un aviso compartido por todas las pantallas.
El estado desconocido inicial no se presenta como desconexión. La observación
usa `ACCESS_NETWORK_STATE`, un permiso normal sin diálogo; libera el callback
cuando deja de observarse. Una red validada no garantiza que el servidor esté
disponible, por eso también se manejan fallos de la solicitud.

**Límites:** las imágenes remotas no se garantizan sin Internet. Los detalles
completos de lugares no tienen caché persistente. El detalle offline de un evento
sigue garantizado únicamente si está guardado como favorito, no por aparecer en
la caché del catálogo.
La copia de un favorito se reemplaza al volver a guardarlo, no se sincroniza
automáticamente durante la consulta.

### Endpoints utilizados

```text
GET /events?q=&category=
GET /events/{id}
GET /categories
GET /venues
GET /venues/{id}
GET /venues/{id}/events
POST /auth/register
POST /auth/login
GET /me
GET /me/attendances
POST /events/{id}/attendance
DELETE /events/{id}/attendance
```

El catálogo, los lugares y los favoritos siguen disponibles sin cuenta. Los
errores 404 del detalle tienen mensajes específicos. Los errores de red o
servidor permiten reintentar. Los conflictos 409 de inscripción actualizan el detalle
y muestran un aviso comprensible.

### Cuenta y sesión

Desde Perfil se puede iniciar sesión o crear una cuenta. Los formularios validan
nombre en el registro, correo y contraseña de al menos ocho caracteres antes de
consultar la API. Los errores 400/422 se muestran junto al campo correspondiente.
El perfil muestra nombre, correo, actualización y cierre local de sesión.

`CuentaRepository` separa formularios y perfil de la API y de DataStore.
`SesionStore` conserva token y datos básicos del usuario de forma atómica.
Al reabrir la aplicación se recupera el usuario y se consulta `/me` para validar
la sesión; un fallo de conexión conserva la sesión local y permite explorar.
No se obliga al usuario a autenticarse para utilizar las funciones públicas.

El interceptor agrega `Accept: application/json` y adjunta el token a `/me` y
a rutas de detalle/inscripción que admiten sesión. No lo envía a login, registro
ni catálogo público. Un 401 autenticado borra la sesión y notifica a navegación,
que abre el formulario sin cerrar la aplicación. Un 401 de login se trata como
credenciales incorrectas, no como expiración de otra cuenta.

Una respuesta antigua no puede invalidar una sesión nueva. Tampoco puede
restaurarse un usuario con un refresco tardío después de cerrar sesión.
El cierre de sesión no llama a un endpoint inexistente ni borra favoritos/caché.

La contraseña solo vive en memoria del ViewModel: sobrevive a la rotación, pero
no se guarda en SavedStateHandle ni en DataStore, y se limpia al autenticar o
cambiar de formulario. El token se almacena en los archivos privados de la app,
se excluye del backup y de la transferencia entre dispositivos y no se registra
en logs. **DataStore no cifra el token por sí mismo**; para un producto comercial
convendría incorporar cifrado con Android Keystore.

### Inscripción y cancelación desde el detalle

Un detalle remoto reciente muestra la inscripción de la cuenta actual mediante
`isAttending`. El botón permite inscribirse o cancelar y utiliza los cupos devueltos
por el servidor, sin sumarlos ni restarlos de forma optimista.

- Sin cuenta, la acción abre acceso; al autenticar se regresa al evento y se
  consulta de nuevo su estado. **No se inscribe automáticamente** al iniciar sesión:
  el usuario confirma la acción desde el detalle.
- Mientras se envía una operación se bloquean pulsaciones duplicadas.
- Sin cupos se impide una inscripción nueva; una persona ya inscrita puede cancelar.
- Los eventos pasados y las copias offline no permiten operaciones de inscripción.
- Un 409 informa del conflicto y vuelve a consultar cupos e inscripción.
- Un fallo de envío conserva el detalle visible y exige actualizarlo antes de
  otra operación. No se repite el POST automáticamente desde la UI; el cliente
  también desactiva la recuperación automática de fallos de conexión.
- El 401 utiliza el cierre de sesión y la navegación al acceso ya implementados.

`InscripcionRepository` encapsula las escrituras autenticadas. Un tag local de
Retrofit fija la sesión esperada, no se envía como cabecera y evita que un cambio
de cuenta previo al envío inscriba a otra persona. Las respuestas de una cuenta
anterior se descartan y el detalle se recarga al cambiar de usuario.

Los favoritos y el catálogo no conservan `isAttending` como información válida
de sesión: se limpia al guardar/leer copias y solo se confía en datos remotos
recientes de la cuenta actual. Este cambio JSON no modifica el esquema de Room.

### Mis inscripciones en Perfil

`InscripcionRepository.obtenerMisInscripciones` consulta `GET /me/attendances`
con autorización Bearer y la sesión esperada. No consulta la API sin cuenta ni
utiliza la caché pública para almacenar estas filas personales.

`MisInscripcionesViewModel` mantiene un estado independiente del perfil de
usuario. Las inscripciones se agrupan por el instante ISO-8601 del evento:
próximas en orden ascendente y pasadas en orden descendente. Los registros sin
fecha válida se muestran en una sección explícita, sin confundirse con pasados.
Las tarjetas reutilizan el componente del catálogo y abren el detalle del evento.

Al entrar o regresar al Perfil se vuelve a consultar la lista, por lo que una
cancelación desde el detalle deja de aparecer después de regresar. Al abandonar
la pantalla se cancela la consulta pendiente. La actualización manual también
está disponible.

Un fallo inicial se muestra como error, no como una lista vacía exitosa. Si ya
había datos de la misma cuenta, una actualización fallida los conserva con un
aviso y reintento. Al cerrar sesión o cambiar de usuario se limpian las filas;
una respuesta tardía no puede colocarse bajo otra cuenta. La UI comprueba también
que la lista corresponda al usuario mostrado.

**Límite:** estas inscripciones solo viven en memoria y se vuelven a consultar
por red. No se garantiza una lista personal offline después de reiniciar la app.
Los flujos completos de RF-12 aún deben verificarse en dispositivo con la API.

## Compilar y probar

En PowerShell, desde la raíz:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

Si todas las dependencias y el SDK ya están descargados, puede agregarse `--offline`.
Ese indicador afecta a Gradle, no simula que el teléfono esté sin Internet.

Las pruebas JVM usan un servidor HTTP local de prueba, no crean cuentas ni cambian
datos del backend público. Cubren consultas y rutas, mapeo, filtros, fechas,
coordenadas, espera de búsqueda, restauración de filtros, 404 y conservación de
datos ante fallos. No sustituyen las pruebas visuales en Android.

Las pruebas de cuenta usan MockWebServer con credenciales ficticias. No registran
usuarios reales ni envían datos al backend público. También se prueba DataStore
con archivos temporales aislados: reapertura, cierre, expiración condicionada y
ausencia de contraseña en sus claves. No certifican la interfaz en un teléfono.
`CuentaPantallasTest` contiene dos pruebas instrumentadas de los composables
con estados ficticios, sin modificar la sesión real. Requieren ejecutarse
expresamente en un dispositivo o emulador; compilar el APK no significa ejecutarlas.
Las pruebas habituales de inscripciones usan MockWebServer y estados ficticios.
`InscripcionRealAutorizadaTest` es independiente: sí inscribe/cancela en el
backend configurado y exige autorización explícita y un evento concreto.
No se habilita con `eventia.liveReadOnly=true` ni durante una ejecución normal.
`InscripcionPantallaTest` comprueba botones con composables aislados y requiere
ejecución instrumentada para certificar la interfaz.
`MisInscripcionesPantallaTest` contiene casos de pestañas, apertura del detalle,
errores y protección ante listas de otra cuenta. Usa estados ficticios, no la
cuenta del dispositivo; compilarlo no equivale a ejecutarlo.

Las nuevas pruebas de favoritos verifican copia completa, actualización sin
duplicados, eliminación, Deshacer, errores y observación del estado.
`FavoritosPersistenciaTest` comprueba la reapertura de una base Room **de prueba**
sin acceder a `eventia.db`; requiere dispositivo y no se considera ejecutada solo
por compilar el APK de pruebas. Para ejecutarla expresamente:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.moviles_eventia.db.FavoritosPersistenciaTest"
```

`CatalogoMigracionTest` verifica la migración de una base de prueba versión 1,
la conservación del favorito y la reapertura de la caché. No se considera
ejecutado por compilarlo; requiere un dispositivo o emulador:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.moviles_eventia.db.CatalogoMigracionTest"
```

Consulta [docs/PRUEBAS_MANUALES.md](docs/PRUEBAS_MANUALES.md) para los escenarios
pendientes de comprobar en dispositivo.

### Resultado de la verificación (21 de noviembre de 2026)

- Compilación debug completada.
- **24 pruebas JVM aprobadas** (23 de esta etapa y la prueba de ejemplo preexistente).
- `lintDebug` completado, sin errores bloqueantes; conserva advertencias.
- API pública consultada por HTTP sin modificar datos.
- No se instaló ni se ejecutó esta versión en el teléfono. Las comprobaciones visuales,
  modo avión y rotación siguen pendientes.

### Verificación del avance con Hilt

- `assembleDebug`, `testDebugUnitTest` y `lintDebug` completados correctamente.
- **26 pruebas JVM aprobadas**, incluidas dos nuevas para los proveedores de red.
- KSP y Hilt generaron y compilaron el grafo de dependencias.
- Lint reportó 21 advertencias, sin errores bloqueantes.
- El arranque real y la navegación con Hilt en dispositivo siguen pendientes.

### Verificación de favoritos (22 de noviembre de 2026)

- `assembleDebug`, `testDebugUnitTest`, `lintDebug` y `assembleDebugAndroidTest`
  completados correctamente.
- **37 pruebas JVM aprobadas**, incluidas 11 nuevas de favoritos.
- Lint terminó sin errores bloqueantes; conserva 24 advertencias.
- La prueba Room de persistencia fue compilada, **no ejecutada** en dispositivo.
- No se instaló esta versión ni se verificó visualmente el cierre forzado,
  el modo avión o el snackbar Deshacer. Esos escenarios siguen pendientes.

### Verificación del detalle sin conexión (23 de noviembre de 2026)

- Compilación debug, pruebas JVM, Lint y compilación del APK de pruebas completados.
- **51 pruebas JVM aprobadas**, sin fallos.
- Lint terminó sin errores bloqueantes; conserva 17 advertencias.
- Se comprobaron selección de fuente, copia guardada, recuperación tras fallos,
  404, cancelación y estado observable de conectividad mediante pruebas locales.
- El modo avión, los callbacks reales de Android y la presentación visual siguen
  pendientes de verificar en un dispositivo. No se instaló esta versión.

### Verificación de la caché del catálogo

- Compilación debug, pruebas JVM, Lint y compilación del APK de pruebas completados.
- **66 pruebas JVM aprobadas**, sin fallos.
- Lint terminó sin errores bloqueantes; conserva 13 advertencias.
- Se comprobaron caché antes de red, filtros offline, antigüedad, respuesta vacía,
  errores de guardado y separación entre búsquedas, catálogo completo y favoritos.
- La prueba de migración fue compilada, no ejecutada. La migración real, la
  reapertura en modo avión y la interfaz siguen pendientes de probar en dispositivo.

### Verificación de cuenta y sesión

- Compilación debug, pruebas JVM, Lint y compilación del APK de pruebas completados.
- **93 pruebas JVM aprobadas**, sin fallos.
- Se comprobaron registro/login con MockWebServer, campos inválidos, errores 422,
  autorización Bearer, expiración 401, cierre local y respuestas antiguas.
- DataStore se verificó con archivos temporales reales, incluyendo reapertura
  y ausencia de contraseña en los datos persistidos.
- Lint terminó sin errores bloqueantes; conserva advertencias.
- No se crearon cuentas en el servidor público ni se instaló esta versión en el
  teléfono. Los flujos visuales y las pruebas instrumentadas siguen pendientes.

### Verificación de inscripción desde el detalle

- Compilación debug, pruebas JVM, Lint y compilación del APK de pruebas completados.
- **119 pruebas JVM aprobadas**, sin fallos.
- Se verificaron POST/DELETE autenticados, cupos, doble pulsación, 409, expiración,
  regreso del estado de sesión y descarte de respuestas antiguas.
- Se comprobó que favoritos y caché no recuperan una inscripción personal como
  estado válido de otra cuenta.
- Lint terminó sin errores bloqueantes; conserva 26 advertencias.
- No se realizaron inscripciones en el backend público ni se instaló esta versión
  en el teléfono. Las pruebas instrumentadas se compilaron, pero no se ejecutaron.

### Verificación de Mis inscripciones

- Compilación debug, pruebas JVM, Lint y compilación del APK de pruebas completados.
- **137 pruebas JVM aprobadas**, sin fallos.
- Se verificaron la ruta autenticada, próximas/pasadas, zonas horarias, fechas
  inválidas, actualización al reanudar, errores y limpieza al cambiar de cuenta.
- Lint terminó sin errores bloqueantes; conserva 26 advertencias.
- Las pruebas de pestañas y navegación de tarjetas se compilaron, pero no se
  ejecutaron en teléfono. No se consultaron ni modificaron cuentas reales.

## Mapa y ubicación

El mapa usa osmdroid y teselas de `https://tile.openstreetmap.org/`.
No requiere una clave API ni facturación. Confirmar su aprobación con el docente.
`ConfiguracionMapa` define HTTPS, atribución visible, un agente propio
`EventiaAndroid/...` y la política del proveedor: dos descargas simultáneas,
sin descarga masiva ni preventiva.

`agruparEventosMapa` omite coordenadas nulas, no finitas o fuera de rango y
respeta próximos eventos y categoría. Un punto compartido agrupa varios eventos.
La lista “Elegir evento” ofrece una alternativa a tocar el marcador.
El resumen permite guardar favoritos y abrir el detalle.

La cámara y selección usan estado guardable. `MapaEventos` integra `onResume`,
`onPause` y `onDetach` del mapa con el lifecycle de Compose. El fondo se guarda
en caché de la app y requiere conexión para cargar zonas nuevas: no se garantiza
cartografía offline. Los datos de eventos guardados son independientes de las
teselas. Un fallo de teselas ofrece reintento sin cerrar la pantalla.
No se solicita ubicación del usuario; RF-10 es opcional y no está implementado.

La [matriz de requisitos](docs/REQUISITOS.md) relaciona cada RF con su implementación
y distingue pruebas automáticas de verificaciones finales en teléfono.

## Validación anterior del mapa

- `assembleDebug`, `testDebugUnitTest`, `lintDebug` y `assembleDebugAndroidTest`
  completados; **143 pruebas JVM aprobadas**, sin fallos. Lint: 26 advertencias.
- Se añadieron pruebas de coordenadas inválidas, categorías, agrupación y
  recorrido circular de eventos coincidentes.
- El catálogo integra búsqueda, filtros y avisos en su lista desplazable para
  no ocultar las tarjetas en pantallas de poca altura.
- **17 pruebas Android aprobadas** en un 21061110AG con Android 13:
  última repetición `OK (17 tests)`, 39,518 s, ejecutadas directamente con ADB
  el 27 de noviembre de 2026, con la corrección del refresco del detalle instalada.
- Incluyen persistencia y migración/caché de Room en bases aisladas, Cuenta,
  inscripciones ficticias, seis casos del mapa (incluido toque de marcador nativo)
  y navegación de la app real catálogo → detalle → mapa → resumen → detalle.
- La navegación real es una prueba optativa de lectura del backend configurado:
  no registra cuentas ni cambia favoritos o inscripciones. Puede actualizar la
  caché pública de la app. Si el detalle muestra explícitamente un fallo de
  conexión, comprueba ese mensaje y permite un único reintento mediante el botón
  de UI. Si sigue fallando, la prueba falla y registra el estado de la pantalla;
  no omite errores persistentes ni reintenta registros o inscripciones.
- `PrimerPlanoTestRule` abre una actividad vacía para evitar el bloqueo de inicio
  en segundo plano de MIUI y mantiene encendidas solo las ventanas de prueba.
  El teléfono debe estar desbloqueado al comenzar. No modifica permisos ni el
  tiempo de bloqueo global.
- Se corrigieron dos verificaciones del test, sin cambiar esas funciones de la
  app: esperar hasta que el resumen del toque nativo esté visible y desplazar la
  lista hasta el título antes de comprobarlo. Una revisión posterior reprodujo
  también fallos reales de conexión en el detalle: no todos los tiempos de
  espera agotados se debían a una fila fuera de pantalla.

Para repetir tras compilar e instalar los APK debug y de pruebas:

```powershell
adb shell am instrument -w -r -e eventia.liveReadOnly true com.example.moviles_eventia.test/androidx.test.runner.AndroidJUnitRunner
```

Sin `eventia.liveReadOnly=true`, se omiten las pruebas que requieren el backend real.
El resultado anterior no certifica todavía modo avión, rotación física ni
escrituras de cuenta/inscripción con el servidor.

## Corrección del refresco del detalle (27 de noviembre de 2026)

- `LifecycleResumeEffect` conecta la reanudación del destino del evento con su
  ViewModel. Al volver desde otra pantalla o desde segundo plano, vuelve a leer
  cupos e inscripción sin vaciar el evento mostrado ni recrear su lista.
- La primera reanudación no duplica la carga inicial. Mientras se comprueban
  los datos de regreso, no se permiten nuevas operaciones con datos antiguos.
- Si hay una consulta o inscripción en curso, se agrupa un refresco de lectura
  para cuando termine. Salir de la pantalla no cancela ni repite una escritura.
  Si vuelve a salir antes de terminar, el refresco espera a otra reanudación.
- Diez pruebas JVM nuevas cubren dos detalles del mismo evento, cambios de cupos
  e inscripción, cancelación, fallo de red, cargas pendientes y escrituras en curso.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug` y `assembleDebugAndroidTest`
  completados correctamente: **153 pruebas JVM aprobadas**, sin fallos;
  Lint sin errores bloqueantes y 15 advertencias.
- El archivo local `app/build.gradle.kts` ya no contiene comandos Git y compila.
  La corrección y este cambio aún deben guardarse en un commit.
- Después de desbloquear el teléfono, se instalaron ambos APK con `adb install -r`
  y se repitieron las pruebas Android con esta corrección: **17 aprobadas**,
  `OK (17 tests)` en 39,518 s, incluida la navegación real de lectura.
  No se registraron cuentas ni se enviaron inscripciones/cancelaciones reales.

## Verificación ampliada en celular (27 de noviembre de 2026)

- Compilación debug y APK de pruebas, `testDebugUnitTest` y `lintDebug` correctos:
  **153 pruebas JVM**, sin fallos; Lint: **15 advertencias**, sin errores.
- Instalación con `adb install -r`, sin borrar los datos existentes. Suite completa
  ejecutada en el 21061110AG con Android 13: **`OK (33 tests)` en 55,991 s**,
  con `eventia.liveReadOnly=true`, sin casos omitidos.
- Con el backend público: catálogo/detalle/mapa, búsqueda combinada con categoría,
  regreso, limpieza, rotación real de la actividad a horizontal conservando filtros
  y recorrido evento → lugar → mismo evento → regreso.
- Con servidor HTTP local, Retrofit e interceptor reales: registro, login,
  recuperación de sesión tras reabrir DataStore, logout conservando favoritos,
  inscripción, consulta de inscripciones, cancelación, 401 y conflicto 409.
  El 409 comprueba refresco y una sola escritura ante doble invocación.
- Room aislado: caché y detalle favorito después de cerrar/reabrir la base,
  desconexión inyectada sin solicitudes HTTP, error 503 conservando datos y
  recuperación que reemplaza la caché. También pasó quitar/Deshacer con snackbar
  y Room reales. No se cambió el modo avión del teléfono.
- UI con ventana de prueba de 320×480 dp y fuente al 160 %: catálogo con avisos,
  detalle extenso y Perfil permiten alcanzar sus acciones mediante desplazamiento.
  No se cambió la fuente global.
- Cuatro pruebas de indicaciones interceptan el intent y sus errores. Además,
  ADB abrió Google Maps mediante un intent `geo:` y se regresó a Eventia.
  Esto no certifica todavía el recorrido completo del botón real hasta Maps.
- Durante la preparación se corrigieron dos problemas del test: título ambiguo
  entre búsqueda/tarjeta y espera de categorías que cargan en paralelo al catálogo.
  La ejecución anterior de 32/33 no se contó como aprobada; la repetición final
  completó los 33 casos.

Resultados y límites por escenario: [docs/PRUEBAS_MANUALES.md](docs/PRUEBAS_MANUALES.md).
Las cuentas, bases y sesiones de esta etapa fueron temporales: no se crearon
cuentas ni se cambiaron favoritos o inscripciones personales en el servidor
público durante esa ejecución. La prueba autorizada posterior se detalla abajo.

## Inscripción real autorizada y recuperación de lecturas

Después de que el usuario iniciara sesión y autorizara inscribir/cancelar:

- La app recuperó la sesión al arrancar y consultó Perfil. En el evento 21,
  “Meetup de desarrolladores Android”, se comprobó el estado inicial no inscrito.
- El botón real confirmó la inscripción, cupos **80 → 79**, y Perfil incorporó
  el evento. Desde su tarjeta se abrió el detalle, se canceló, los cupos volvieron
  a **80** y Perfil retiró la inscripción de prueba (próximas **0 → 1 → 0**).
- `InscripcionRealAutorizadaTest`: **`OK (1 test)` en 14,565 s**. Los primeros
  intentos detectaron fallos de lectura y cancelaron la inscripción en `finally`;
  no se contó ninguno como aprobado. No se tocaron inscripciones preexistentes.
- Se diagnosticaron `IOException → EOFException` en GET `/me` y
  `/me/attendances`. `ReintentoLecturaInterceptor` permite un solo reintento de
  GET ante EOF/socket cerrado, antes de entregar cabeceras; no reintenta POST,
  DELETE, estados HTTP, timeout ni cambios de sesión. Autorización se comprueba
  de nuevo en el reintento. Los errores de lectura tienen diagnóstico debug
  limitado a ruta/tipo/estado, sin cuerpos, correos, tokens ni contraseñas.
- **La intermitencia de red no está eliminada**: la prueba aprobada aún mostró
  un error de lectura en Perfil y comprobó recuperación con un único reintento
  desde la UI. No hay reenvío automático de inscripciones/cancelaciones.
- Después del cambio: **161 pruebas JVM aprobadas**, compilaciones y Lint
  correctos (15 advertencias, sin errores). Regresión sin escrituras:
  **`OK (33 tests)` en 60,472 s**, excluyendo explícitamente el test autorizado.
  Son **34 casos Android aprobados en dos ejecuciones separadas**, no una suite
  de 34 ejecutada en una sola tanda.
- La sesión quedó abierta; no se hizo logout ni se crearon otras cuentas.
  La consulta pública final del evento confirmó 80 cupos.

La prueba real queda desactivada por defecto. Para repetirla se necesita permiso
nuevo para la cuenta/evento y ambos argumentos `eventia.liveWrites=true` y
`eventia.eventTitle` (título exacto). Nunca ejecutarla en una cuenta ajena.

## Verificación final en dispositivo

Se completaron dos fases optativas en el celular:
online `OK (1 test)` en **10,228 s** y offline/recuperación/Maps `OK (1 test)`
en **35,328 s**. Quedó comprobado:

- Búsqueda sin coincidencias y limpieza; detalle y mapa con resumen conservados
  al solicitar rotación horizontal de la actividad.
- Modo avión real con Wi-Fi desactivado, cierre forzado mediante `am force-stop`
  y reapertura: catálogo con antigüedad, mismo favorito existente y detalle local.
  La inscripción queda bloqueada con la copia offline.
- Recuperación real de Internet: el aviso global desaparece por callback Android;
  Actualizar retira la marca de copia del detalle y la de caché del catálogo.
- El botón real “Cómo llegar” del detalle abrió Google Maps en primer plano.

No se cambiaron favoritos ni se enviaron nuevas inscripciones. La conectividad
se restauró en `finally`: modo avión apagado, Wi-Fi encendido y red validada.
La app quedó abierta y se restauraron los estados iniciales de conectividad.

Total verificado: **161 JVM y 36 casos Android**, estos últimos en cuatro
ejecuciones separadas (33 + 1 + 1 + 1). Compilación debug, APK de pruebas y
Lint correctos; 15 advertencias, sin errores. No equivale a cobertura exhaustiva.

## Cobertura adicional y entrega pendientes

- La migración Room pasó con una base aislada; falta comprobar una instalación
  previa real. La reapertura offline y la recuperación de red actuales sí pasaron.
- Registro/logout y formulario de acceso completo contra el servidor público:
  el usuario informó acceso exitoso y la sesión recuperada sí pasó en el teléfono;
  no se automatizó el ingreso de credenciales ni se cerró esa sesión.
- Regreso desde el formulario de acceso al evento original; el recorrido con
  sesión ya abierta, inscripción → Perfil → detalle → cancelación → Perfil sí pasó.
  El 409 solo pasó con servidor controlado.
- Las pestañas y protección entre cuentas pasaron con datos ficticios; falta
  comprobar el cambio de cuenta real y el regreso entre dos detalles del mismo
  evento después de modificar una inscripción.
- Verificar visualmente las teselas y sus errores de red; confirmar la aprobación
  de la biblioteca de mapas por el docente.
- Validación completa de accesibilidad y otros tamaños; rotación del formulario
  y sensor físico (lista, contenido del detalle y resumen del mapa sí pasaron con
  rotación de actividad).
- Comprobar el acceso al video para terceros y confirmar su duración con el
  docente (la consigna indica 3–4 minutos). La etiqueta `v1.0` sigue pendiente.

El catálogo sin conexión requiere haber descargado datos al menos una vez.
Los favoritos y la caché se almacenan por separado.
No se ha trasladado el historial Git del proyecto `25`.
