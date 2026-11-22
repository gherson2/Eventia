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
| Abrir sin Internet desde cero | Error comprensible con reintento, sin cierre | Pendiente; caché aún no implementada |
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
| Listar favoritos en modo avión | Mostrar las copias locales y permitir quitarlas | Pendiente; no incluye abrir detalle offline |
| Reabrir una base Room de prueba | Mantener favorito y poder eliminarlo | FavoritosPersistenciaTest añadido; ejecución pendiente |
| Abrir detalle de favorito sin red | Mostrar detalle local | No implementado en esta etapa |

## Verificación HTTP de referencia

Durante esta etapa se consultaron sin escritura categorías, catálogo, búsqueda
combinada (`q=Compose&category=7`), lista de lugares, detalle del evento 14,
detalle del lugar 10 y eventos de ese lugar del servidor público.
Se observaron datos reales, una descripción completa y coordenadas incompletas.
Esto comprueba la disponibilidad y el formato de esas respuestas en ese momento,
no el funcionamiento de la app en un teléfono.
