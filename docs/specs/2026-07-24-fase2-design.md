# Fase 2 — Diseño: Import · Selector de empleado · Pantalla de revisión

**Fecha:** 2026-07-24  
**Estado:** aprobado por el usuario, pendiente de implementación

---

## Alcance

Fase 2 cubre el flujo completo de usuario desde que recibe un PDF hasta que confirma las alarmas:

1. Recibir el PDF (intent o selección manual)
2. Parsear el cuadrante (pipeline ya implementado en Fase 1)
3. Elegir el empleado
4. Revisar y editar el horario extraído
5. Confirmar y programar las alarmas

**Fuera de alcance en Fase 2:**
- Persistencia entre sesiones (Fase 3)
- `AlarmManager` propio (Fase 5)
- Ajustes de antelación y turno partido (Fase 4)

---

## Decisiones de diseño

| Pregunta | Decisión |
|---|---|
| Layout pantalla de revisión | **3B** — scroll horizontal de semana + panel de detalle del día seleccionado |
| ¿Editable? | **Sí** — cambios de turno y días libres de última hora |
| Edición de horas | **Bottom sheet propio** con dos ruedas de scroll (horas / minutos) |
| Arquitectura de navegación | **NavHost con tres rutas** — historial de navegación (atrás funciona entre pantallas) |
| Persistencia de ediciones | **Solo en sesión** — sin Room hasta Fase 3 |

---

## Navegación

Una única `MainActivity` con un `NavHost`. Tres rutas:

```
import          → ImportScreen
selector        → EmployeeSelectorScreen
review/{index}  → ReviewScreen
```

**Flujo normal:** `import → selector → review`

- Atrás en `selector` → vuelve a `import` (cambiar PDF)
- Atrás en `review` → vuelve a `selector` (cambiar empleado)

**Flujo por intent:** cuando `MainActivity` recibe `ACTION_SEND` o `ACTION_VIEW` con `application/pdf`, navega directamente a `import` con el URI resuelto, saltándose la pantalla de inicio manual.

Los datos parseados (`Cuadrante`) no se serializan en la ruta de navegación. Viven en el `AppContainer` (instanciado en `Application`) y se pasan a los ViewModels vía factory.

---

## Pantallas y ViewModels

### ImportScreen + ImportViewModel

**Estado:**
```kotlin
sealed class ImportUiState {
    object Idle : ImportUiState()
    object Loading : ImportUiState()
    data class Error(val message: String) : ImportUiState()
    object Done : ImportUiState()  // navega a selector automáticamente
}
```

**Comportamiento:**
- Si llega URI por intent → empieza a parsear inmediatamente, muestra spinner
- Sin intent → muestra botón "Seleccionar PDF" + instrucción de compartir desde WhatsApp
- Al terminar → deposita `Cuadrante` en `AppContainer` y navega a `selector`
- Error de parser o de acceso al fichero → muestra mensaje con botón "Intentar con otro PDF"

---

### EmployeeSelectorScreen + SelectorViewModel

Recupera el `Cuadrante` del `AppContainer`.

**Comportamiento:**
- Campo de búsqueda con filtrado en tiempo real por nombre normalizado (NFD, sin diacríticos)
- Lista de resultados con avatar inicial, nombre completo y horas de contrato
- Si hay un único resultado → se resalta automáticamente
- Si hay varios con el mismo nombre parcial → se muestran todos, el usuario elige
- Sin resultados → "Sin coincidencias" bajo el campo, sin navegar
- Al confirmar selección → navega a `review/{index}` con el índice del empleado en `Cuadrante.employees`

---

### ReviewScreen + ReviewViewModel

Recibe `index`, recupera `EmployeeSchedule` del `AppContainer`. Mantiene en memoria una lista mutable de `EditableDayState`.

**Modelo de edición en memoria:**
```kotlin
data class EditableDayState(
    val date: LocalDate,
    val originalShifts: List<Shift>,   // del PDF, para restaurar
    val status: DayStatus,
    val shifts: List<Shift>,           // puede estar editado
    val alarmEnabled: Boolean,
)
```

**Layout (3B):**
- Barra info: nombre del empleado, centro, semana
- Scroll horizontal de tarjetas de día (7 tarjetas): día abreviado, número, horas o badge LIBRE/VAC
- Panel de detalle del día seleccionado (tap en tarjeta para seleccionar):
  - Chips de estado: `Trabajo` / `Libre` / `Vacaciones` (mutuamente excluyentes)
  - Si `WORK`: hora entrada (tap → bottom sheet) · flecha → hora salida (tap → bottom sheet) · toggle de alarma
  - Hora de alarma calculada: primer turno − 60 min
- Botón fijo abajo: "Programar alarmas (N días)"

**Edición de estado del día:**
- Al elegir `FREE` o `VACATION` → shifts se ocultan, alarma se desactiva
- Al volver a `WORK` → se restauran `originalShifts` del PDF

**Bottom sheet de edición de hora:**
- Se abre al tocar hora de entrada o salida
- Dos ruedas de scroll: horas (0–23) y minutos (0–59)
- Al pulsar "Aplicar" → actualiza el shift y recalcula hora de alarma

**Confirmación de alarmas:**
- "Programar alarmas" → `AlarmPlanner.plan()` → diálogo con lista de `AlarmPlan`:
  - Cada ítem muestra: día, hora de alarma, label del turno, checkbox (desmarcable)
  - Alarmas en el pasado aparecen tachadas con nota "ya pasó"
- Solo al pulsar "Confirmar" en el diálogo se ejecuta `IntentAlarmScheduler`

---

## Errores y casos límite

| Caso | Comportamiento |
|---|---|
| PDF sin cabecera de fechas | Error claro del parser → mensaje en `ImportScreen` + botón para elegir otro PDF |
| Ningún empleado coincide | "Sin resultados" en `EmployeeSelectorScreen`, sin navegar |
| Varios empleados con mismo nombre parcial | Se listan todos, el usuario elige. Nunca se elige automáticamente |
| Alarma en el pasado | `AlarmPlanner` la marca como omitida (usa `Clock` inyectado). Aparece tachada en el diálogo |
| Turno que cruza medianoche (fin < inicio) | Aviso visible en el panel de detalle. Alarma se crea igualmente con hora de entrada |
| URI de intent sin permiso de lectura | Error de acceso → mismo flujo que PDF no reconocido |

---

## Clases nuevas en Fase 2

| Clase | Paquete | Notas |
|---|---|---|
| `MainActivity` | `ui/` | Entry point, NavHost, recibe intents |
| `ImportScreen` | `ui/import/` | |
| `ImportViewModel` | `ui/import/` | |
| `EmployeeSelectorScreen` | `ui/selector/` | |
| `SelectorViewModel` | `ui/selector/` | |
| `ReviewScreen` | `ui/review/` | Layout 3B |
| `ReviewViewModel` | `ui/review/` | Estado `EditableDayState` |
| `EditableDayState` | `ui/review/` | Data class de edición en memoria |
| `ShiftEditBottomSheet` | `ui/review/` | Composable con ruedas de scroll |
| `AlarmConfirmDialog` | `ui/review/` | Lista de AlarmPlan con checkboxes |
| `AppContainer` | raíz | Instanciado en `Application`, pasa datos a VMs vía factory |
| `AlarmPlanner` | `alarm/` | Calcula `AlarmPlan` a partir de `EditableDayState` |
| `IntentAlarmScheduler` | `alarm/` | Implementa `AlarmScheduler` con `AlarmClock.ACTION_SET_ALARM` |

---

## Lo que NO cambia de Fase 1

- Todo el pipeline del parser (`RowClusterer`, `HeaderDetector`, `GridBuilder`, `EmployeeRowMapper`, `NameMatcher`, `CuadranteParser`)
- Los modelos de dominio (`Cuadrante`, `EmployeeSchedule`, `DaySchedule`, `Shift`, `AlarmPlan`)
- Los golden tests
- `PdfBoxTextExtractor` y la interfaz `PdfTextExtractor`
