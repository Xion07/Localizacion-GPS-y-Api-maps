# Guía didáctica: abrir, ejecutar y probar GPSMapsApp

Cada paso tiene tres partes:
- 🔧 **Qué haces**
- ✅ **Qué debes ver** (tu punto de control: si no lo ves, no sigas)
- 💡 **Por qué** (para que lo entiendas, no solo lo copies)

Tiempo estimado: 20 a 30 minutos la primera vez (la mayor parte es esperar descargas).

---

## PARTE A — Antes de empezar

### Qué necesitas
| Cosa | Detalle |
|---|---|
| Android Studio | La versión estable más reciente. Trae todo lo demás (Java, emulador). |
| Internet | La primera vez descarga varios cientos de MB (Gradle, librerías, imagen del emulador). |
| Espacio en disco | Unos 10 GB libres, contando el emulador. |
| La API key de la clase | Te la pasa David **por mensaje privado**. No la publiques. |
| El zip `GPSMapsApp.zip` | Se descomprime en el Paso 1. |

💡 **Por qué una API key:** Google no regala el mapa sin saber quién lo usa. La key identifica a quién se le cuenta (y se le cobra) cada carga de mapa. Por eso es como una contraseña: no se comparte en público.

---

## PARTE B — Paso a paso

### Paso 1. Descomprimir el zip
🔧 Clic derecho en `GPSMapsApp.zip` → **Extraer todo / Descomprimir**.
Deja la carpeta en una ruta corta y simple, por ejemplo `C:\proyectos\GPSMapsApp` o `~/proyectos/GPSMapsApp`. Evita rutas con tildes, espacios o carpetas de OneDrive.

✅ **Debes ver** dentro de la carpeta: `app`, `gradle`, `gradlew`, `settings.gradle.kts`, `local.properties`, `ruta_prueba_medellin.gpx`.

💡 **Por qué una ruta simple:** Windows tiene un límite de largo de ruta y algunas herramientas se confunden con tildes y espacios. Es la causa más tonta de errores raros.

---

### Paso 2. Abrir el proyecto en Android Studio
🔧
1. Abre Android Studio. En la pantalla de bienvenida pulsa **Open** (o **File → Open**).
2. Elige la carpeta `GPSMapsApp` (la que contiene `settings.gradle.kts`, **no** la carpeta `app`).
3. Si aparece "Trust and Open Project", pulsa **Trust Project**.

✅ **Debes ver** el proyecto cargando y, abajo a la derecha, una barra de progreso que dice algo como *"Gradle sync"* o *"Downloading..."*.

💡 **Por qué abrir la carpeta raíz:** Android Studio se guía por `settings.gradle.kts` para saber qué módulos tiene el proyecto. Si abres `app` directamente, no encuentra la configuración.

---

### Paso 3. Esperar el Sync de Gradle
🔧 No toques nada. Espera a que la barra de abajo desaparezca. La primera vez puede tardar de 3 a 10 minutos.

Si Android Studio te pide algo, esto es lo habitual:
| Mensaje | Qué hacer |
|---|---|
| "Install missing SDK package: Android SDK Platform 35" | Pulsa **Install** y acepta las licencias. |
| "Gradle JDK is not configured / invalid" | **File → Settings** (Mac: **Android Studio → Settings**) → **Build, Execution, Deployment → Build Tools → Gradle** → *Gradle JDK*: elige **jbr-17** o **jbr-21** (el que viene incluido). |
| "A new version of Android Gradle Plugin is available" (Upgrade Assistant) | Elige **Remind me later / Don't ask**. Primero prueba que funcione tal cual. |

✅ **Debes ver** en la pestaña **Build** (abajo): `BUILD SUCCESSFUL` o simplemente que termina sin errores en rojo.

💡 **Por qué se descarga tanto:** Gradle es el sistema que construye la app. Descarga su propia versión (por eso el archivo `gradle-wrapper.properties`) y luego todas las librerías que el proyecto declara (mapa, ubicación, Compose...). Se hace una sola vez; después queda en caché.

---

### Paso 4. Pegar la API key
🔧
1. En el panel izquierdo, cambia la vista de **Android** a **Project** (menú desplegable arriba del árbol de archivos).
2. Abre `local.properties` (en la raíz).
3. Reemplaza `PEGA_AQUI_LA_KEY` por la key, sin comillas ni espacios:
```properties
MAPS_API_KEY=AIza...tu_key...
```
4. Si Android Studio agregó una línea `sdk.dir=...`, **no la borres**.
5. Pulsa **Sync Now** (aparece un banner arriba del editor) o el ícono del elefante.

✅ **Debes ver** que el Sync termina otra vez sin errores.

💡 **Por qué en este archivo:** `local.properties` está en el `.gitignore`, así que si algún día subes el proyecto a GitHub, la key **no** se sube. El *Secrets Gradle Plugin* lee la key de ahí y la mete en el Manifest como `${MAPS_API_KEY}` al compilar. La key nunca queda escrita en el código.

---

### Paso 5. Crear el emulador (teléfono virtual)
🔧
1. Menú lateral derecho o **Tools → Device Manager**.
2. **Create Virtual Device** (el botón `+`).
3. Elige un teléfono, por ejemplo **Pixel 7** → **Next**.
4. En la lista de imágenes del sistema elige una de **API 34 o 35** que tenga el **ícono de Play Store** en la columna *Play Store*. Si dice "Download" junto al nombre, pulsa para descargarla. → **Next** → **Finish**.

✅ **Debes ver** el teléfono nuevo en la lista del Device Manager.

💡 **Por qué debe tener Play Store:** el mapa y el servicio de ubicación que usamos (*Fused Location Provider*) viven dentro de **Google Play Services**. Una imagen "pelada" de Android no los trae, y el mapa no funciona.

---

### Paso 6. Ejecutar la app
🔧
1. Arriba, en el selector de dispositivo, elige el emulador que acabas de crear.
2. Pulsa el botón verde **▶ Run**.
3. Espera: la primera vez el emulador tarda en arrancar y la app en compilar.

✅ **Debes ver** en el emulador la app "GPS y Maps" y un diálogo del sistema que pregunta por la ubicación.

💡 **Por qué aparece el diálogo:** desde Android 6, los permisos "peligrosos" como la ubicación no solo se declaran en el Manifest, además se **piden en pantalla** y el usuario decide. Eso lo hace el bloque "PASO 1: Permisos" de `MapScreen.kt`.

---

### Paso 7. Aceptar el permiso
🔧 En el diálogo elige **Precise** (precisa) y luego **While using the app** (mientras uso la app).

✅ **Debes ver** el mapa de Medellín, con la barra de búsqueda arriba y el panel con pestañas **Tracking** y **Reto** abajo.

💡 **Por qué "Precise":** pedimos dos permisos, `FINE` (exacta, GPS) y `COARSE` (aproximada). Si eliges aproximada, la app funciona pero con menos precisión.

> Si ves el mapa gris, salta a la **Parte C**.

---

### Paso 8. Decirle al emulador dónde está
🔧
1. En la barra lateral del emulador pulsa **⋯ (Extended controls)**.
2. Pestaña **Location** → pestaña **Single points**.
3. En los campos escribe **Latitude `6.2676`** y **Longitude `-75.5685`** (o busca "Universidad de Antioquia") → pulsa **Set location**.
4. De vuelta en la app, pulsa el botón de **mi ubicación** (el círculo con mira, arriba a la derecha del mapa).

✅ **Debes ver** el **punto azul** con un círculo de precisión, y el mapa centrado ahí.

💡 **Por qué hay que hacerlo:** el emulador no tiene GPS real. Por defecto cree que está en California. Si al abrir la app ves otro país, es normal: es la ubicación falsa que trae.

---

### Paso 9. Recorrido por las funciones (tu checklist)
Haz cada prueba y marca lo que ves.

| # | Prueba | Debes ver | Lo que enseña |
|---|---|---|---|
| 1 | Toca un punto cualquiera del mapa | Un marcador, y abajo "Último punto: [dirección]" | `onMapClick`, `Marker` y geocodificación inversa |
| 2 | Toca un segundo punto | Una **línea roja** entre los dos y "Punto 1 → Punto 2: X m" | `Polyline` y `Location.distanceBetween` |
| 3 | En el buscador escribe `6.2712, -75.5648` y pulsa **Ir** | Un marcador y la cámara vuela ahí (usa **punto** como decimal) | Parseo de coordenadas |
| 4 | Escribe `Parque Explora Medellín` y pulsa **Ir** | Un marcador en ese lugar | `Geocoder` (dirección → coordenadas) |
| 5 | Pulsa **Mapa** | El mapa cambia a satélite y vuelve | `MapType` |
| 6 | Pulsa **Limpiar** | Desaparecen marcadores y ruta | Manejo de estado |

> Si la prueba 4 falla con "No se encontró", revisa que el emulador tenga internet y Google Play.

💡 **Por qué todo se redibuja solo:** la lista de marcadores es un *estado* de Compose. Cuando cambia, la pantalla se recompone sola; nadie le dice "dibuja un marcador nuevo".

---

### Paso 10. Probar el tracking con una ruta GPX
Un **GPX** es un archivo con una lista de coordenadas en orden. El emulador lo "camina" como si fuera un GPS en movimiento. Te incluí uno: `ruta_prueba_medellin.gpx` (de la Universidad de Antioquia hacia el Parque Explora, unos 570 m).

🔧
1. En el emulador: **⋯ → Location → pestaña Routes**.
2. Pulsa **Import GPX/KML** y elige `ruta_prueba_medellin.gpx`.
3. Sube la velocidad de reproducción (por ejemplo **5x**) para no esperar 5 minutos.
4. En la app: pestaña **Tracking** → **Iniciar**.
5. Vuelve al emulador y pulsa **Play route**.

✅ **Debes ver** el punto azul avanzando, una **línea azul gruesa** dibujándose detrás, y en el panel la distancia aumentando, la velocidad y la precisión.

💡 **Por qué funciona:** al pulsar **Iniciar**, el `ViewModel` pide al repositorio un *Flow* de ubicaciones. Cada nueva posición se agrega a la ruta, se suma la distancia y la pantalla se actualiza. Al pulsar **Detener**, se cancela el flujo y se apaga el GPS (ahorra batería).

---

### Paso 11. Jugar el Reto de coordenadas
🔧 Pulsa la pestaña **Reto**.

1. Verás "Reto 1/5" con unas coordenadas. Intenta adivinar el lugar.
2. **Nombre** revela el nombre.
3. **Ver** pone un **marcador verde** y vuela hasta el lugar.
4. **Siguiente** pasa al siguiente reto y limpia el mapa.

**Variante de distancia:** pulsa **Siguiente**, aleja el mapa, toca donde crees que está el lugar y después pulsa **Ver**. Aparece una línea verde y los metros de diferencia.

✅ **Debes ver** el marcador verde en el lugar correcto y la distancia entre tu marcador y el objetivo.

💡 **Por qué no hay ubicación real aquí:** el reto trabaja con coordenadas fijas (`ChallengePlaces.kt`), así que funciona igual en un salón sin GPS.

---

## PARTE C — Si algo falla

### El mapa sale gris (el problema más común)
Casi siempre es la key. Revisa en este orden:

| # | Revisión | Cómo |
|---|---|---|
| 1 | ¿Pegaste la key? | `local.properties` debe decir `MAPS_API_KEY=` + la key real, y haber hecho Sync |
| 2 | ¿Mensaje de Google en Logcat? | Pestaña **Logcat** → filtro `Google Maps` → busca "Authorization failure" o "API key" |
| 3 | ¿Tu huella SHA-1 está registrada? | Ver abajo |
| 4 | ¿El dueño habilitó "Maps SDK for Android" y la facturación? | Debe revisarlo en Google Cloud Console |

**Cómo sacar tu SHA-1:**
1. Pestaña **Terminal** (abajo en Android Studio).
2. Mac/Linux: `./gradlew signingReport`. Windows: `gradlew.bat signingReport`.
3. Busca la variante **debug** y copia la línea **SHA1**.
4. Se la pasas al dueño de la key. Él la agrega en Google Cloud Console → Credenciales → la key → *Restricciones de aplicaciones* → Android: paquete `com.example.gpsmaps` + tu SHA-1.
5. Espera unos minutos y vuelve a ejecutar.

💡 **Por qué:** la key está "atada" a `nombre de paquete + huella de firma`. Si alguien copia la key, no le sirve en otra app. Cada computador genera su propia huella de debug, así que cada compañero debe registrar la suya.

### Otros errores
| Síntoma | Qué hacer |
|---|---|
| `Manifest merger failed ... MAPS_API_KEY` | Falta `local.defaults.properties` en la raíz, o no hiciste Sync. |
| `Unsupported Java` / `Gradle JDK` | Paso 3: elige el JDK incluido (jbr-17 o jbr-21). |
| `SDK location not found` | Abre `local.properties` y agrega `sdk.dir=` con la ruta del Android SDK (Android Studio normalmente la agrega solo al abrir el proyecto). |
| `Could not resolve ...` o `Connection timed out` | Sin internet o proxy del colegio/universidad. Prueba con datos del celular. |
| Android Studio dice que la versión de AGP no es compatible | Prueba "Upgrade Assistant"; si el código se rompe, usa la guía `COMPILAR_PASO_A_PASO.md` (proyecto nuevo). |
| La app se cierra al abrir | Pestaña **Logcat**, filtro **Error**: la primera línea roja dice la causa. |
| No hay punto azul | Permiso negado o ubicación sin fijar (Paso 8). |
| Estás en otro país | Es la ubicación por defecto del emulador (Paso 8). |
| `Geocoder` no responde | Emulador sin Play Store o sin internet. |

---

## PARTE D — Cuando termine la clase
- El dueño de la key debe **eliminarla o rotarla** en Google Cloud Console (Credenciales).
- Borra tu `local.properties` o la línea de la key.
- Si usaste Git, confirma que `local.properties` nunca se subió.
