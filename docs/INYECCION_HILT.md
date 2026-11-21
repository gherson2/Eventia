# Avance pequeño: inyección de dependencias con Hilt

Este cambio cubre el requisito técnico de Hilt sobre las funciones ya existentes.
No añade favoritos, caché, sesión, inscripciones ni mapa. El avance porcentual
es orientativo; la nota depende de toda la rúbrica y de la defensa.

## Qué cambió

1. `EventiaApplication` usa `@HiltAndroidApp` y se registra en el manifiesto.
2. `MainActivity` usa `@AndroidEntryPoint` para recibir las fábricas de Hilt.
3. `RedModule` proporciona Retrofit y `EventiaApi`, ambos singleton.
4. `EventRepositoryImpl` tiene constructor `@Inject`.
5. `RepositoriosModule` vincula `EventRepository` con su implementación mediante
   `@Binds` y mantiene una instancia del repositorio por aplicación.
6. Los cuatro ViewModel usan `@HiltViewModel` y constructor `@Inject`.
7. Cada destino de Navigation Compose obtiene su ViewModel con `hiltViewModel()`.
8. Se retiran los contenedores manuales ya reemplazados para no mantener dos
   formas paralelas de construir las mismas dependencias.

## Flujo

```text
EventiaApplication (@HiltAndroidApp)
  └─ SingletonComponent
      ├─ Retrofit → EventiaApi                         [RedModule]
      └─ EventRepository → EventRepositoryImpl(API)   [RepositoriosModule]
          └─ ViewModel del destino + SavedStateHandle
              └─ StateFlow → pantalla
```

`@Provides` sirve para crear objetos de bibliotecas externas como Retrofit.
`@Binds` indica qué implementación corresponde a una interfaz.
`@Singleton` comparte una instancia dentro del componente de aplicación.
`@HiltViewModel` permite que Hilt cree un ViewModel respetando su ciclo de vida.

## Estado y navegación

Hilt no reemplaza `SavedStateHandle`, `StateFlow` ni Navigation Compose:
solo resuelve las dependencias. Los identificadores de las rutas se reciben
en `SavedStateHandle`, y búsqueda/categoría siguen guardándose en él.
No se hicieron singleton los ViewModel porque cada pantalla necesita su propio estado.

## Cómo verificar

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

La compilación ejecuta KSP y valida que el grafo de Hilt pueda construirse.
Las pruebas JVM existentes validan que los ViewModel y repositorios conserven
su comportamiento. `RedModuleTest` agrega dos comprobaciones de los proveedores:
la URL configurada se conserva y la API usa el Retrofit recibido. Estas pruebas
no verifican los scopes en ejecución ni sustituyen el arranque real de la app.

Pendiente en dispositivo: iniciar la aplicación, abrir evento y lugar, regresar
y rotar para confirmar la creación de los ViewModel y la conservación del estado.

## Resultado de esta etapa

La compilación debug, las 26 pruebas JVM y Lint terminaron correctamente.
Hilt generó y compiló el componente de aplicación. Lint conserva 21 advertencias
no bloqueantes. No se ejecutó esta versión en un dispositivo.
