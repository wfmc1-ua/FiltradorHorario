# CLAUDE.md

Instrucciones de proyecto para Claude Code. Léelas enteras antes de tocar código.

---

## 1. Qué es este proyecto

App **Android nativa** que:

1. Recibe un PDF de cuadrante semanal de turnos (compartido desde WhatsApp/Drive o abierto desde el explorador).
2. Reconstruye la rejilla del PDF y extrae el horario de **un empleado concreto**.
3. Muestra una pantalla de revisión editable con los 7 días.
4. Programa las alarmas del teléfono para esa semana.

**Solo Android.** No hay target iOS ni Kotlin Multiplatform. No propongas Compose Multiplatform ni módulos `shared`.

---

## 2. Restricciones no negociables

| Regla | Motivo |
|---|---|
| **Nunca programar alarmas sin confirmación explícita del usuario** en la pantalla de revisión | Un parser de PDF de hoja de cálculo siempre falla algún caso; una alarma mal puesta un lunes a las 7:00 es un fallo caro |
| Librería PDF: **`com.tom-roush:pdfbox-android`** (Apache 2.0) | iText es AGPL. No lo uses ni lo sugieras |
| El parser **debe usar coordenadas X/Y**, nunca texto plano por líneas | Las celdas vacías desaparecen en el texto plano y las horas se desalinean de día |
| El matching de empleado es **por nombre normalizado**, nunca por ID | Los IDs del cuadrante son heterogéneos (DNI, NIE) y hay filas sin ID |
| Toda la lógica de rejilla es **Kotlin puro, sin imports de Android** | Ver §4 |

---

## 3. Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **minSdk 26**, targetSdk 35
- **PDFBox-Android** (`com.tom-roush:pdfbox-android`)
- `java.time` (desugaring activado)
- **Sin framework de DI.** Un `AppContainer` manual instanciado en `Application` y pasado a los ViewModels vía factory
- **Room solo si hace falta** persistir entre sesiones (ver fase 3). Hasta entonces, estado en memoria
- Tests: **JUnit4 + Truth**. Robolectric solo donde sea inevitable

---

## 4. Arquitectura: mantenla proporcionada

Este proyecto es pequeño. **No** es Clean Architecture, no hay capas `domain/data/ui`, no hay `Repository` + `RepositoryImpl`, y no hay use cases que solo reenvían una llamada al parser. El ViewModel llama directamente a las clases que necesita.

```
com.wesley.cuadrantes/
├── model/     Cuadrante, EmployeeSchedule, DaySchedule, Shift, AlarmPlan   (puro)
├── parser/    PdfTextExtractor, RowClusterer, HeaderDetector, GridBuilder,
│              EmployeeRowMapper, NameMatcher
├── alarm/     AlarmScheduler (interfaz), IntentAlarmScheduler, AlarmPlanner
├── data/      Room — solo cuando llegue la fase 3
└── ui/        import/, review/, settings/
```

### La única regla de abstracción

> Pon una interfaz **solo** donde ya sabes que habrá dos implementaciones, o donde el test no puede pasar sin ella.

En este proyecto eso son exactamente tres sitios. Cualquier interfaz fuera de esta lista hay que justificarla antes de escribirla:

| Abstracción | Por qué existe |
|---|---|
| `AlarmScheduler` | Habrá `IntentAlarmScheduler` (v1), `AlarmManagerScheduler` (v2) y `FakeAlarmScheduler` (tests) |
| `PdfTextExtractor` | Aísla PDFBox/Android para que el resto del parser se teste con JUnit puro en milisegundos |
| `Clock` | Para poder testear "no programes alarmas en el pasado" sin depender de la hora real |

### La frontera que sí importa

```kotlin
// única clase que toca Android/PDFBox — ~20 líneas, se testea con un test de instrumentación
interface PdfTextExtractor { fun extract(input: InputStream): List<PositionedText> }

// todo lo demás: Kotlin puro, JUnit a pelo
class GridBuilder { fun build(cells: List<PositionedText>): Grid }
```

Si un test del parser necesita Robolectric o un emulador, la frontera está mal puesta.

---

## 5. Modelo

```kotlin
data class Cuadrante(
    val weekStart: LocalDate,
    val center: String?,          // "PLAZA MAR"
    val employees: List<EmployeeSchedule>,
)

data class EmployeeSchedule(
    val rawId: String?,           // DNI/NIE tal cual, puede ser null
    val name: String,
    val contractHours: Int?,
    val days: List<DaySchedule>,  // siempre 7, lunes→domingo
)

data class DaySchedule(
    val date: LocalDate,
    val status: DayStatus,
    val shifts: List<Shift>,      // 0..2 — los turnos partidos son reales
)

enum class DayStatus { WORK, FREE, VACATION, UNKNOWN }

data class Shift(val start: LocalTime, val end: LocalTime)

data class AlarmPlan(
    val date: LocalDate,
    val time: LocalTime,
    val label: String,            // "Turno 10:00–15:00"
    val enabled: Boolean = true,
)
```

`shifts` es una **lista**, no un campo único. Es el error más fácil de cometer aquí.

---

## 6. Especificación del parser

### Pipeline

1. **`PdfTextExtractor`** — extiende `PDFTextStripper`, sobrescribe `writeString(text, positions)` y emite `PositionedText(text, x = xDirAdj, y = yDirAdj)`.
2. **`RowClusterer`** — agrupa por Y con tolerancia configurable (empieza en ±3 pt). Cada cluster es una fila visual.
3. **`HeaderDetector`** — localiza la fila con las fechas (`27-jul`, `28-jul`…) y la fila `Turno A / Turno B`. Devuelve los **centros X de las 14 columnas** (7 días × 2 turnos) y la fecha de cada una.
4. **`GridBuilder`** — asigna cada celda a la columna con el centro X más cercano. Resultado: `Grid`, un `Map<RowIndex, Map<ColumnIndex, String>>`.
5. **`EmployeeRowMapper`** — convierte una fila en `EmployeeSchedule` aplicando las reglas de abajo.
6. **`NameMatcher`** — normaliza (uppercase, `Normalizer.NFD` + strip de diacríticos, colapso de espacios) y hace match por subconjunto de tokens, para que "Wesley Murillo" encuentre "WESLEY FABIAN MURILLO CASTRO". Si hay varias coincidencias, la UI pide desambiguar. **Nunca elijas por el usuario.**

### Reglas de negocio del cuadrante

| Contenido de celda | Interpretación |
|---|---|
| `L` | `DayStatus.FREE` — sin alarma |
| `V` | `DayStatus.VACATION` — sin alarma (suele ocupar la fila entera) |
| `HH:MM HH:MM` | Un `Shift`. Si aparece en turno A y turno B del mismo día → **turno partido**, dos `Shift` |
| Celda vacía | Sin turno. **No** heredar el valor de la columna anterior |
| Fin < inicio | Turno que cruza medianoche. Marca y avisa en la UI; no lo asumas en silencio |

### Filas que hay que descartar

- Filas numeradas sin nombre (`21 0,0 0,0`)
- `INVENTARIO`
- Separadores de sección tipo `VAN A GRAN VIA` y las filas de personal desplazado que van debajo (nombre corto, sin ID, sin horas contratadas) → **fuera de alcance en v1**
- Filas de facturación (importes en €)
- Cabeceras y la fila de totales

### Fixture obligatorio

Coloca el PDF real en `app/src/test/resources/fixtures/cuadrante_plaza_mar_sem31.pdf` y escribe un **golden test** que verifique el horario esperado de al menos tres empleados con formas distintas: uno con turno partido, uno de vacaciones (`V`) y uno con días libres intercalados. Ese test es el contrato del parser: con él en verde, cualquier refactor es seguro.

---

## 7. Alarmas

### v1 — `AlarmClock.ACTION_SET_ALARM`

```kotlin
Intent(AlarmClock.ACTION_SET_ALARM).apply {
    putExtra(AlarmClock.EXTRA_HOUR, plan.time.hour)
    putExtra(AlarmClock.EXTRA_MINUTES, plan.time.minute)
    putExtra(AlarmClock.EXTRA_MESSAGE, plan.label)
    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
}
```

Manifest: `<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />`

**Limitaciones que hay que enseñar en la propia UI, no esconder:**

- **No se pueden borrar** las alarmas creadas así. Si el usuario reimporta la semana, se duplican. Avisa antes de reprogramar y registra qué alarmas ya se crearon.
- **No uses `EXTRA_DAYS`**: crea alarmas semanales recurrentes y el cuadrante rota cada semana. Queremos alarmas de un solo disparo.
- El comportamiento de `EXTRA_SKIP_UI` depende de la app Reloj del fabricante. Ten un fallback que abra la UI si el intent no se resuelve.

### v2 — `AlarmManager` propio

Cuando v1 esté cerrada: `AlarmManager.setAlarmClock()` + `full-screen intent` + `ForegroundService`, con nuestra propia pantalla de alarma. Permite borrar, editar y snooze. Requiere `SCHEDULE_EXACT_ALARM` (API 31+) / `USE_EXACT_ALARM` (API 33+) y gestionar el envío a ajustes si el permiso está denegado.

### `AlarmPlanner`

- Hora de alarma = inicio del **primer** turno del día − `leadMinutes` (por defecto 60, configurable).
- Turno partido: por defecto **una sola alarma**, la del primer turno. Segunda alarma opcional en ajustes.
- Días `FREE`/`VACATION`: no generan `AlarmPlan`.
- No programes alarmas en el pasado (usa el `Clock` inyectado); márcalas como omitidas y muéstralo.

---

## 8. Entrada del PDF

`intent-filter` en la activity de importación:

- `ACTION_SEND` con `application/pdf` (compartir desde WhatsApp) ← **el flujo principal, no lo dejes para el final**
- `ACTION_VIEW` con `application/pdf`
- Botón de selección manual con `ActivityResultContracts.OpenDocument`

---

## 9. Cómo trabajar

- **TDD siempre**: invoca la skill `test-driven-development` antes de implementar cualquier feature o fix. Test que falla → implementación → verde → refactor.
- Antes de decir que algo está terminado, invoca `verification-before-completion`: ejecuta los comandos y enseña la salida. Nada de "debería funcionar".
- Antes de crear una feature nueva, invoca `brainstorming` para acordar el alcance conmigo.
- Commits pequeños, y el idioma de los mensajes **consistente** en todo el repo.

### Comandos

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest          # el del bucle de TDD — debe seguir siendo rápido
./gradlew connectedDebugAndroidTest  # requiere emulador/dispositivo
```

---

## 10. Qué NO hacer

- **No introduzcas capas.** Nada de `domain/`, `data/` como capa, `Repository` + `RepositoryImpl`, use cases de una línea ni mappers entre modelos idénticos. Si crees que hace falta una capa, pregúntame antes.
- **No añadas Hilt, Koin ni ningún DI framework.**
- No refactorizar hacia KMP ni añadir un módulo `shared`.
- No cambiar la librería de PDF sin discutirlo.
- No programar alarmas desde un test ni desde un ViewModel sin pasar por la pantalla de revisión.
- No hardcodear "PLAZA MAR", las fechas de la semana 31 ni los nombres del fixture fuera de los tests.
- No inventar el mapeo de columnas: si el `HeaderDetector` no encuentra la fila de fechas, **falla con un error claro** en vez de asumir 14 columnas equiespaciadas.
- No añadir analytics, crash reporting ni dependencias de red. La app es 100% offline.

---

## 11. Roadmap

| Fase | Alcance |
|---|---|
| **0** | Esqueleto Gradle + Compose + fixture del PDF en tests |
| **1** | Pipeline de parseo completo con golden tests (sin UI) |
| **2** | Import por intent + selector de empleado + pantalla de revisión |
| **3** | `IntentAlarmScheduler` + persistencia de lo ya programado |
| **4** | Ajustes (antelación, turno partido, empleado por defecto) |
| **5** | `AlarmManager` propio con UI de alarma |
