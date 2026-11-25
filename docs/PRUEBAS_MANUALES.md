# Escenarios de verificación · catálogo y lugares

No marcar como aprobado sin ejecutar la comprobación en un dispositivo o emulador.
Las verificaciones automáticas de repositorio/ViewModel no certifican la interfaz.

| Escenario | Resultado esperado | Estado |
|---|---|---|
| Abrir Explorar con Internet | Datos de la API, imágenes y fechas locales | Pendiente |
| Arrancar tras integrar Hilt | Application y actividad inicializan el grafo sin cierres | Pendiente |
| Abrir evento y lugar con Hilt | ViewModel recibe el id correcto mediante SavedStateHandle | Pendiente |
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
| Abrir evento sin cupos | Mostrar cero cupos y aviso; inscripción aún no implementada | Pendiente |
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
| Reabrir una base Room de prueba | Mantener favorito y poder eliminarlo | FavoritosPersistenciaTest añadido; ejecución pendiente |
| Abrir detalle de favorito sin red | Mostrar detalle local y advertencia de copia guardada | Cubierto en JVM; modo avión pendiente |
| Abrir evento no guardado sin red | Error comprensible sin inventar datos | Cubierto en JVM; UI pendiente |
| Recuperar conexión y actualizar el detalle | Consultar la API y retirar la marca de copia guardada | Cubierto en JVM; UI pendiente |
| Perder o recuperar conexión | Mostrar o retirar el aviso global | Estado observable cubierto en JVM; callbacks Android pendientes |
| Servidor 404 con favorito guardado | Informar evento no disponible, sin borrar el favorito | Cubierto en JVM; UI pendiente |
| Reabrir el catálogo en modo avión tras descargarlo | Lista local y antigüedad visible | Repositorio cubierto en JVM; dispositivo pendiente |
| Buscar y filtrar en el catálogo sin conexión | Solo coincidencias de la copia disponible | Prueba JVM; UI pendiente |
| Actualizar tras recuperar conexión | Reemplazar la copia y retirar la marca de datos guardados | Prueba JVM; UI pendiente |
| Buscar y luego limpiar filtros | No reemplazar el catálogo completo por los resultados de búsqueda | Prueba JVM; UI pendiente |
| Migrar una instalación con favoritos | Mantener favoritos al agregar caché Room | CatalogoMigracionTest añadido; ejecución pendiente |
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
| Ver mis inscripciones en Perfil | Próximas ascendentes y pasadas descendentes | Repositorio, agrupación y ViewModel en JVM; UI pendiente |
| Abrir una inscripción desde Perfil | Detalle del evento y regreso conservando navegación | Prueba instrumentada añadida; ejecución pendiente |
| Volver al Perfil tras cancelar | Refrescar y retirar el evento cancelado | Ciclo de reanudación cubierto en JVM; navegación real pendiente |
| Error en primera carga de inscripciones | Error y reintento, sin mensaje de lista vacía exitosa | Prueba JVM; UI pendiente |
| Error al actualizar mis inscripciones | Conservar solo la lista de la misma cuenta | Prueba JVM; modo avión pendiente |
| Cambiar de cuenta o cerrar sesión en Perfil | Limpiar filas y descartar respuestas antiguas | Prueba JVM; UI pendiente |
| Inscripción con fecha inválida | Sección Fecha no disponible, sin cierre | Agrupación probada en JVM; UI pendiente |

## Verificación HTTP de referencia

Durante esta etapa se consultaron sin escritura categorías, catálogo, búsqueda
combinada (`q=Compose&category=7`), lista de lugares, detalle del evento 14,
detalle del lugar 10 y eventos de ese lugar del servidor público.
Se observaron datos reales, una descripción completa y coordenadas incompletas.
Esto comprueba la disponibilidad y el formato de esas respuestas en ese momento,
no el funcionamiento de la app en un teléfono.
