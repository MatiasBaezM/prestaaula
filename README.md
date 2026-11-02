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
| Inventario (ver y agregar equipos) | | ✔ | | |
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
| Almacenamiento local | JSON en el almacenamiento privado de la app (`data/Persistencia.kt`) |
| Recursos nativos (2) | **Cámara** (foto del equipo al devolverlo, `ui/components/Fotos.kt`) y **vibración** (confirmaciones y errores, `ui/components/Vibracion.kt`) |
| Animaciones | Transiciones entre pantallas, sacudida del campo inválido, mensajes de error animados, cambio de color de etiquetas, movimiento de ítems en listas |

### Recursos nativos

- **Cámara:** en *Registrar devolución* el botón **Tomar foto** abre la cámara del sistema. La foto queda asociada al préstamo y se ve en *Equipos con incidencias*. Se usa la app de cámara del sistema, por lo que no requiere el permiso `CAMERA`; el archivo se comparte mediante un `FileProvider` limitado a la carpeta `fotos/` de la app.
- **Vibración:** un pulso corto confirma una acción (registrar préstamo, devolución, reserva, retiro) y dos pulsos avisan de un error. Usa el permiso normal `VIBRATE`.

## Arquitectura

MVVM con repositorios compartidos como única fuente de verdad.

```
app/src/main/java/appmovil/prestaaula/
├── model/        Dominio puro: Equipo, Reserva, Prestamo, Rol, Bloques, Validaciones…
├── data/         Repositorios (inventario, reservas, préstamos, autenticación) y persistencia JSON
├── ui/
│   ├── theme/        Tema Material 3
│   ├── components/   CampoValidado, Etiqueta, Fotos, Vibracion…
│   ├── navigation/   AppNavHost
│   ├── login/ home/ inventario/ disponibilidad/ reservas/ prestamos/ reportes/
│   └── (cada carpeta: Screen + ViewModel)
├── PrestaAulaApp.kt  Crea los repositorios y guarda los cambios en disco
└── MainActivity.kt
```

- Los `ViewModel` no conocen Android: reciben repositorios por constructor, lo que permite probarlos en JVM.
- El login depende de la interfaz `Autenticador`, de modo que la validación local de demostración se puede reemplazar por una API REST sin tocar la UI.
- `PrestaAulaApp` guarda inventario, reservas y préstamos **juntos** ante cualquier cambio, para que en disco nunca queden versiones inconsistentes.

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
gradlew :app:testDebugUnitTest            # unitarias (JVM)
gradlew :app:connectedDebugAndroidTest    # de interfaz, con emulador abierto
```

Cubren validaciones, repositorios, reglas de reserva y préstamo, persistencia, ViewModels y el flujo de cada rol.

## Seguridad

- `allowBackup="false"` y reglas de exclusión: los datos de préstamos no salen del dispositivo por backup.
- Build *release* con optimización de R8 activada.
- Las fotos viven en almacenamiento privado y el `FileProvider` no es exportado.
- Pendiente para producción: autenticación contra servidor y firma del APK con clave propia.

## Tecnologías

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel / StateFlow · `org.json` para persistencia · Gradle (Kotlin DSL).
