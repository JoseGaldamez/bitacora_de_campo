# Bitácora de Campo (Consumo de API REST - Tarea 3)

Aplicación móvil desarrollada para Android en Java para registrar, organizar y consultar observaciones e información de campo conectándose a una API REST en **.NET 10** mediante **Retrofit**, implementando arquitectura **MVVM** y **Patrón Repositorio**.

---

## Integrantes del Grupo #3 — Programación Móvil 1

| Nombre Completo | Número de Cuenta |
| :--- | :--- |
| **Melvin Edgardo Peña Caballero** | 201230010100 |
| **Christian Yahir Rodriguez Sanchez** | 202310030212 |
| **Linda Guadalupe Nolasco Martínez** | 202110080018 |
| **William Eduardo Banegas Cruz** | 201410010285 |
| **José Oveniel Galdámez Ramos** | 201210011668 |
| **Ricardo Andres Montes Jovel** | 201830010183 |

---

## Cómo trabajan en conjunto ambos proyectos

El sistema está compuesto por dos proyectos independientes que se comunican mediante peticiones HTTP/JSON:

1. **Backend — API REST en .NET 10 (`Tarea2.2_ApiRest_BitacoraUTH`):**
   - **Repositorio:** [https://github.com/williambanegas/Tarea2.2_ApiRest_BitacoraUTH](https://github.com/williambanegas/Tarea2.2_ApiRest_BitacoraUTH)
   - Construido bajo **Clean Architecture** (`Bitacora.Domain`, `Bitacora.Application`, `Bitacora.Infrastructure`, `Bitacora.Api`).
   - Utiliza **Entity Framework Core** con una base de datos local **SQLite** (`bitacora.db`), por lo que no requiere instalar un motor de base de datos externo.
   - Expone los endpoints REST en `http://0.0.0.0:5033/api/registros` y cuenta con documentación interactiva **Swagger UI** en `http://localhost:5033/swagger`.
   - Valida que el título tenga entre 3 y 100 caracteres, la descripción hasta 500 caracteres y que la fotografía en Base64 sea válida y no supere los **2 MB**.

2. **Frontend Móvil — App Android en Java (`bitacora_de_campo`):**
   - Se conecta al servidor .NET a través de la IP especial del emulador Android **`http://10.0.2.2:5033/`** (que redirige al puerto `5033` de la computadora anfitriona).
   - **Captura y envío (`POST /api/registros`):** Toma la fotografía con la cámara, la redimensiona a un máximo de `1024px`, la comprime en JPEG al `70%` y la codifica en **Base64** (`fotoBase64`) junto con el título y la descripción (escrita o dictada por voz).
   - **Consulta en lista y detalle (`GET /api/registros` y `GET /api/registros/{id}`):** Obtiene todos los registros desde el servidor, decodifica las imágenes Base64 a `Bitmap` para mostrarlas tanto en las tarjetas del listado principal (`RecyclerView`) como en la vista de detalle (`DetailRecordActivity`), y permite eliminarlos (`DELETE /api/registros/{id}`).

### Tabla de Endpoints Consumidos

| Método HTTP | Ruta en el API (.NET) | Método en `RegistroApi` (Android) | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/registros` | `getRecords()` | Obtiene la lista de registros con su estado e imagen para el listado principal. |
| `GET` | `/api/registros/{id}` | `getRecordById(id)` | Obtiene el detalle completo de un registro específico y su fotografía en Base64. |
| `POST` | `/api/registros` | `createRecord(record)` | Crea un nuevo registro en la base de datos SQLite del servidor. |
| `PUT` | `/api/registros/{id}` | `updateRecord(id, record)` | Actualiza el título, descripción y fotografía de un registro existente. |
| `DELETE` | `/api/registros/{id}` | `deleteRecord(id)` | Elimina un registro de la base de datos por su ID. |

---

## Características y Requisitos Implementados en Android

- **Arquitectura MVVM & Patrón Repositorio:**
  - `views`: Activities (`MainActivity`, `AddRecordActivity`, `DetailRecordActivity`) y adaptador `RecordAdapter`. Únicamente muestran datos y reciben eventos de usuario.
  - `viewmodel`: `RegistroViewModel` extiende `AndroidViewModel` y expone los estados de pantalla mediante `LiveData`.
  - `data/remote`: Interfaz `RegistroApi` de Retrofit, configuración de `RetrofitClient` (`OkHttpClient`) y modelo `Record`.
  - `data/repository`: `RegistroRepository`, responsable único de interactuar con la API REST y traducir códigos de error HTTP.
- **Cliente HTTP (Retrofit & OkHttp):**
  - Conversión JSON automática con `Gson` (`@SerializedName` sincronizado con los DTOs de .NET: `fotoBase64`, `tieneFoto`, `fechaCreacion`).
  - `OkHttpClient` configurado con `connectTimeout` y `readTimeout` de 15 segundos e interceptor de logs (`HttpLoggingInterceptor`).
  - Configuración de seguridad de red (`network_security_config.xml`) para permitir tráfico HTTP en texto plano hacia `10.0.2.2` y `localhost` durante el desarrollo.
- **Funciones Multimedia y de Captura:**
  - **Cámara:** Captura de fotografía integrada con redimensionamiento automático a un máximo de 1024px y compresión JPEG al 70% antes de convertir a Base64.
  - **Voz a Texto:** Dictado por voz mediante `RecognizerIntent` para rellenar la descripción del registro.
- **Manejo de Estados de Pantalla:**
  - **Cargando:** `ProgressBar` visible y controles deshabilitados durante peticiones de red.
  - **Lista Vacía:** Vista dedicada cuando no existen registros en el servidor.
  - **Error del Servidor / Sin Conexión / Timeout:** Captura de errores con vista de estado y botón **Reintentar**.

---

## Guía Paso a Paso para Ejecutar Ambos Proyectos

Para que la aplicación Android funcione correctamente, **primero debes iniciar la API REST en .NET** y luego ejecutar la app en Android Studio.

### Paso 1: Ejecutar la API REST en .NET (`Tarea2.2_ApiRest_BitacoraUTH`)

1. Clona o descarga el repositorio del backend desde:
   ```powershell
   git clone https://github.com/williambanegas/Tarea2.2_ApiRest_BitacoraUTH
   ```
2. Asegúrate de tener instalado el **SDK de .NET 10** (o Visual Studio 2022 / 2026 con soporte para .NET 10).
3. Abre una terminal en la carpeta `Tarea2.2_ApiRest_BitacoraUTH` o abre la solución `Bitacora.slnx` en Visual Studio.
4. Ejecuta el proyecto con el perfil `http` (que escucha en el puerto `5033`):
   ```powershell
   dotnet run --project src/Bitacora.Api/Bitacora.Api.csproj --launch-profile http
   ```
   *(O en Visual Studio, selecciona el perfil **http** en la barra superior y presiona `F5` / `Ctrl + F5`).*
5. Verifica en el navegador de tu PC que la API esté respondiendo entrando a Swagger UI:
   - `http://localhost:5033/swagger`

### Paso 2: Ejecutar la App Android (`bitacora_de_campo`)

1. Abre **Android Studio** y selecciona **File > Open**, eligiendo la carpeta `bitacora_de_campo`.
2. Espera a que **Gradle Sync** termine de descargar e indexar las dependencias.
3. Inicia un **Emulador Android (AVD)** desde el *Device Manager*.
4. Haz clic en **Run 'app'** (`Shift + F10`).
5. La aplicación se conectará automáticamente a `http://10.0.2.2:5033/api/registros` y mostrará los registros almacenados en la base de datos SQLite de la API.

---

## Solución de Problemas: Qué hacer si Windows o el Emulador bloquean la conexión

Si la API abre bien en el navegador de tu PC (`http://localhost:5033/swagger`), pero en la app de Android aparece error de **Timeout (`SocketTimeoutException`)** al intentar conectar con `10.0.2.2:5033`, revisa estos puntos:

### 1. Desbloquear el puerto `5033` en el Firewall de Windows Defender
Cuando el servidor .NET se ejecuta en el puerto `5033`, el Firewall de Windows puede bloquear las peticiones entrantes desde el emulador:
- Si aparece la ventana de **Alerta de seguridad de Windows Defender**, marca tanto **Redes privadas** como **Redes públicas** y haz clic en **Permitir acceso**.
- Si no apareció la ventana o fue bloqueado previamente, abre **PowerShell como Administrador** y ejecuta este comando para permitir el tráfico en el puerto `5033`:
  ```powershell
  New-NetFirewallRule -DisplayName "Bitacora API Puerto 5033" -Direction Inbound -LocalPort 5033 -Protocol TCP -Action Allow
  ```

### 2. Restricción de Red Local en Android API 37 (`Local Network Protection`)
Si se compila la app con `targetSdk = 37` (Android 17 Preview), el sistema operativo Android activa la política `Compat change id: 333566037` (*Local Network Protection*), la cual **bloquea el acceso de las aplicaciones a direcciones IP de subred local como `10.0.2.2`** aunque el navegador del emulador sí pueda entrar.
- **Solución:** En `app/build.gradle.kts`, asegúrate de mantener **`targetSdk = 36`** (o `35`):
  ```kotlin
  defaultConfig {
      applicationId = "dev.josegaldamez.bitacora_de_campo"
      minSdk = 24
      targetSdk = 36 // Usar 35 o 36 para permitir tráfico hacia 10.0.2.2
  ```

### 3. Alternativa directa por cable virtual (`adb reverse`)
Si por políticas de red de tu equipo, VPN o antivirus el emulador sigue sin alcanzar `10.0.2.2:5033`, puedes crear un túnel directo por ADB entre el emulador (o un teléfono físico por USB) y tu PC:
1. Ejecuta en la terminal:
   ```powershell
   adb reverse tcp:5033 tcp:5033
   ```
2. En `RetrofitClient.java`, cambia `BASE_URL` a `"http://localhost:5033/"`.

---

## Tecnologías y Dependencias

- **Lenguaje:** Java 11 (Android SDK, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 37`)
- **Arquitectura:** MVVM + Repository Pattern
- **Red y Serialización:** Retrofit 2.11.0, Gson Converter, OkHttp 4.12.0 (`HttpLoggingInterceptor`)
- **Ciclo de Vida:** AndroidX Lifecycle (`ViewModel` & `LiveData`) 2.8.7
- **UI / Componentes:** `ConstraintLayout`, `RecyclerView`, `SwipeRefreshLayout`, Material Design 3

---

## Estructura del Proyecto Android

```text
bitacora_de_campo/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/dev/josegaldamez/bitacora_de_campo/
│           │   ├── controllers/    # RecordController (Utilidades Base64 / Bitmap)
│           │   ├── data/
│           │   │   ├── remote/     # RegistroApi, RetrofitClient
│           │   │   └── repository/ # RegistroRepository
│           │   ├── models/         # Record (Modelo de datos sincronizado con el API)
│           │   ├── viewmodel/      # RegistroViewModel
│           │   └── views/          # MainActivity, AddRecordActivity, DetailRecordActivity, RecordAdapter
│           └── res/
│               ├── layout/         # Diseños XML con estados de carga, error y vacíos
│               └── xml/            # network_security_config.xml
└── build.gradle.kts
```

---

## Licencia

Proyecto desarrollado con fines académicos para la clase de **Programación Móvil 1** — Universidad Tecnológica de Honduras (UTH).
