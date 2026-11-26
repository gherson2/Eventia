# Escenarios de verificación · Eventia

No marcar como aprobado sin ejecutar la comprobación en un dispositivo o emulador.
Las verificaciones automáticas de repositorio/ViewModel no certifican la interfaz.

## Resultado más reciente en dispositivo

**17 pruebas Android aprobadas**, última repetición `OK (17 tests)` en 37,211 s, en el teléfono
21061110AG con Android 13, ejecutadas directamente con ADB. Se incluyó la prueba
optativa `NavegacionRealTest` con `eventia.liveReadOnly=true`.
Comprobó catálogo y detalle de la app real, regreso, mapa, selección por lista,
resumen y detalle del evento elegido. Otro caso comprobó el toque nativo del marcador.
No se realizaron registros, cambios de favoritos ni inscripciones reales.

Los tests de Cuenta/Perfil/inscripción usan datos ficticios. Las pruebas de Room
usan bases aisladas. Restaurar estado guardable no equivale a rotar físicamente
el teléfono. La lista siguiente distingue esas evidencias de escenarios manuales.

### Incidencias durante la preparación (resueltas para esta ejecución)

Se detectó el teléfono 21061110AG (Android 13), conectado y autorizado por ADB.
La ejecución instrumentada se detuvo al instalar el APK de pruebas con
`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`.
En ese primer intento se ejecutaron 0 pruebas Android.

Después de autorizar la instalación, se iniciaron 15 pruebas y se completaron
sin fallos las tres primeras: contexto de aplicación, migración/caché Room y
persistencia de favoritos. La primera prueba UI quedó bloqueada: el sistema
registró `Abort background activity starts` al iniciar `ComponentActivity`.
El teléfono terminó con la pantalla apagada y el comando alcanzó su tiempo
límite. Se detuvo el proceso de prueba sin ejecutar un borrado de datos.
Ese intento no certificó la suite ni las pantallas.

En un intento posterior, `PrimerPlanoTestRule` permitió abrir las pantallas
sin cambiar permisos: la suite de 15 pruebas terminó con **`OK (15 tests)`**
en 26,31 s. Se comprobaron composables aislados, selección por lista del mapa,
marcadores agrupados, filtro, vacío y restauración de selección guardable.
No equivale a rotación física ni a inscripción real.

Se observó el arranque real con Hilt y el catálogo público en el teléfono.
La ampliación a 17 pruebas terminó con 13 fallos con el teléfono bloqueado:
no había jerarquías Compose ni actividades reanudadas. El toque nativo del
marcador y la navegación integrada quedaron pendientes en ese intento. La regla
ahora detecta el bloqueo y mantiene encendidas solo las ventanas de prueba.

Con el teléfono desbloqueado, la siguiente ejecución completó 15 de 17 casos:
el toque nativo requería esperar el intervalo de doble toque de osmdroid y el
título del detalle no estaba compuesto por quedar fuera del área visible de
la lista. Se ajustaron únicamente esas verificaciones, esperando la confirmación
del toque y desplazando hasta el título. Primero pasaron ambos casos por
separado y luego la suite completa de 17, sin fallos.

Una revisión posterior volvió a reproducir un fallo intermitente de navegación:
el árbol de UI mostraba “No pudimos conectarnos”, no una lista desplazada.
Se corrigió la interpretación anterior: también había fallos reales de red.
La prueba ahora espera contenido o ese error explícito; si aparece el error,
lo verifica y permite un solo reintento desde la UI. Un error persistente sigue
fallando y deja un diagnóstico, sin convertirlo en aprobado. No se modificó la
política de reintentos de escrituras de la app. Tras los ajustes, las dos pruebas
pasaron juntas en tres repeticiones y la suite final terminó con `OK (17 tests)`.

Los escenarios siguientes siguen pendientes salvo las comprobaciones aisladas
indicadas. No se usaron cuentas reales para inscribirse ni cancelar.

| Escenario | Resultado esperado | Estado |
|---|---|---|
| Abrir Explorar con Internet | Datos de la API, imágenes y fechas locales | Observado en Android 13 con catálogo público |
| Arrancar tras integrar Hilt | Application y actividad inicializan el grafo sin cierres | Arranque real observado en Android 13 |
| Abrir evento y lugar con Hilt | ViewModel recibe el id correcto mediante SavedStateHandle | Evento desde catálogo/mapa aprobado en app real; lugar pendiente |
| Buscar por nombre y elegir categoría | Solo coincidencias de ambas condiciones | Pendiente |
| Escribir rápidamente | Consultar después de 300 ms sin nuevos cambios | Cubierto en JVM; UI pendiente |
| Limpiar filtros | Restablecer el catálogo completo de próximos eventos | Cubierto en JVM; UI pendiente |
| Buscar algo inexistente | Mensaje de vacío y acción Limpiar filtros | Pendiente |
| Abrir evento y volver tras desplazar la lista | Conservar posición, búsqueda y categoría | Pendiente |
| Cambiar de pestaña Eventos/Lugares | Conservar la posición de ambas listas | Pendiente |
| Rotar en lista y detalle | Conservar estado, filtros y posición | Pendiente |
| Actualizar con modo avión después de cargar datos | Mantener lista y mostrar aviso con reintento | Cubierto con IOException en JVM; modo avión pendiente |
| Abrir sin Internet sin datos descargados previamente | Error comprensible con reintento, sin cierre | Prueba JVM; UI pendiente |
| Abrir evento sin imagen | Mostrar imagen sustituta | Pendiente |
| Abrir evento sin cupos | Mostrar cero cupos y bloquear inscripción nueva; permitir cancelar si ya estaba inscrito | Prueba JVM; UI real pendiente |
| Evento → lugar → otro evento | Navegar a los detalles correspondientes y poder regresar | Pendiente |
| Detalle de lugar | Descripción, horario, dirección y próximos eventos | Pendiente |
| Fallar la consulta de eventos del lugar | Mantener datos del lugar y mostrar error de sus eventos | Cubierto en JVM; UI pendiente |
| Abrir recurso inexistente (404) | Mensaje de recurso no disponible y posibilidad de volver | Cubierto en JVM; UI pendiente |
| Coordenadas incompletas o inválidas | No utilizar coordenadas incorrectas; intentar por dirección | Validación JVM; intent pendiente |
| Cómo llegar con aplicación de mapas | Abrir la ubicación en una aplicación externa | Pendiente |
| Cómo llegar sin aplicación compatible | Informar sin cerrar la app | Pendiente |
| Texto grande y pantalla pequeña | Poder leer y utilizar las acciones | Pendiente |
| Guardar desde catálogo o detalle | Corazón y lista Favoritos reflejan el cambio | Pruebas JVM; UI pendiente |
| Guardar el mismo evento otra vez | No duplicar la fila | Prueba JVM; Room real pendiente |
| Quitar en Favoritos y tocar Deshacer | Recuperar el mismo evento | Prueba JVM; snackbar real pendiente |
| Cerrar forzadamente y reabrir | Mantener favoritos guardados en Room | Prueba instrumentada añadida; ejecución pendiente |
| Listar favoritos en modo avión | Mostrar las copias locales y permitir quitarlas | Pendiente en dispositivo |
| Reabrir una base Room de prueba | Mantener favorito y poder eliminarlo | Completado sin fallos en Android 13; no equivale a cierre forzado de la app real |
| Abrir detalle de favorito sin red | Mostrar detalle local y advertencia de copia guardada | Cubierto en JVM; modo avión pendiente |
| Abrir evento no guardado sin red | Error comprensible sin inventar datos | Cubierto en JVM; UI pendiente |
| Recuperar conexión y actualizar el detalle | Consultar la API y retirar la marca de copia guardada | Cubierto en JVM; UI pendiente |
| Perder o recuperar conexión | Mostrar o retirar el aviso global | Estado observable cubierto en JVM; callbacks Android pendientes |
| Servidor 404 con favorito guardado | Informar evento no disponible, sin borrar el favorito | Cubierto en JVM; UI pendiente |
| Reabrir el catálogo en modo avión tras descargarlo | Lista local y antigüedad visible | Repositorio cubierto en JVM; dispositivo pendiente |
| Buscar y filtrar en el catálogo sin conexión | Solo coincidencias de la copia disponible | Prueba JVM; UI pendiente |
| Actualizar tras recuperar conexión | Reemplazar la copia y retirar la marca de datos guardados | Prueba JVM; UI pendiente |
| Buscar y luego limpiar filtros | No reemplazar el catálogo completo por los resultados de búsqueda | Prueba JVM; UI pendiente |
| Migrar una instalación con favoritos | Mantener favoritos al agregar caché Room | CatalogoMigracionTest completado sin fallos en base aislada en Android 13; instalación real pendiente |
| Recibir un catálogo vacío desde la API | Reemplazar la caché anterior con una lista vacía válida | Prueba JVM; UI pendiente |
| Registrar con campos inválidos | Validar antes de hacer solicitudes | Prueba JVM; UI pendiente |
| Registrar e iniciar sesión | Guardar token y mostrar usuario en Perfil | MockWebServer; servidor real y UI pendientes |
| Error 422 por correo registrado | Mostrar el mensaje junto al campo | Prueba JVM; UI pendiente |
| Clave incorrecta (401 de login) | Mostrar error sin cerrar otra sesión existente | Prueba JVM; UI pendiente |
| Reiniciar la app con una sesión guardada | Recuperar usuario sin exigir acceso nuevamente | DataStore con archivo temporal; dispositivo pendiente |
| Rotar en el formulario | Conservar campos y clave solo en memoria | ViewModel probado; rotación real pendiente |
| Cerrar sesión | Borrar sesión, sin borrar favoritos ni caché | Cierre local probado; UI pendiente |
| Sesión expirada en solicitud autenticada | Borrar sesión y llevar al acceso sin cerrar la app | Interceptor probado; navegación real pendiente |
| Respuesta 401 de una sesión anterior | No borrar una sesión nueva | Prueba JVM |
| Abrir sin cuenta | Mantener catálogo, lugares y favoritos disponibles | UI pendiente |
| Inscribirse desde detalle con sesión | Confirmar inscripción y actualizar cupos con la respuesta | MockWebServer y ViewModel; UI pendiente |
| Cancelar inscripción | Cambiar botón y mostrar cupos devueltos por el servidor | Prueba JVM; UI pendiente |
| Inscribirse sin sesión | Abrir acceso, regresar al evento y pedir confirmación | ViewModel probado; navegación real pendiente |
| Evento sin cupos | Bloquear una inscripción nueva, sin impedir cancelar una existente | Prueba JVM; UI pendiente |
| Conflicto 409 | Avisar y refrescar el detalle | Prueba JVM; UI pendiente |
| Doble pulsación | Enviar una sola operación mientras se procesa | Prueba JVM; UI pendiente |
| Caída de red durante la inscripción | Mantener detalle y actualizar antes de volver a operar | Prueba JVM; modo avión pendiente |
| Cambiar de cuenta durante la solicitud | Descartar la respuesta anterior y no inscribir otra cuenta | Prueba JVM; UI pendiente |
| Favorito guardado por otra cuenta | No recuperar su estado personal de inscripción | Prueba JVM |
| Ver mis inscripciones en Perfil | Próximas ascendentes y pasadas descendentes | JVM y composable aislado aprobados; cuenta real pendiente |
| Abrir una inscripción desde Perfil | Detalle del evento y regreso conservando navegación | Callback de tarjeta aprobado en Android; flujo con cuenta real pendiente |
| Volver al Perfil tras cancelar | Refrescar y retirar el evento cancelado | Ciclo de reanudación cubierto en JVM; navegación real pendiente |
| Error en primera carga de inscripciones | Error y reintento, sin mensaje de lista vacía exitosa | Prueba JVM; UI pendiente |
| Error al actualizar mis inscripciones | Conservar solo la lista de la misma cuenta | Prueba JVM; modo avión pendiente |
| Cambiar de cuenta o cerrar sesión en Perfil | Limpiar filas y descartar respuestas antiguas | Prueba JVM; UI pendiente |
| Inscripción con fecha inválida | Sección Fecha no disponible, sin cierre | Agrupación probada en JVM; UI pendiente |
| Mapa con coordenadas incompletas | Omitir el marcador inválido, mantener los demás | JVM y composable con MapView aprobados en Android 13 |
| Filtrar categoría en Mapa | Solo marcadores y resúmenes de esa categoría | JVM y prueba de retirada de marcadores/resumen aprobadas en Android 13 |
| Seleccionar un marcador | Resumen y botón Ver detalle | Toque nativo con datos ficticios aprobado en Android 13; detalle real probado desde lista del mapa |
| Elegir evento desde la lista del mapa | Alternativa accesible al marcador; resumen y detalle | Composable y navegación integrada con backend aprobados en Android 13 |
| Varios eventos en un mismo punto | Un marcador, acción Otro evento aquí | JVM y composable con MapView aprobados en Android 13 |
| Rotar con resumen del mapa abierto | Conservar selección y cámara | Restauración guardable añadida; rotación real pendiente |
| Mapa sin Internet o con fallo de teselas | Mantener eventos disponibles; advertir fondo incompleto sin cierre | Pendiente en dispositivo |

## Verificación HTTP de referencia

Durante esta etapa se consultaron sin escritura categorías, catálogo, búsqueda
combinada (`q=Compose&category=7`), lista de lugares, detalle del evento 14,
detalle del lugar 10 y eventos de ese lugar del servidor público.
Se observaron datos reales, una descripción completa y coordenadas incompletas.
Esto comprueba la disponibilidad y el formato de esas respuestas en ese momento,
no el funcionamiento de la app en un teléfono.
