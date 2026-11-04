# 📱 Bitácora de Campo

Aplicación móvil desarrollada para Android diseñada para registrar, organizar y consultar observaciones e información de campo de manera eficiente.

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

## Características Principales

- **Gestión de Registros de Campo:** Creación, lectura, actualización y eliminación (CRUD) de notas de campo.
- **Multimedia y Fotografías:** Soporte para adjuntar y visualizar fotografías convertidas en formato Base64.
- **Almacenamiento Local Robusto:** Base de datos SQLite integrada mediante `SQLiteOpenHelper` para garantizar persistencia sin necesidad de conexión a internet.
- **Interfaz Moderna y Fluida:** Implementación con `RecyclerView`, `CardView` y componentes de Material Design.
- **Control de Permisos:** Gestión de permisos de cámara y audio para futuras expansiones multimedia.

---

## Tecnologías y Arquitectura

- **Lenguaje:** Java (Android SDK)
- **Patrón de Arquitectura:** MVC (Modelo - Vista - Controlador)
- **Base de Datos:** SQLite (`SQLiteOpenHelper`)
- **UI / Componentes:** 
  - `ConstraintLayout`
  - `RecyclerView` & Adaptadores personalizados (`RecordAdapter`)
  - Material Design Components (`com.google.android.material`)
- **Entorno de Desarrollo:** Android Studio / Android Gradle Plugin (AGP)

---

## Estructura del Proyecto

```text
bitacora_de_campo/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/dev/josegaldamez/bitacora_de_campo/
│           │   ├── controllers/   # Controladores de negocio (RecordController)
│           │   ├── models/        # Modelos de datos (Record)
│           │   ├── services/      # Servicios de base de datos (DatabaseService)
│           │   └── views/         # Actividades y adaptadores (MainActivity, AddRecordActivity, DetailRecordActivity, RecordAdapter)
│           └── res/
│               ├── layout/        # Diseños XML de las interfaces
│               ├── drawable/      # Recursos gráficos y placeholders
│               └── values/        # Colores, temas y cadenas de texto
└── build.gradle.kts
```

---

## Requisitos del Sistema

- **Android SDK:** Mínimo API 24 (Android 7.0 Nougat) o superior.
- **Target SDK:** API 37.
- **Java Development Kit (JDK):** Versión 11 o superior.
- **Gradle:** 8.x / Gradle Wrapper incluido.

---

## Instrucciones de Configuración y Ejecución

1. **Clonar o descargar el repositorio** en tu equipo local.
2. **Abrir Android Studio** (versión recomendada: Koala / Jellyfish o superior).
3. Seleccionar **File > Open** y elegir la carpeta raíz del proyecto (`bitacora_de_campo`).
4. Esperar a que **Gradle Sync** finalice la descarga de dependencias e indexación.
5. Conectar un dispositivo físico con depuración USB habilitada o iniciar un **Emulador Android** (AVD) con API 24+.
6. Hacer clic en el botón **Run 'app'** (o presionar `Shift + F10`) para compilar e instalar la aplicación.

---

## Licencia

Este proyecto ha sido desarrollado con fines académicos para la clase de **Programación Móvil 1**.
