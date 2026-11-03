# PrestaAula — Colegio Marcela Paz

App Android para gestionar el **préstamo de recursos tecnológicos del CRA** (notebooks, tablets, proyectores y cables HDMI): inventario, disponibilidad, reservas, entrega y devolución de equipos, y reportes para la dirección.

> Evaluación Parcial 2 — Desarrollo de Aplicaciones Móviles (DSY1105). Caso: *PrestaAula*.

## Integrantes

| Nombre | Rol en el proyecto |
|---|---|
| Mati *(completar nombre completo)* | Desarrollo completo (trabajo individual) |

- Número de equipo: *(completar)*
- Sección: *(completar)*

## Problema que resuelve

En el colegio, los equipos del CRA se prestaban de forma manual: no se sabía qué equipo estaba libre, se cruzaban las reservas y no quedaba registro de quién devolvió un equipo dañado. PrestaAula centraliza el inventario, evita reservas duplicadas por bloque horario y deja un historial con responsable por cada préstamo.

## Funcionalidades

La app muestra **solo las funciones del rol** con que se inicia sesión.

| Función | Docente | Encargado CRA | Jefe UTP | Jefe Adm. y Finanzas |
|---|:-:|:-:|:-:|:-:|
| Inventario (ver, agregar equipos con todos sus campos y marcar un equipo como dañado) | | ✔ | | |
| Disponibilidad por día y bloque (consulta) | ✔ | ✔ | | |
| Reservar un equipo libre, desde Disponibilidad (uno o varios bloques seguidos) | ✔ | | | |
| Mis reservas (ver y cancelar) | ✔ | | | |
| Retiro y devolución (confirmar retiro, avisar devolución) | ✔ | | | |
| Registrar préstamo | | ✔ | | |
| Registrar devolución (con foto) | | ✔ | | |
| Reporte de uso por período | | ✔ | ✔ | ✔ |
| Equipos con incidencias | | ✔ (puede marcar reparado) | ✔ (consulta) | ✔ (consulta) |

Reglas de negocio principales:

- Un equipo no puede reservarse dos veces en el mismo bloque, ni reservarse si está prestado o en mantención.
- Una reserva ocupa bloques **seguidos** (clase doble). Jornada de 9 bloques de 45 min desde las 08:00, lunes a viernes.
- Un equipo se agrega con tipo, marca, número de identificación interno (sugerido, editable, sin repetir) y estado inicial (Disponible o En mantención).
- El CRA puede marcar como dañado un equipo disponible: pasa a **En mantención** y no se puede reservar ni prestar hasta repararlo. Si tenía reservas pendientes, se le avisa.
- Si el equipo vuelve con daños o faltantes, las observaciones son obligatorias y el equipo pasa a **En mantención** hasta que el CRA lo marque como reparado.
- Cada préstamo guarda al docente responsable; los reportes muestran el último responsable de cada equipo.

## Requisitos de la evaluación

| Requisito | Dónde está |
|---|---|
| Diseño visual con Material 3 | `ui/theme`, componentes `material3` en todas las pantallas |
| Formularios validados (ícono + mensaje por campo) | `ui/components/CampoValidado.kt`; formularios de login, reservar, entregar, agregar equipo y recibir devolución |
| Validaciones desacopladas de la UI | `model/Validaciones.kt` (lógica pura); usada por los `ViewModel` |
| Navegación funcional | `ui/navigation/AppNavHost.kt` (Navigation Compose, con guardia de acceso por rol) |
| Gestión de estado | `ViewModel` + `StateFlow` en cada pantalla |
| Almacenamiento local | SQLite con Room: tablas en `data/db/Entidades.kt`, DAOs `EquipoDao`, `ReservaDao` y `PrestamoDao`, base `data/db/PrestaAulaDb.kt` |
| Recursos nativos (2) | **Cámara** (foto del equipo al devolverlo, `ui/components/Fotos.kt`) y **vibración** (confirmaciones y errores, `ui/components/Vibracion.kt`) |
| Animaciones | Transiciones entre pantallas, sacudida del campo inválido, mensajes de error animados, cambio de color de etiquetas, movimiento de ítems en listas |

### Recursos nativos

- **Cámara:** en *Registrar devolución* el botón **Tomar foto** abre la cámara del sistema. La foto queda asociada al préstamo y se ve en *Equipos con incidencias*. Se usa la app de cámara del sistema, por lo que no requiere el permiso `CAMERA`; el archivo se comparte mediante un `FileProvider` limitado a la carpeta `fotos/` de la app.
- **Vibración:** un pulso corto confirma una acción (registrar préstamo, devolución, reserva, retiro) y dos pulsos avisan de un error. Usa el permiso normal `VIBRATE`.

## Arquitectura

MVVM: la base de datos SQLite (Room) es la única fuente de verdad y los datos fluyen en una sola dirección.

```
Screen (Compose) ──evento──▶ ViewModel ──llama──▶ Repository ──▶ DAO ──▶ SQLite (Room)
       ▲                         │                                          │
       └──── StateFlow ◀── stateIn(viewModelScope) ◀──── Flow ◀─────────────┘
```

```
app/src/main/java/appmovil/prestaaula/
├── model/        Model: dominio y reglas puras (Equipo, Reserva, Prestamo, Rol, Bloques, Validaciones…)
├── data/
│   ├── db/           Room: entidades (tablas), DAOs, convertidores de fecha y PrestaAulaDb
│   ├── InventarioRepository, ReservaRepository, PrestamoRepository   Repository
│   ├── AuthRepository, Docentes    Login y nómina de demostración
│   └── DatosDemo     Datos de ejemplo que se guardan la primera vez
├── ui/
│   ├── theme/        Tema Material 3
│   ├── components/   CampoValidado, Etiqueta, Fotos, Vibracion…
│   ├── navigation/   AppNavHost
│   ├── Operaciones.kt  ejecutar(): lanza una operación de la base en viewModelScope
│   ├── login/ home/ inventario/ disponibilidad/ reservas/ prestamos/ reportes/
│   └── (cada carpeta: Screen + ViewModel)
├── PrestaAulaApp.kt  Crea la base de datos y los repositorios (inyección manual de dependencias)
└── MainActivity.kt
```

Responsabilidades:

- **Model** (`model/`): datos del dominio y reglas puras (validaciones, ocupación de bloques, reportes). No depende de Android ni de Room.
- **DAO** (`data/db/*Dao.kt`): consultas SQL. `@Insert` inserta, `@Update` actualiza y `@Query` recupera o borra. Las lecturas que muestra la UI devuelven `Flow`, que vuelve a emitir cada vez que la tabla cambia.
- **Repository** (`data/*Repository.kt`): única puerta de los ViewModels a los datos. Traduce filas de Room al modelo y aplica las reglas de negocio. Cada operación (reservar, prestar, devolver…) valida y guarda en **una sola transacción** (`withTransaction`): si algo falla, SQLite revierte todo.
- **ViewModel** (`ui/*/…ViewModel.kt`): convierte los `Flow` del repositorio en `StateFlow` (`stateIn`) y lanza las acciones con `viewModelScope.launch`, para que una operación no se corte al rotar la pantalla. Avisa el resultado (`null` = guardado, o el mensaje de error).
- **Screen** (`ui/*/…Screen.kt`): observa el estado con `collectAsStateWithLifecycle()` y envía los eventos del usuario al ViewModel. No contiene lógica de negocio.

Además:

- La base aplica integridad por su cuenta: claves foráneas (una reserva o préstamo debe apuntar a un equipo existente) y código de equipo único.
- El login depende de la interfaz `Autenticador`, de modo que la validación local de demostración se puede reemplazar por una API REST sin tocar la UI.

### Persistencia: cómo probarla

1. Iniciar sesión como Encargado CRA, abrir **Inventario** y marcar un equipo como dañado (o agregar uno).
2. Cerrar la app por completo (quitarla de recientes) y volver a abrirla.
3. El cambio sigue ahí: se leyó de la base SQLite `prestaaula.db`.

## Cómo ejecutar

Requisitos: Android Studio (incluye el JDK) y un emulador o teléfono con **Android 8.0 (API 26) o superior**. No necesita internet ni servidor.

1. Abrir la carpeta del proyecto en Android Studio (*File > Open*).
2. Esperar la sincronización de Gradle.
3. Elegir un emulador o dispositivo y presionar **Run ▶**.

Desde terminal (con `JAVA_HOME` apuntando al JDK de Android Studio):

```
gradlew :app:installDebug
```

Para volver a los datos de ejemplo: `adb shell pm clear appmovil.prestaaula`.

## Usuarios de demostración

Cuentas **solo para pruebas**, incluidas en el código (`data/AuthRepository.kt`):

| Rol | Correo | Contraseña |
|---|---|---|
| Docente | `docente@marcelapaz.cl` | `docente123` |
| Encargado CRA | `cra@marcelapaz.cl` | `cra123` |
| Jefe de UTP | `utp@marcelapaz.cl` | `utp123` |
| Jefe de Administración y Finanzas | `finanzas@marcelapaz.cl` | `admin123` |

El correo no distingue mayúsculas ni espacios en los extremos; la contraseña sí distingue mayúsculas.

> **Aviso de seguridad:** estas credenciales están en el código y cualquiera que descompile la app puede verlas. Antes de usar la app con datos reales hay que validar el ingreso contra un servidor (con contraseñas almacenadas con hash) y no publicar credenciales reales.

## Pruebas

```
gradlew :app:testDebugUnitTest            # unitarias (JVM, con Room en memoria vía Robolectric)
gradlew :app:connectedDebugAndroidTest    # DAOs en SQLite real e interfaz, con emulador abierto
```

Cubren validaciones, DAOs, repositorios contra la base real, reglas de reserva y préstamo, transacciones, ViewModels y el flujo de cada rol.

## Seguridad

- `allowBackup="false"` y reglas de exclusión: los datos de préstamos no salen del dispositivo por backup.
- Build *release* con optimización de R8 activada.
- Las fotos viven en almacenamiento privado y el `FileProvider` no es exportado.
- Pendiente para producción: autenticación contra servidor y firma del APK con clave propia.

## Tecnologías

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel / StateFlow · Corrutinas · Room (SQLite) · Gradle (Kotlin DSL).
