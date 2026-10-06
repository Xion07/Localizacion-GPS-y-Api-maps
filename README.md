# GPSMapsApp — GPS y Google Maps en Android (Kotlin + Jetpack Compose)

App de ejemplo para exponer **Localización (GPS)** y **Google Maps API**:
permisos, ubicación en tiempo real, marcadores, rutas, Geocoder y una actividad "Reto de coordenadas".

## Inicio rápido
1. Abre la carpeta en Android Studio (File → Open).
2. **JDK de Gradle:** *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* → elige un **JDK 17 o 21**
   (si no tienes, *Download JDK…* → versión 17). Gradle 8.10.2 no corre con Java 25, que es el que trae Android Studio reciente.
   Luego espera el Sync de Gradle.
3. Pide la key **por privado** al dueño y pégala en `local.properties` (en la raíz; Android Studio lo crea con `sdk.dir`):
   `MAPS_API_KEY=tu_key` — sin comillas ni espacios.
4. Crea un emulador con **Google APIs** o **Google Play** y pulsa Run.
5. Fija la ubicación del emulador (⋯ → Location) o carga `ruta_prueba_medellin.gpx` en *Location → Routes* y dale Play.

### Si algo falla
| Síntoma | Causa y solución |
|---|---|
| Sync falla con "Unsupported class file major version" o similar | El Gradle JDK es Java 25: cámbialo a 17/21 (paso 2). |
| El mapa sale gris o en blanco | Falta la key en `local.properties`, está mal copiada, o la key se restringió/borró. Mira Logcat filtrando por `Authorization failure`. |
| No aparece el punto azul | Acepta el permiso de ubicación (o usa el botón **Dar permiso**) y fija una ubicación en el emulador. |

Tests unitarios: `./gradlew testDebugUnitTest`.

El paso a paso completo, con puntos de control, está en **`docs/GUIA_DIDACTICA.md`**.
Qué hace cada pieza y por qué (para exponer): **`docs/EXPLICACION_PASOS.md`**.
Explicación del código línea por línea: **`docs/EXPLICACION_CODIGO.md`**.

## Contenido
| Ruta | Qué es |
|---|---|
| `app/src/main/java/com/example/gpsmaps/` | Código: `MainActivity`, `MapScreen`, `MapViewModel`, `LocationRepository`, `GeocoderHelper`, `GeoUtils`, `ChallengePlaces` |
| `app/src/main/AndroidManifest.xml` | Permisos y API key |
| `app/src/test/java/com/example/gpsmaps/` | Tests unitarios (`./gradlew testDebugUnitTest`): `parseLatLng` y `distanceMeters` |
| `ruta_prueba_medellin.gpx` | Ruta para simular movimiento en el emulador |
| `gradlew`, `gradle/wrapper/` | Gradle wrapper (Gradle 8.10.2) |
| `local.properties` | Aquí va la key (no se sube a Git) |
| `local.defaults.properties` | Valor por defecto para que el proyecto compile sin key |

## Versiones
Android Gradle Plugin 8.7.3 · Kotlin 2.0.21 · Gradle 8.10.2 · compileSdk 35 · minSdk 24 · Compose BOM 2024.10.01 · maps-compose 6.2.1.

## Seguridad de la key
La key de la clase es prestada. No la subas a GitHub ni la muestres en pantalla, y su dueño debe eliminarla o rotarla al terminar.
