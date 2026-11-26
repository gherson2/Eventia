# Preparación de la entrega

Esta lista no certifica una entrega completa. Registrar los resultados reales en
[PRUEBAS_MANUALES.md](PRUEBAS_MANUALES.md) antes de entregar.

## Lista de control

- Confirmar con el docente el uso de osmdroid para RF-07 y RF-08.
- Confirmar la versión instalada de Android Studio registrada en el README (Help → About).
- Usar las credenciales de prueba autorizadas por la asignatura; no publicar
  contraseñas personales, tokens ni `local.properties`.
- Ejecutar compilación, pruebas JVM, Lint y pruebas instrumentadas.
- Probar con el backend: acceso, inscripción, cancelación y lista del Perfil.
- Comprobar modo avión, reapertura, rotación, indicaciones y mapa.
- Grabar y revisar el video de 3–4 minutos.
- Revisar el diff, crear el commit final y publicar el repositorio.
- Solo sobre el commit realmente entregado, crear y publicar `v1.0`:

```bash
git tag -a v1.0 -m "Entrega de Eventia"
git push origin v1.0
```

No mover una etiqueta existente sin revisar qué versión identifica. No se exige
entregar un APK. El historial debe reflejar el trabajo real, sin fechas inventadas.

## Guion de demostración (3:45)

| Tiempo | Demostración |
|---|---|
| 0:00–0:35 | Abrir catálogo real; búsqueda + categoría; limpiar filtros. |
| 0:35–1:05 | Detalle del evento, cupos, lugar y regreso conservando posición. |
| 1:05–1:40 | Mapa, filtro, marcador, resumen y detalle; abrir indicaciones. |
| 1:40–2:25 | Guardar favorito; quitar y Deshacer; activar modo avión y reabrir para mostrar favoritos, detalle local y caché con antigüedad. |
| 2:25–3:20 | Recuperar conexión; acceder con cuenta de prueba; inscribirse, mostrar cupos y Perfil, cancelar y volver a comprobar Perfil. |
| 3:20–3:45 | Mostrar rotación y un estado vacío/error; explicar límites conocidos. |

Preparar antes los eventos y la cuenta autorizada. No presentar un error simulado
como si fuera una respuesta real del servidor. No mostrar la contraseña ni el token.

## Preparación de la defensa individual

- Explicar `Screen → ViewModel → Repository → Retrofit/Room/DataStore`.
- Localizar el debounce y los filtros en `ExplorarViewModel`.
- Explicar por qué los DTO se convierten a modelos antes de llegar a la UI.
- Mostrar cómo el repositorio elige entre API y caché, y cómo Room conserva favoritos.
- Explicar `SavedStateHandle`, estado guardable de Compose y posición de las listas.
- Localizar Bearer/401, sesión persistente y protección ante respuestas de otra cuenta.
- Explicar cupos del servidor, conflicto 409 y bloqueo de doble operación.
- Mostrar validación de coordenadas, agrupación de marcadores y lifecycle de `MapView`.
- Practicar un cambio pequeño: agregar un campo a una tarjeta o una validación,
  localizar la prueba adecuada y comprobar el resultado.
