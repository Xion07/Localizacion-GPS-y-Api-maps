# Explicación del código de GPSMapsApp
**Carpeta por carpeta, archivo por archivo y línea por línea.**

Cómo leer este documento:
- `L14` significa "línea 14" del archivo que se está explicando.
- Los números de línea corresponden a la versión inicial del código. Tras las mejoras (edge-to-edge, modo "Seguirme", permisos, `GeoUtils.kt` y tests) pueden estar corridos unas líneas; el contenido explicado sigue vigente.
- Los números de línea corresponden a los archivos del zip. Si editas un archivo, los números se mueven.
- Si una línea es solo `}` o está vacía, no se explica (solo cierra un bloque).
- Para exponer, no hace falta leer todo: usa el **mapa del proyecto** (sección 1) y el **flujo completo** (sección 5).

---

## 1. Mapa del proyecto (qué es cada carpeta y cada archivo)

```
GPSMapsApp/
├── settings.gradle.kts            Dónde buscar plugins/librerías y qué módulos tiene el proyecto
├── build.gradle.kts               Declara los plugins (con versión) para todo el proyecto
├── gradle.properties              Opciones globales de Gradle (memoria, AndroidX)
├── local.properties               TU API key (secreta, no va a Git)
├── local.defaults.properties      Key de relleno para que compile sin key
├── .gitignore                     Archivos que Git debe ignorar
├── gradlew / gradlew.bat          Scripts del Gradle wrapper (Mac/Linux y Windows)
├── gradle/wrapper/                Gradle wrapper: descarga la versión correcta de Gradle
│   ├── gradle-wrapper.jar         Programa que descarga y lanza Gradle
│   └── gradle-wrapper.properties  Qué versión de Gradle usar y de dónde bajarla
├── ruta_prueba_medellin.gpx       Ruta para simular movimiento en el emulador
├── docs/                          Guías en Markdown
└── app/                           El módulo de la aplicación
    ├── build.gradle.kts           Configuración y dependencias del módulo app
    └── src/main/
        ├── AndroidManifest.xml    Ficha de identidad de la app: permisos, key, pantalla inicial
        ├── res/drawable/
        │   └── ic_launcher.xml    Ícono de la app (dibujo vectorial)
        └── java/com/example/gpsmaps/     (aunque diga "java", aquí va el código Kotlin)
            ├── MainActivity.kt           Punto de entrada: arranca la pantalla
            ├── MapScreen.kt              Interfaz: mapa, buscador, pestañas, permisos
            ├── MapViewModel.kt           Estado y lógica del tracking
            ├── LocationRepository.kt     Habla con el GPS (Fused Location Provider)
            ├── GeocoderHelper.kt         Dirección ⇄ coordenadas
            └── ChallengePlaces.kt        Datos del Reto (lugares y coordenadas)
```

**¿Por qué `java/com/example/gpsmaps`?** Android Studio guarda el código en `java/` aunque sea Kotlin (por historia). Las carpetas `com/example/gpsmaps` son el **nombre del paquete** (`com.example.gpsmaps`), que debe coincidir con la primera línea `package` de cada archivo.

**Arquitectura en una imagen** (dibújala en la pizarra):
```
GPS / Wi-Fi / antenas
        ↓
Fused Location Provider (Google Play Services)
        ↓
LocationRepository   →  entrega un Flow de ubicaciones
        ↓
MapViewModel         →  guarda el estado (TrackingState) en un StateFlow
        ↓
MapScreen            →  dibuja el mapa y los paneles según el estado
```
La regla: **cada capa solo habla con la de abajo**. La pantalla nunca toca el GPS directo.

---

## 2. Glosario rápido (palabras que verás en el código)

- **Composable (`@Composable`)**: función que dibuja una parte de la interfaz. No se "crea" con XML; se escribe como código.
- **Estado (`mutableStateOf`)**: valor que, cuando cambia, hace que la pantalla se **redibuje sola** (recomposición).
- **`remember`**: guarda un valor para que no se pierda cada vez que la pantalla se redibuja.
- **`by`** (en `var x by remember {...}`): permite usar el estado como una variable normal (`x = 5`) sin escribir `.value`.
- **Corrutina**: tarea que puede **pausarse sin bloquear** la pantalla (por ejemplo, esperar la ubicación).
- **`suspend`**: marca una función que puede pausarse; solo se llama desde una corrutina u otra función `suspend`.
- **`Flow`**: una "tubería" que va entregando valores en el tiempo (cada nueva ubicación).
- **`StateFlow`**: un Flow que siempre guarda el **último valor**; ideal para el estado de la pantalla.
- **ViewModel**: guarda el estado de la pantalla y **sobrevive a rotar el teléfono**.
- **`LatLng`**: pareja `latitud, longitud`. Latitud va primero (norte/sur), longitud segundo (este/oeste). Medellín tiene latitud positiva (norte del ecuador) y longitud negativa (oeste de Greenwich).
- **Nullable (`Tipo?`)**: valor que puede ser `null` (vacío). `?.` accede si no es null; `?:` da un valor alternativo si es null.

---

## 3. Archivos de configuración

### 3.1 `settings.gradle.kts` (16 líneas)
Le dice a Gradle **dónde buscar** y **qué módulos** tiene el proyecto.

- **L1-7 `pluginManagement { ... }`**: dónde buscar **plugins** (herramientas que enseñan a Gradle a construir cosas).
  - **L3 `google()`**: repositorio de Google (el plugin de Android vive ahí).
  - **L4 `mavenCentral()`**: repositorio público grande de librerías Java/Kotlin.
  - **L5 `gradlePluginPortal()`**: repositorio oficial de plugins de Gradle.
- **L8-14 `dependencyResolutionManagement { ... }`**: dónde buscar **librerías** (dependencias).
  - **L9** `repositoriesMode.set(FAIL_ON_PROJECT_REPOS)`: prohíbe que cada módulo declare sus propios repositorios. Todo se define aquí, en un solo lugar.
  - **L10-13** repositorios para librerías: Google y Maven Central.
- **L15 `rootProject.name = "GPSMapsApp"`**: nombre del proyecto.
- **L16 `include(":app")`**: dice que el proyecto tiene un módulo llamado `app` (la carpeta `app/`).

### 3.2 `build.gradle.kts` (raíz, 6 líneas)
- **L1 `plugins {`**: lista de plugins del proyecto.
- **L2** `id("com.android.application") version "8.7.3" apply false`: el **Android Gradle Plugin (AGP)**, el que sabe compilar apps Android. `version` fija la versión; **`apply false`** significa "regístralo aquí, pero no lo apliques a la raíz: lo aplicará el módulo `app`".
- **L3** `org.jetbrains.kotlin.android` versión 2.0.21: permite compilar Kotlin en Android.
- **L4** `org.jetbrains.kotlin.plugin.compose` versión 2.0.21: el compilador de Jetpack Compose (desde Kotlin 2.0 es un plugin aparte).
- **L5** `...secrets-gradle-plugin` versión 2.0.1: lee la API key de `local.properties` y la inyecta al Manifest.

### 3.3 `gradle.properties` (3 líneas)
- **L1** `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`: Gradle puede usar hasta 2 GB de memoria y lee los archivos en UTF-8 (para tildes y ñ).
- **L2** `android.useAndroidX=true`: usa las librerías modernas `androidx.*` (obligatorio hoy).
- **L3** `kotlin.code.style=official`: estilo oficial de Kotlin.

### 3.4 `local.properties` y `local.defaults.properties`
- **`local.properties`**
  - Las líneas que empiezan con `#` son comentarios.
  - `MAPS_API_KEY=...`: **tu key**. Está en `.gitignore`, así que no se sube a GitHub.
  - Android Studio también agrega aquí `sdk.dir=...` (la ruta del Android SDK en tu computador). No la borres.
- **`local.defaults.properties`**
  - L1: comentario.
  - L2 `MAPS_API_KEY=DEFAULT_API_KEY`: valor de relleno. Si alguien no tiene `local.properties`, el proyecto compila igual (el mapa saldrá gris) en vez de fallar con un error confuso.

### 3.5 `.gitignore`
Lista de cosas que **Git no debe subir**:
- `*.iml`: archivos de módulo del IDE.
- `.gradle/`: caché de Gradle.
- `build/`: lo que se genera al compilar.
- `.idea/`: configuración personal del IDE.
- `local.properties`: **tu API key**.
- `captures/`, `.externalNativeBuild`, `.cxx`: archivos temporales de Android Studio y de código nativo.

### 3.6 `gradle/wrapper/gradle-wrapper.properties`
El **wrapper** hace que todos usen **la misma versión de Gradle** sin instalarla a mano.
- `distributionBase` y `distributionPath`: dónde se guarda Gradle descargado (en tu carpeta de usuario).
- `distributionUrl=...gradle-8.10.2-bin.zip`: de dónde baja Gradle 8.10.2 y qué versión es.
- `networkTimeout=10000`: espera máxima de red (10 segundos).
- `validateDistributionUrl=true`: verifica que la URL sea válida.
- `zipStoreBase` y `zipStorePath`: dónde se guarda el zip descargado.

`gradlew` (Mac/Linux) y `gradlew.bat` (Windows) son los **scripts** que usan el `gradle-wrapper.jar` para descargar y ejecutar esa versión. Al pulsar Sync o Run, Android Studio los usa por debajo.

### 3.7 `app/build.gradle.kts` (47 líneas) — el archivo más importante de configuración
- **L1-6 `plugins { }`**: plugins que **sí se aplican** a este módulo (sin versión, porque la versión ya se fijó en el archivo raíz).
  - **L2** `com.android.application`: este módulo es una **app** (no una librería).
  - **L3** `kotlin.android`: código en Kotlin.
  - **L4** `kotlin.plugin.compose`: compilador de Compose.
  - **L5** `secrets-gradle-plugin`: inyecta la key.
- **L8 `android { }`**: configuración propia de Android.
  - **L9** `namespace = "com.example.gpsmaps"`: paquete base del código y de las clases que Android genera (como `R`).
  - **L10** `compileSdk = 35`: versión de la API de Android **contra la que se compila** (no significa "solo corre en Android 15").
  - **L12-18 `defaultConfig`**:
    - **L13** `applicationId`: identificador **único** de la app en el teléfono y en Google. Es el nombre de paquete que se registra al restringir la API key.
    - **L14** `minSdk = 24`: versión mínima de Android (7.0). Más antiguas no pueden instalar la app.
    - **L15** `targetSdk = 35`: versión para la que fue diseñada y probada; Android activa los comportamientos de esa versión.
    - **L16** `versionCode = 1`: número entero interno; sube con cada actualización.
    - **L17** `versionName = "1.0"`: versión que ve el usuario.
  - **L20-23 `compileOptions`**: compila el código Java con compatibilidad de **Java 17** (L21 origen, L22 destino).
  - **L24** `kotlinOptions { jvmTarget = "17" }`: el código Kotlin se genera para Java 17 (debe coincidir con L21-22).
  - **L25** `buildFeatures { compose = true; buildConfig = true }`: activa **Jetpack Compose**; `buildConfig` genera una clase `BuildConfig` (no se usa ahora, es inofensiva).
- **L28-31 `secrets { }`**: configura el plugin.
  - **L29** lee la key real de `local.properties`.
  - **L30** si falta, usa el valor de `local.defaults.properties`.
- **L33-47 `dependencies { }`**: librerías que Gradle descarga. `implementation` = disponible para este módulo al compilar y ejecutar.
  - **L35** `play-services-location:21.3.0`: **Fused Location Provider** (pedir ubicación).
  - **L36** `play-services-maps:19.0.0`: el **SDK de Google Maps**.
  - **L37** `maps-compose:6.2.1`: permite usar el mapa como composable (`GoogleMap`, `Marker`...).
  - **L38** `kotlinx-coroutines-play-services:1.8.1`: da `.await()` para convertir las tareas (`Task`) de Google en funciones `suspend`.
  - **L41** `platform("...compose-bom:2024.10.01")`: el **BOM** fija de una vez las versiones de todas las librerías de Compose, por eso L42-43 no llevan versión.
  - **L42** `material3`: componentes visuales (`Button`, `Card`, `Text`, `TabRow`...).
  - **L43** `compose.ui`: base de la interfaz (`Modifier`, `Color`, `dp`...).
  - **L44** `activity-compose:1.9.3`: conecta Compose con la `Activity` (`setContent`, permisos).
  - **L45** `lifecycle-viewmodel-compose`: permite `viewModel()` dentro de composables.
  - **L46** `lifecycle-runtime-compose`: da `collectAsStateWithLifecycle()`.

### 3.8 `app/src/main/AndroidManifest.xml` (30 líneas)
La **ficha de identidad** de la app: Android la lee antes de abrirla.
- **L1** declaración XML (versión y codificación).
- **L2** `<manifest ...>`: raíz del archivo; `xmlns:android` define el prefijo `android:`.
- **L4-5** `<uses-permission ... INTERNET>`: permiso para usar internet (el mapa descarga "tiles", pedacitos de imagen).
- **L7** `ACCESS_FINE_LOCATION`: ubicación **exacta** (GPS).
- **L8** `ACCESS_COARSE_LOCATION`: ubicación **aproximada** (Wi-Fi/antenas).
  - Declarar un permiso **no basta**: la ubicación es "peligrosa" y también se pide en pantalla (ver `MapScreen.kt` L48-65).
- **L10-14 `<application ...>`**: datos de la app.
  - **L11** `allowBackup="false"`: la app no se incluye en la copia de seguridad en la nube (buena práctica de seguridad).
  - **L12** `icon="@drawable/ic_launcher"`: ícono (el archivo `res/drawable/ic_launcher.xml`).
  - **L13** `label="GPS y Maps"`: nombre bajo el ícono.
  - **L14** `theme=...Theme.Material.Light.NoActionBar`: tema del sistema sin barra superior (Compose dibuja toda la interfaz).
- **L17-19 `<meta-data>`**: dato que el **SDK de Maps** lee.
  - **L18** el nombre `com.google.android.geo.API_KEY` es el que Google busca; no se puede cambiar.
  - **L19** `${MAPS_API_KEY}`: **marcador** que el Secrets Plugin reemplaza por tu key al compilar.
- **L21-28 `<activity>`**: una pantalla.
  - **L22** `.MainActivity`: el punto indica que es relativo al `namespace` (`com.example.gpsmaps.MainActivity`).
  - **L23** `exported="true"`: obligatorio (Android 12+) para la actividad que abre el launcher.
  - **L24-27 `<intent-filter>`**: **L25** acción `MAIN` = "punto de entrada" y **L26** categoría `LAUNCHER` = "aparece en el menú de apps". Juntas hacen que esta sea la pantalla que se abre al tocar el ícono.

### 3.9 `app/src/main/res/drawable/ic_launcher.xml`
Ícono dibujado con **vectores** (no una imagen).
- `<vector ...>` con `width/height 108dp` y `viewportWidth/Height 108`: lienzo de 108×108.
- **1.er `<path>`**: `M0,0h108v108h-108z` dibuja un cuadrado azul de fondo (`#1565C0`).
- **2.º `<path>`**: dibuja un **pin de mapa** blanco con curvas.
- **3.er `<path>`**: un círculo azul en el centro del pin (el "agujero").
- Letras de `pathData`: `M` mover, `h/v` línea horizontal/vertical, `c`/`s` curvas, `a` arco, `z` cerrar.

### 3.10 `ruta_prueba_medellin.gpx`
Archivo para simular movimiento. Estructura:
- `<gpx>`: raíz.
- `<trk>`: un recorrido; `<name>` su nombre.
- `<trkseg>`: un segmento continuo.
- Cada `<trkpt lat="..." lon="...">`: un punto con su **latitud y longitud**, con `<ele>` (altura en metros) y `<time>` (hora). Son 25 puntos separados 12 s, de la Universidad de Antioquia hacia el Parque Explora (unos 570 m).

---
## 4. Código Kotlin (carpeta `app/src/main/java/com/example/gpsmaps/`)

Orden de explicación: de abajo hacia arriba (datos → GPS → lógica → pantalla → arranque).

### 4.1 `ChallengePlaces.kt` (16 líneas) — datos del Reto
- **L1** `package com.example.gpsmaps`: el paquete al que pertenece el archivo (debe coincidir con su carpeta).
- **L3** `import ...LatLng`: trae la clase que guarda latitud y longitud.
- **L5** `data class Place(val name: String, val latLng: LatLng)`: un **lugar** con un nombre y unas coordenadas. Una `data class` genera sola `toString`, `equals`, `copy`, etc.
- **L7** comentario KDoc: avisa que las coordenadas son aproximadas y hay que verificarlas.
- **L8** `object ChallengePlaces {`: `object` crea un **singleton** (una sola instancia, sin `new`). Se usa como `ChallengePlaces.list`.
- **L9** `val list = listOf(`: lista **inmutable** de lugares.
- **L10-14**: cada línea es un `Place(nombre, LatLng(latitud, longitud))`: Universidad de Antioquia, Jardín Botánico, Plaza Botero, Estadio Atanasio Girardot y Pueblito Paisa.

---

### 4.2 `LocationRepository.kt` (55 líneas) — habla con el GPS
**Imports (L3-12):**
- `SuppressLint`: silencia una advertencia del IDE (ver L20).
- `Context`: acceso al sistema Android.
- `Location`: objeto con latitud, longitud, precisión, velocidad...
- `Looper`: hilo donde se entregan los resultados (L52).
- `com.google.android.gms.location.*`: trae `FusedLocationProviderClient`, `LocationServices`, `Priority`, `LocationRequest`, `LocationCallback`, `LocationResult`.
- `CancellationTokenSource`: permite cancelar una petición de ubicación.
- `awaitClose`, `Flow`, `callbackFlow`: herramientas para convertir un callback en un Flow.
- `await`: convierte un `Task` de Google en algo que una corrutina puede esperar.

**Código:**
- **L14** `class LocationRepository(context: Context) {`: clase que recibe un `Context`. Es un *repositorio*: la única pieza que sabe cómo obtener ubicación.
- **L16-17** `private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)`: crea el **cliente del Fused Location Provider**, que combina GPS, Wi-Fi y antenas y elige la mejor fuente. `private` = solo se usa dentro de la clase.
- **L19** comentario: "ubicación actual, una sola vez".
- **L20** `@SuppressLint("MissingPermission")`: el IDE avisa que se usa una API que necesita permiso. Aquí lo silenciamos porque el permiso se **pide y verifica en la pantalla** (`MapScreen`), no en esta clase.
- **L21** `suspend fun getCurrentLocation(): Location? = try {`:
  - `suspend`: puede pausarse sin congelar la app.
  - `Location?`: devuelve una ubicación **o null**.
  - `= try {`: el `try` es una expresión; lo que devuelva es el resultado de la función.
- **L22-25** `val cts = CancellationTokenSource()` y `client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await(cts)`:
  - pide una **lectura nueva** de ubicación;
  - `PRIORITY_HIGH_ACCURACY` = máxima precisión (GPS);
  - `cts.token` permite cancelar la petición;
  - `.await(cts)` pausa la corrutina hasta que Google responda y entrega el `Location`; si la corrutina se cancela (por ejemplo, se cierra la pantalla), **también cancela la petición al GPS**.
- **L26-28** `} catch (e: Exception) { null }`: si algo falla (sin permiso, GPS apagado, etc.) devuelve `null` en vez de cerrar la app.
- **L30-36** `getLastLocation()`: igual, pero con `client.lastLocation.await()`: la **última ubicación guardada en caché**. Es instantánea, pero puede ser `null` o vieja.
- **L38** comentario: flujo continuo para el tracking.
- **L40** `fun locationUpdates(intervalMs: Long = 3000L): Flow<Location> = callbackFlow {`:
  - recibe cada cuántos milisegundos queremos una ubicación (por defecto 3 s);
  - devuelve un `Flow<Location>`: una tubería de ubicaciones;
  - `callbackFlow` convierte la API de "callbacks" de Google en un Flow.
- **L41-44** construye la **petición** (`LocationRequest`):
  - **L41** `Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)`: máxima precisión, intervalo deseado.
  - **L42** `setMinUpdateIntervalMillis(intervalMs / 2)`: acepta actualizaciones tan rápidas como la mitad del intervalo (si otra app ya pidió ubicación).
  - **L43** `setMinUpdateDistanceMeters(3f)`: **ignora** movimientos de menos de 3 metros (evita ruido y gasto).
  - **L44** `.build()`: crea el objeto.
- **L46-50** `val callback = object : LocationCallback() { ... }`: un "oyente" que Google llama cuando hay datos nuevos.
  - **L47** `onLocationResult(result)`: se ejecuta con cada resultado.
  - **L48** `result.lastLocation?.let { trySend(it) }`: toma la ubicación más reciente (si no es null) y la **mete en el Flow** con `trySend` (no bloquea).
- **L52** `client.requestLocationUpdates(request, callback, Looper.getMainLooper())`: **empieza** a pedir ubicaciones con esa petición; los resultados llegan al hilo principal.
- **L53** `awaitClose { client.removeLocationUpdates(callback) }`: se queda esperando hasta que quien escucha el Flow se **cancele**; en ese momento **detiene las actualizaciones** (apaga el GPS y ahorra batería). `callbackFlow` exige esta línea.

---

### 4.3 `GeocoderHelper.kt` (70 líneas) — dirección ⇄ coordenadas
El `Geocoder` de Android convierte texto en coordenadas (**geocodificación**) y coordenadas en texto (**geocodificación inversa**). Necesita internet. En Android 13 (API 33) cambió: ahora es **asíncrono** con un listener; antes era un método que **bloqueaba**.

**Imports (L3-12):** `Context`, `Address` (resultado del geocoder), `Geocoder`, `Build` (para leer la versión de Android), `LatLng`, `Dispatchers` (hilos), `suspendCancellableCoroutine` (puente callback → suspend), `withContext` (cambiar de hilo), `Locale` (idioma del dispositivo), `resume` (entregar el resultado a la corrutina).

- **L14** `object GeocoderHelper {`: singleton con funciones de ayuda.

**`geocode` (L16-41): "Universidad de Antioquia" → LatLng**
- **L17** `suspend fun geocode(context: Context, query: String): LatLng? {`: recibe el texto y devuelve coordenadas o `null`.
- **L18** `if (!Geocoder.isPresent()) return null`: si el dispositivo no tiene servicio de geocodificación, termina con `null`.
- **L19** crea el `Geocoder` con el idioma del dispositivo (`Locale.getDefault()`).
- **L20** `val address: Address? =`: guardará el primer resultado (o `null`). El `if/else` de abajo **devuelve un valor**.
- **L21** `if (Build.VERSION.SDK_INT >= 33) {`: en Android 13 o superior usa la versión nueva.
- **L22** `suspendCancellableCoroutine { cont ->`: pausa la corrutina hasta que se llame `cont.resume(...)`. `cont` es la "continuación" a la que se entrega el resultado.
- **L23** `geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {`: busca el texto, **máximo 1 resultado**, y avisa con un listener.
- **L24-26** `onGeocode(addresses)`: llega la lista de resultados; `cont.resume(addresses.firstOrNull())` entrega el **primero** (o `null` si la lista está vacía).
- **L27-29** `onError(...)`: si falla, `cont.resume(null)`.
- **L32** `} else {`: Android 12 o inferior.
- **L33** `withContext(Dispatchers.IO) {`: pasa a un **hilo de fondo** (porque el método viejo bloquea y congelaría la pantalla).
- **L34** `try {` / **L37** `} catch (e: Exception) { null }`: si hay error (por ejemplo sin internet), devuelve `null`.
- **L35** `@Suppress("DEPRECATION")`: silencia el aviso de que ese método está obsoleto.
- **L36** `geocoder.getFromLocationName(query, 1)?.firstOrNull()`: busca 1 resultado y toma el primero. El `?.` es porque el método viejo puede devolver `null`.
- **L40** `return address?.let { LatLng(it.latitude, it.longitude) }`: si hay dirección, la convierte en `LatLng`; si es `null`, devuelve `null`.

**`reverseGeocode` (L43-69): LatLng → "Calle 67 #53-108, Medellín"**
- **L44-46** mismo comienzo: verifica el servicio y crea el `Geocoder`.
- **L47-59** versión Android 13+: `geocoder.getFromLocation(latitud, longitud, 1, listener)` con el mismo patrón de listener y `cont.resume(...)`.
- **L60-67** versión antigua: dentro de `withContext(Dispatchers.IO)` con `try/catch`, usa `getFromLocation(lat, lng, 1)?.firstOrNull()`.
- **L68** `return address?.getAddressLine(0)`: toma la **primera línea de la dirección ya formateada** (calle, ciudad, país); `null` si no hay.

---

### 4.4 `MapViewModel.kt` (83 líneas) — estado y lógica del tracking
**Imports (L3-13):** `Application`, `Location`, `AndroidViewModel` y `viewModelScope` (ViewModel y su ámbito de corrutinas), `LatLng`, `Job` (referencia a una corrutina en marcha), `MutableStateFlow`/`StateFlow`/`asStateFlow`/`update` (estado observable), `launch` (iniciar corrutinas).

**La "foto" del estado: `TrackingState` (L15-22)**
- **L15** `data class TrackingState(`: agrupa **todo lo que la pantalla necesita saber** del tracking.
- **L16** `current: LatLng? = null`: posición actual (o `null` si aún no se sabe).
- **L17** `route: List<LatLng> = emptyList()`: los puntos del recorrido, en orden.
- **L18** `distanceMeters: Float = 0f`: distancia acumulada en metros.
- **L19** `speedKmh: Float = 0f`: velocidad en km/h.
- **L20** `accuracyMeters: Float = 0f`: precisión del GPS (radio de error en metros).
- **L21** `isTracking: Boolean = false`: si se está grabando el recorrido.
- Todo es `val` (inmutable). Para "cambiar" algo se hace una **copia** con `.copy(...)`.

**La clase (L24-83)**
- **L24** `class MapViewModel(app: Application) : AndroidViewModel(app) {`: un ViewModel que recibe la `Application` (contexto seguro, no se filtra memoria como pasaría con una `Activity`).
- **L26** `private val repo = LocationRepository(app)`: crea el repositorio que habla con el GPS.
- **L27** `private var trackingJob: Job? = null`: guarda la corrutina del tracking para poder **cancelarla** luego.
- **L29** `private val _state = MutableStateFlow(TrackingState())`: el estado **editable**, privado, con valores iniciales.
- **L30** `val state: StateFlow<TrackingState> = _state.asStateFlow()`: versión **solo lectura** que ve la pantalla. La pantalla puede mirar, pero no cambiar el estado directamente.

**`locateOnce()` (L33-43): una sola lectura**
- **L34** `viewModelScope.launch {`: inicia una corrutina ligada al ViewModel (se cancela sola cuando el ViewModel muere).
- **L35** `val loc = repo.getCurrentLocation() ?: repo.getLastLocation() ?: return@launch`: intenta la lectura actual; si es `null`, usa la de caché; si también es `null`, **sale** de la corrutina (`return@launch`).
- **L36-41** `_state.update { it.copy(current = LatLng(...), accuracyMeters = loc.accuracy) }`: actualiza el estado de forma segura con la nueva posición y precisión.

**`startTracking()` (L45-63): empezar a grabar**
- **L46** `if (trackingJob != null) return`: si ya está grabando, no hace nada (evita duplicados).
- **L47** marca `isTracking = true`.
- **L48** `trackingJob = viewModelScope.launch {`: inicia la corrutina y **guarda** su referencia.
- **L49** `repo.locationUpdates().collect { loc ->`: se suscribe al Flow; este bloque se ejecuta **cada vez** que llega una ubicación.
- **L50** crea un `LatLng` con la ubicación recibida.
- **L51** `_state.update { s ->`: actualiza el estado; `s` es el estado actual.
- **L52** `val added = s.route.lastOrNull()?.let { distanceMeters(it, point) } ?: 0f`: calcula cuántos metros hay desde el **último punto de la ruta** hasta el nuevo; si la ruta está vacía, suma `0`.
- **L53-59** `s.copy(...)`:
  - **L54** `current = point`: nueva posición actual.
  - **L55** `route = s.route + point`: lista nueva con el punto agregado.
  - **L56** `distanceMeters = s.distanceMeters + added`: acumula la distancia.
  - **L57** `speedKmh = loc.speed * 3.6f`: la velocidad llega en m/s; ×3.6 la pasa a km/h.
  - **L58** `accuracyMeters = loc.accuracy`: precisión.

**`stopTracking()` (L65-69): detener**
- **L66** `trackingJob?.cancel()`: cancela la corrutina → el Flow se cierra → se ejecuta el `awaitClose` del repositorio → **se apaga el GPS**.
- **L67** `trackingJob = null`: libera la referencia (permite iniciar de nuevo).
- **L68** `isTracking = false` y `speedKmh = 0f`.

**`clearRoute()` (L71-73):** deja `route` vacía y `distanceMeters` en 0.

**`distanceMeters(a, b)` (en `GeoUtils.kt`)**: función de nivel superior (no necesita objeto), con su test en `DistanceMetersTest`.
- `fun distanceMeters(a: LatLng, b: LatLng): Float {`: distancia en metros entre dos puntos.
- **L78** `val r = FloatArray(1)`: arreglo de 1 posición donde Android escribirá el resultado.
- **L79** `Location.distanceBetween(...)`: calcula la distancia sobre la superficie de la Tierra (no en línea recta de plano) y la guarda en `r[0]`.
- **L80** `return r[0]`.

---
### 4.5 `MapScreen.kt` (269 líneas) — la interfaz
Es el archivo más largo. Se divide en 3 zonas: **funciones de ayuda** (L27-40), **estado y permisos** (L42-89) y **lo que se dibuja** (L91-268).

**Imports (L3-25), agrupados:**
- **Permisos y sistema:** `Manifest` (nombres de permisos), `Context`, `PackageManager` (para comparar con `PERMISSION_GRANTED`), `Toast` (mensajito emergente), `ContextCompat` (verificar permisos de forma compatible), `rememberLauncherForActivityResult` y `ActivityResultContracts` (pedir permisos desde Compose).
- **Diseño:** `foundation.layout.*` (`Box`, `Column`, `Row`, `Spacer`, `Arrangement`, `fillMaxSize`, `padding`, `weight`...), `material3.*` (`Card`, `Button`, `OutlinedButton`, `OutlinedTextField`, `Text`, `TabRow`, `Tab`, `MaterialTheme`), `Alignment`, `Modifier`, `Color`, `dp`.
- **Estado de Compose:** `runtime.*` (`remember`, `mutableStateOf`, `mutableIntStateOf`, `LaunchedEffect`, `rememberCoroutineScope`, `Composable`, `getValue/setValue` para usar `by`), `LocalContext` (el Context actual).
- **ViewModel:** `collectAsStateWithLifecycle` y `viewModel`.
- **Mapa:** `CameraUpdateFactory` (movimientos de cámara), `BitmapDescriptorFactory` (colores de marcadores), `CameraPosition`, `LatLng`, y `com.google.maps.android.compose.*` (`GoogleMap`, `Marker`, `Polyline`, `Circle`, `MapProperties`, `MapUiSettings`, `MapType`, `rememberCameraPositionState`, `rememberMarkerState`).
- `launch`: iniciar corrutinas.

#### Funciones de ayuda
- **L27-31** `private fun hasLocationPermission(context: Context): Boolean =`: pregunta "¿ya tengo permiso de ubicación?".
  - **L28-29** `checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED`: ¿está concedido el permiso exacto?
  - **L29** `||` = "o".
  - **L30-31** lo mismo para el permiso aproximado. Con **uno** de los dos basta.
  - `private` = solo se usa en este archivo.
- **L33** comentario: acepta `"6.2676, -75.5685"` con punto decimal.
- **L34** `fun parseLatLng(text: String): LatLng? {` (ahora en `GeoUtils.kt`, probada en `ParseLatLngTest`): convierte texto en coordenadas, o `null` si no son válidas.
- **L35** `val p = text.trim().split(Regex("[,;\\s]+")).filter { it.isNotEmpty() }`:
  - `trim()` quita espacios de los extremos;
  - `split(Regex("[,;\\s]+"))` parte el texto por **comas, punto y coma o espacios** (uno o más seguidos);
  - `filter { it.isNotEmpty() }` descarta pedazos vacíos.
- **L36** `if (p.size != 2) return null`: deben quedar **exactamente 2 números**. Una dirección como "Calle 10, Medellín" no cumple y se descarta aquí.
- **L37** `val lat = p[0].toDoubleOrNull() ?: return null`: convierte el 1.º pedazo a número; si no es número, devuelve `null`.
- **L38** lo mismo para la longitud.
- **L39** `return if (lat in -90.0..90.0 && lng in -180.0..180.0) LatLng(lat, lng) else null`: verifica los **rangos válidos** (latitud entre −90 y 90, longitud entre −180 y 180).

#### El composable principal
- **L42** `@Composable`: esta función dibuja interfaz.
- **L43** `fun MapScreen(vm: MapViewModel = viewModel()) {`: la pantalla recibe un ViewModel; si no se le pasa, `viewModel()` **lo crea o recupera** (por eso sobrevive a rotar el teléfono).
- **L44** `val context = LocalContext.current`: el `Context` actual (lo piden el Geocoder y el Toast).
- **L45** `val scope = rememberCoroutineScope()`: un "lanzador" de corrutinas ligado a esta pantalla (para botones que hacen tareas asíncronas).
- **L46** `val state by vm.state.collectAsStateWithLifecycle()`: **convierte el `StateFlow` del ViewModel en estado de Compose**. Cada vez que el ViewModel cambia el estado, la pantalla se redibuja. "WithLifecycle" = deja de escuchar cuando la app no está visible.

**Permisos (L48-65)**
- **L49** `var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }`: estado "¿tengo permiso?"; arranca con la respuesta real.
- **L50-56** `val permissionLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result -> ... }`: prepara el **lanzador del diálogo de permisos**. El bloque `{ result -> }` se ejecuta cuando el usuario responde; `result` es un mapa `permiso → true/false`.
  - **L53-54** `hasPermission = result[FINE] == true || result[COARSE] == true`: queda en `true` si el usuario aceptó cualquiera de los dos. (`== true` porque el mapa devuelve `Boolean?`.)
  - **L55** `if (hasPermission) vm.locateOnce()`: si aceptó, busca la ubicación de inmediato.
- **L57** `LaunchedEffect(Unit) {`: bloque que se ejecuta **una sola vez** al aparecer la pantalla (`Unit` es una clave que nunca cambia).
- **L58** si ya hay permiso → `vm.locateOnce()`.
- **L59-64** si no → `permissionLauncher.launch(arrayOf(FINE, COARSE))`: **muestra el diálogo del sistema** pidiendo ambos permisos.

**Estado del mapa y la interfaz (L67-80)**
- **L68-70** `val cameraState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(6.2442, -75.5812), 12f) }`: controla la **cámara** del mapa. Empieza en Medellín con zoom 12. (Zoom: 1 = mundo, 10 = ciudad, 15 = calles, 20 = edificios.)
- **L71** `var mapType by remember { mutableStateOf(MapType.NORMAL) }`: tipo de mapa actual (normal o satélite).
- **L72** `var markers by remember { mutableStateOf(listOf<LatLng>()) }`: lista de **marcadores** del usuario; empieza vacía.
- **L73** `var query ...`: texto del buscador.
- **L74** `var addressText ... <String?>`: dirección del último punto tocado (o `null`).
- **L77** `var tab ... mutableIntStateOf(0)`: pestaña activa: 0 = Tracking, 1 = Reto.
- **L78** `challengeIndex`: número del reto actual.
- **L79** `target`: coordenadas del objetivo del reto (o `null` si no se ha mostrado).
- **L80** `revealed`: si ya se reveló el nombre del lugar.

**Seguir al usuario con la cámara (L82-89)**
- **L83** `LaunchedEffect(state.current) {`: se ejecuta **cada vez que `state.current` cambia** (nueva posición).
- **L84** `if (tab == 0) {`: solo en la pestaña Tracking (en el Reto la cámara queda libre).
- **L85-87** `state.current?.let { cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 17f), 800) }`: si hay posición, **mueve la cámara con animación** hasta ella, zoom 17, durante 800 ms.

#### Lo que se dibuja
- **L91** `Box(Modifier.fillMaxSize()) {`: un contenedor que **apila** sus hijos uno encima del otro y ocupa toda la pantalla. Abajo va el mapa; encima, el buscador y el panel.

**El mapa (L94-153)**
- **L94** `GoogleMap(`: el mapa de Google.
- **L95** `modifier = Modifier.fillMaxSize()`: ocupa todo.
- **L96** `cameraPositionState = cameraState`: conecta la cámara que creamos.
- **L97-100** `properties = MapProperties(...)`:
  - **L98** `isMyLocationEnabled = hasPermission`: muestra el **punto azul** de mi ubicación. Solo si hay permiso (si no, se lanzaría `SecurityException`).
  - **L99** `mapType = mapType`: normal o satélite.
- **L101-105** `uiSettings = MapUiSettings(...)`:
  - **L102** `zoomControlsEnabled = false`: oculta los botones +/−.
  - **L103** `myLocationButtonEnabled = hasPermission`: muestra el botón "mi ubicación" solo con permiso.
  - **L104** `compassEnabled = true`: brújula.
- **L106-109** `onMapClick = { point -> ... }`: qué pasa al **tocar el mapa**; `point` es la coordenada tocada.
  - **L107** `markers = markers + point`: crea una **lista nueva** con el punto agregado (al cambiar el estado, Compose redibuja y aparece el marcador).
  - **L108** `scope.launch { addressText = GeocoderHelper.reverseGeocode(context, point) }`: en una corrutina, pide la **dirección** de ese punto y la guarda.
- **L110** `) {`: aquí empieza el contenido **dentro del mapa** (marcadores, líneas, círculos).

*Marcadores (L111-118)*
- **L112** `markers.forEachIndexed { i, p ->`: recorre la lista con el índice `i`.
- **L113-117** `Marker(...)`:
  - **L114** `state = rememberMarkerState(key = "m$i", position = p)`: posición del marcador; la `key` única (`m0`, `m1`...) evita que se mezclen.
  - **L115** `title = "Punto ${i + 1}"`: título (cuenta desde 1).
  - **L116** `snippet = "%.5f, %.5f".format(p.latitude, p.longitude)`: subtítulo con las coordenadas con 5 decimales (≈ 1 metro de precisión).

*Objetivo del Reto (L120-131)*
- **L121** `target?.let { t ->`: solo si hay objetivo.
- **L122-126** `Marker(...)` del objetivo con `icon = ...defaultMarker(HUE_GREEN)`: marcador **verde**.
- **L128-130** `markers.lastOrNull()?.let { guess -> Polyline(...) }`: si el usuario ya tocó algo, dibuja una **línea verde** (`0xFF2E7D32`, grosor 8) desde su último marcador hasta el objetivo.

*Línea entre dos marcadores (L133-136)*
- **L134** `if (target == null && markers.size >= 2)`: solo en modo libre (sin objetivo) y con 2 o más marcadores.
- **L135** `Polyline(points = listOf(markers[0], markers[1]), color = Color.Red, width = 8f)`: línea **roja** entre los dos primeros.

*Ruta del tracking (L138-141)*
- **L139** `if (state.route.size > 1)`: una línea necesita al menos 2 puntos.
- **L140** `Polyline(points = state.route, color = Color(0xFF1565C0), width = 14f)`: línea **azul** gruesa con todos los puntos recorridos.

*Círculo de precisión (L143-152)*
- **L144** `state.current?.let {`: si hay posición actual.
- **L145-151** `Circle(...)`: **L146** centro en la posición; **L147** radio = precisión del GPS en metros, con mínimo 5 (`coerceAtLeast(5.0)`) para que siempre se vea; **L148** relleno azul casi transparente (`0x22` = transparencia); **L149** borde azul; **L150** grosor 2.

**El buscador (L155-184)**
- **L156** `Card(` : una tarjeta (rectángulo elevado).
- **L157-160** `modifier`: `.align(Alignment.TopCenter)` la pone **arriba al centro**; `.padding(12.dp)` margen; `.fillMaxWidth()` ancho completo.
- **L162** `Row(Modifier.padding(8.dp), verticalAlignment = CenterVertically) {`: fila horizontal con los elementos centrados verticalmente.
- **L163-169** `OutlinedTextField(...)`: campo de texto.
  - **L164** `value = query` y **L165** `onValueChange = { query = it }`: el texto mostrado es el estado; al escribir, se actualiza (patrón de Compose).
  - **L166** `Modifier.weight(1f)`: toma **todo el espacio sobrante** de la fila.
  - **L167** `singleLine = true`: una sola línea.
  - **L168** `placeholder`: texto gris de ayuda.
- **L170** `Spacer(Modifier.width(8.dp))`: espacio de 8 dp.
- **L171** `Button(onClick = {`: botón "Ir".
- **L172** `scope.launch {`: corrutina (el geocoder puede tardar).
- **L174** `val point = parseLatLng(query) ?: GeocoderHelper.geocode(context, query)`: **primero** intenta leer el texto como coordenadas; si no lo son (`null`), lo busca como **dirección**.
- **L175-177** si hay punto: agrega el marcador (L176) y mueve la cámara con zoom 16 (L177).
- **L178-180** si no: `Toast` "No se encontró".
- **L182** `}) { Text("Ir") }`: cierra el `onClick` y pone el texto del botón.

**El panel inferior con pestañas (L186-267)**
- **L187-192** `Card(...)` con `.align(BottomCenter)`: tarjeta **abajo al centro**.
- **L193** `Column {`: apila sus hijos verticalmente.
- **L194-197** `TabRow(selectedTabIndex = tab) { ... }`: fila de pestañas; la seleccionada es `tab`.
  - **L195** `Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tracking") })`: pestaña 0.
  - **L196** pestaña 1 "Reto".
- **L199** `Column(Modifier.padding(12.dp)) {`: contenido del panel con margen.
- **L200** `if (tab == 0) {` → **pestaña Tracking**; **L231** `} else {` → **pestaña Reto**.

*Pestaña Tracking (L201-230)*
- **L202-206** `Text("Distancia: %.0f m | Vel: %.1f km/h | Precisión: %.0f m".format(...), style = bodyMedium)`: muestra distancia, velocidad y precisión. `%.0f` = número con 0 decimales; `%.1f` = 1 decimal.
- **L207-209** si hay 2 o más marcadores, muestra la distancia entre el 1.º y el 2.º usando `distanceMeters`.
- **L210** `addressText?.let { Text("Último punto: $it") }`: si hay dirección, la muestra.
- **L212** `Spacer(Modifier.height(8.dp))`: espacio vertical.
- **L213** `Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {`: fila de botones separados 8 dp.
- **L214-220** botón **Iniciar/Detener**: si **no** está grabando (`!state.isTracking`) muestra **Iniciar** (`enabled = hasPermission`, llama `vm.startTracking()`); si está grabando, **Detener** (`vm.stopTracking()`).
- **L221-225** **Limpiar**: `vm.clearRoute()`, `markers = emptyList()`, `addressText = null`.
- **L226-229** **Mapa**: alterna `MapType.NORMAL` ⇄ `MapType.SATELLITE`.

*Pestaña Reto (L231-264)*
- **L233** `val place = ChallengePlaces.list[challengeIndex]`: el lugar del reto actual.
- **L234-238** muestra "Reto n/total: lat, lng" (4 decimales ≈ 11 m), en estilo `titleMedium`.
- **L239** `if (revealed) Text("📍 ${place.name}")`: muestra el nombre solo si ya se reveló.
- **L240-244** si hay objetivo **y** un marcador del usuario, muestra a cuántos metros quedó.
- **L246** espacio.
- **L247** fila de 3 botones.
- **L248-253 "Ver"**: `Modifier.weight(1f)` (los 3 botones miden lo mismo); `target = place.latLng` pone el objetivo; `scope.launch { cameraState.animate(... zoom 15 ...) }` vuela hasta él.
- **L254-256 "Nombre"**: `revealed = true`.
- **L257-262 "Siguiente"**:
  - **L258** `challengeIndex = (challengeIndex + 1) % ChallengePlaces.list.size`: pasa al siguiente; el `%` (resto) hace que después del último vuelva al primero.
  - **L259-261** reinicia `target`, `revealed` y `markers`.
- **L264-268** llaves que cierran `else`, las `Column`, las `Card`, el `Box` y la función.

---

### 4.6 `MainActivity.kt` (17 líneas) — el arranque
- **L1** `package com.example.gpsmaps`.
- **L3** `import android.os.Bundle`: contenedor de datos del estado guardado.
- **L4** `ComponentActivity`: la `Activity` base compatible con Compose.
- **L5** `setContent`: define la interfaz con Compose.
- **L6** `MaterialTheme`: colores, tipografía y formas de Material Design.
- **L8** `class MainActivity : ComponentActivity() {`: la **pantalla (Activity)** que se abre al tocar el ícono (la que declara el Manifest).
- **L9** `override fun onCreate(savedInstanceState: Bundle?) {`: Android llama esta función cuando **crea** la pantalla.
- **L10** `super.onCreate(savedInstanceState)`: ejecuta el código de la clase base (obligatorio).
- **L11** `setContent {`: reemplaza los viejos layouts XML: aquí se escribe la interfaz en código.
- **L12** `MaterialTheme {`: aplica el tema a todo lo de adentro.
- **L13** `MapScreen()`: dibuja nuestra pantalla principal.

---

## 5. El flujo completo: qué pasa, en orden

### 5.1 Cuando abres la app
1. Android lee el **Manifest** y abre `MainActivity` (la que tiene `MAIN` + `LAUNCHER`).
2. `onCreate` llama `setContent` → `MapScreen()`.
3. `MapScreen` crea/recupera el `MapViewModel` y el mapa se carga con la **API key** que el Manifest recibió de `local.properties`.
4. `LaunchedEffect(Unit)` revisa si hay permiso:
   - **Sí** → `vm.locateOnce()`.
   - **No** → muestra el diálogo de permisos; al responder se ejecuta `locateOnce()` si aceptó.
5. `locateOnce()` pide la ubicación al repositorio → actualiza `state.current` → `LaunchedEffect(state.current)` mueve la cámara allí.

### 5.2 Cuando pulsas "Iniciar"
1. `vm.startTracking()` pone `isTracking = true` y lanza una corrutina.
2. La corrutina llama `repo.locationUpdates()` → el Fused Location Provider empieza a pedir GPS cada ~3 s (si te moviste ≥ 3 m).
3. Cada ubicación nueva entra al Flow (`trySend`) → el ViewModel la recibe en `collect`.
4. El ViewModel suma el punto a `route`, acumula distancia, calcula velocidad y **actualiza el estado**.
5. `MapScreen` ve el cambio de `state` → se redibuja: sube el número de distancia, crece la `Polyline` azul y la cámara sigue al punto.

### 5.3 Cuando pulsas "Detener"
1. `vm.stopTracking()` cancela la corrutina.
2. Al cancelarse el Flow, se ejecuta `awaitClose` → `removeLocationUpdates` → **el GPS se apaga**.
3. El botón vuelve a decir "Iniciar".

### 5.4 Cuando tocas el mapa
1. `onMapClick` agrega el punto a `markers` → aparece un `Marker`.
2. En paralelo, una corrutina pide la dirección (`reverseGeocode`) → aparece "Último punto: ...".
3. Con 2 marcadores aparece la línea roja y la distancia entre ellos.

### 5.5 Cuando escribes en el buscador y pulsas "Ir"
1. Se intenta `parseLatLng(query)`: ¿son coordenadas?
2. Si no, `GeocoderHelper.geocode(...)`: ¿es una dirección o un lugar?
3. Con el resultado: marcador nuevo + la cámara vuela allí. Si no hay resultado: Toast "No se encontró".

---

## 6. Ideas para modificar el código (y qué archivo tocar)
| Quiero... | Archivo y línea |
|---|---|
| Cambiar los lugares del Reto | `ChallengePlaces.kt` L10-14 |
| Que el GPS se actualice más seguido | `LocationRepository.kt` L40 (`intervalMs`) y L43 (distancia mínima) |
| Cambiar el color de la ruta | `MapScreen.kt` L140 |
| Cambiar la ciudad inicial del mapa | `MapScreen.kt` L69 |
| Cambiar el zoom al seguir al usuario | `MapScreen.kt` L86 (`17f`) |
| Agregar otro botón al panel | `MapScreen.kt` dentro de las filas L213-230 o L247-263 |
| Cambiar el nombre de la app | `AndroidManifest.xml` L13 |
| Cambiar la versión mínima de Android | `app/build.gradle.kts` L14 |

---

## 7. Si te preguntan "¿y esta línea por qué?"
- **¿Por qué `private val _state` y `val state`?** Para que **solo el ViewModel** pueda cambiar el estado; la pantalla solo lo lee. Evita errores difíciles de rastrear.
- **¿Por qué listas nuevas (`markers + point`) en vez de agregar a la misma?** Compose detecta el cambio cuando la **referencia cambia**; modificar una lista existente por dentro no dispara el redibujado.
- **¿Por qué `suspend` y corrutinas?** Pedir ubicación o dirección tarda; con corrutinas la app **espera sin congelarse**.
- **¿Por qué el `awaitClose`?** Sin él, el GPS seguiría encendido aunque ya no escuches (gasta batería).
- **¿Por qué `?.let` y `?:` por todos lados?** Casi todo lo del GPS **puede ser null** (sin señal, sin permiso). Kotlin te obliga a decidir qué hacer en ese caso, y por eso la app no se cierra.
- **¿Por qué dos permisos?** FINE da precisión de metros; COARSE de cientos de metros. Desde Android 12 el usuario puede elegir la aproximada.
