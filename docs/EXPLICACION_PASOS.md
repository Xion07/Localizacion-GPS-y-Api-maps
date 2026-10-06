# Qué hace cada paso y por qué (para explicarlo en clase)

Cada paso tiene: **Qué hace** · **Por qué** · **Frase para decir en voz alta**.

---

## Paso 1. Crear el proyecto (Empty Activity, paquete `com.example.gpsmaps`)
**Qué hace:** genera el esqueleto de una app con Kotlin y Jetpack Compose: una `MainActivity`, un tema y los archivos Gradle.

**Por qué:**
- El **package name** es la identidad de la app. Google lo usa para validar la API key, así que debe coincidir exactamente con el que se registre en la restricción de la key.
- **minSdk 24** significa que la app corre desde Android 7.0 en adelante.

> "Empezamos con un proyecto vacío. El nombre del paquete importa porque es la identidad de la app ante Google."

---

## Paso 2. Plugin de la API key en el Gradle raíz
**Qué hace:** declara el *Secrets Gradle Plugin* y su versión. `apply false` quiere decir "regístralo, pero no lo apliques al proyecto raíz; lo aplicará el módulo `app`".

**Por qué:** Gradle es el sistema que compila la app. Los plugins le enseñan trucos nuevos; este sabe leer la key de un archivo y pasarla al Manifest.

> "Gradle es el encargado de construir la app. Aquí le avisamos que vamos a usar un plugin."

---

## Paso 3. Plugin, bloque `secrets` y dependencias en `app/build.gradle.kts`
**Qué hace:**
- Aplica el plugin y le dice de qué archivos leer: `local.properties` (la key real) y `local.defaults.properties` (un valor de relleno).
- Las **dependencias** son librerías que Gradle descarga:

| Dependencia | Para qué sirve |
|---|---|
| `play-services-location` | El **Fused Location Provider**: pide ubicación sin que tú manejes GPS, Wi-Fi o antenas por separado |
| `play-services-maps` | El **SDK de Google Maps** (el mapa en sí) |
| `maps-compose` | Permite usar `GoogleMap { ... }`, `Marker`, `Polyline` como funciones composables |
| `kotlinx-coroutines-play-services` | Da `.await()`: convierte las tareas (`Task`) de Google en funciones que se pueden pausar con corrutinas |
| `lifecycle-viewmodel-compose` | Permite `viewModel()` dentro de la UI |
| `lifecycle-runtime-compose` | Da `collectAsStateWithLifecycle()`: la UI deja de escuchar la ubicación cuando la app no está visible (ahorra batería) |

**Por qué `local.defaults.properties`:** si alguien no tiene la key, el proyecto igual compila (el mapa sale gris), en vez de fallar con un error confuso.

**Sync:** Gradle lee los archivos y descarga las librerías. Sin Sync, el código sale en rojo.

> "Cada dependencia es una pieza: una pide la ubicación, otra dibuja el mapa y otra conecta el mapa con Compose."

---

## Paso 4. La API key (`local.properties`)
**Qué hace:** guarda la key en un archivo local, fuera del código.

**Por qué:**
- La API key le dice a Google **quién está usando los mapas y a quién se le cobra**. Si se filtra, otra persona puede gastar la cuota de su dueño.
- `local.properties` está en `.gitignore`, así que no se sube a GitHub por accidente.
- Esta key es prestada solo para la clase, así que no debe quedar en ningún repositorio.

> "La key es como una contraseña de facturación. Por eso no va escrita en el código."

---

## Paso 5. AndroidManifest.xml
**Qué hace:** declara cosas que Android debe saber antes de abrir la app.

- `INTERNET`: el mapa se descarga en pedacitos (tiles) desde internet.
- `ACCESS_FINE_LOCATION`: ubicación **exacta** (GPS, unos metros).
- `ACCESS_COARSE_LOCATION`: ubicación **aproximada** (Wi-Fi y antenas, cientos de metros).
- `meta-data` con `com.google.android.geo.API_KEY`: el SDK de Maps busca la key **con ese nombre exacto**. `${MAPS_API_KEY}` es el valor que inyecta el plugin.

**Por qué se piden las dos de ubicación:** desde Android 12 el usuario puede darte solo la aproximada. Pedir ambas deja que elija.

**Ojo (pregunta típica):** declarar el permiso en el Manifest **no basta**. Los permisos de ubicación son "peligrosos", así que la app debe pedirlos otra vez en pantalla (Paso de permisos en `MapScreen.kt`).

> "El Manifest es la lista de lo que la app necesita. Aquí también se entrega la key al SDK de Maps."

---

## Paso 6. Copiar los 6 archivos de código
**Qué hace:** cada archivo tiene **una sola responsabilidad**.

| Archivo | Responsabilidad |
|---|---|
| `LocationRepository.kt` | Habla con el Fused Location Provider: ubicación una vez, última conocida y en tiempo real (`Flow`) |
| `GeocoderHelper.kt` | Convierte dirección ⇄ coordenadas |
| `MapViewModel.kt` | Guarda el **estado** (posición, ruta, distancia, velocidad) y sobrevive a rotar la pantalla |
| `MapScreen.kt` | La **interfaz**: mapa, permisos, buscador, pestañas Tracking y Reto |
| `ChallengePlaces.kt` | Los datos de la actividad (lugares y coordenadas) |
| `MainActivity.kt` | Punto de entrada: muestra `MapScreen` |

**Flujo de datos** (dibújalo en la pizarra):
```
GPS/Wi-Fi → Fused Location → LocationRepository (Flow)
          → MapViewModel (StateFlow) → MapScreen (se redibuja sola)
```
**Por qué así:** si la UI hablara directo con el GPS, el código se volvería un enredo y se perdería el estado al rotar el teléfono. Con el ViewModel en medio, la UI solo "pinta" lo que hay en el estado.

> "La UI nunca habla con el GPS directamente: le pide al ViewModel, y este al repositorio."

---

## Paso 7. Ejecutar la app y, si el mapa sale gris, registrar tu SHA-1

**Qué hacer:**
1. Abre un emulador con **Google Play**, pulsa **Run** y acepta el permiso de ubicación.
2. En el emulador: *Extended controls (⋯) → Location* y fija un punto, o carga un archivo **GPX** para simular que te mueves. Luego pulsa **Iniciar** en la app.
3. Si el mapa sale gris, obtén tu huella **SHA-1**: en la Terminal de Android Studio ejecuta `./gradlew signingReport` (Windows: `gradlew.bat signingReport`) y copia el **SHA1** de la variante `debug`.
4. Pásasela al dueño de la key. Él la agrega, junto con el paquete `com.example.gpsmaps`, en Google Cloud Console → Credenciales → la key → Restricciones de aplicaciones Android.
5. Espera unos minutos y vuelve a ejecutar.

**Por qué:**
- **Emulador con Google Play:** Maps y Fused Location son parte de *Google Play Services*. Un emulador sin eso no los tiene.
- **Ubicación simulada:** el emulador no tiene GPS real. Un GPX es una lista de coordenadas en secuencia y hace que la app crea que alguien camina.
- **SHA-1:** es la **huella del certificado con el que se firma la app**. La key está restringida a `paquete + SHA-1`, así que si alguien copia la key no puede usarla desde otra app, porque su huella no coincide.
- **Cada compañero pasa la suya:** cada computador crea su propio `debug.keystore`, así que la huella de debug es distinta en cada equipo. Si no está registrada, Google rechaza la petición y el mapa queda gris.

> "El emulador no se mueve solo, así que le damos una ruta GPX. Y si el mapa sale gris, casi siempre es porque Google no reconoce nuestra huella SHA-1."
