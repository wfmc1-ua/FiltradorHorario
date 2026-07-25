# Spec — Fase 3: Alarmas v2 con AlarmManager

Fecha: 2026-07-25

## Contexto

`IntentAlarmScheduler` (v1) usa `AlarmClock.ACTION_SET_ALARM`, que solo acepta hora y minuto
sin fecha. Crea la alarma para "la próxima vez que sean las X:XX", lo que hace que en la práctica
solo suene una alarma (la más próxima) y no las del resto de la semana.

Esta fase reemplaza v1 por `AlarmManagerScheduler` (v2), que usa `AlarmManager.setAlarmClock()`
con un timestamp exacto (fecha + hora). Añade una pantalla de alarma propia sobre la pantalla de
bloqueo, cancelación al reimportar y reschedule tras reinicio.

## Alcance

### Incluido
- `AlarmManagerScheduler` — nueva implementación de `AlarmScheduler`
- `AlarmReceiver` — BroadcastReceiver que recibe el disparo de `AlarmManager`
- `AlarmActivity` — pantalla fullscreen sobre bloqueo con botón Apagar
- `BootReceiver` — reschedule automático tras reinicio del teléfono
- `AlarmStore` — SharedPreferences para persistir los planes programados
- Cancelación al reimportar — cancela alarmas de la semana anterior antes de programar nuevas
- Gestión de permisos — `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`

### Excluido
- Snooze (posponer alarma)
- Sonido o vibración personalizados
- Room (SharedPreferences es suficiente)

## Arquitectura

### Nuevos archivos

```
alarm/
  AlarmManagerScheduler.kt   nueva implementación de AlarmScheduler
  AlarmReceiver.kt           BroadcastReceiver — disparo → notificación fullscreen
  BootReceiver.kt            BroadcastReceiver — BOOT_COMPLETED → reschedule
  AlarmStore.kt              SharedPreferences wrapper para persistir AlarmPlan
ui/alarm/
  AlarmActivity.kt           pantalla de alarma fullscreen
```

### Archivos modificados

```
alarm/AlarmScheduler.kt      añadir fun cancelAll()
AppContainer.kt              swap IntentAlarmScheduler → AlarmManagerScheduler
AndroidManifest.xml          nuevos permisos + receiver/activity declarations
ui/import/ImportViewModel.kt cancelAll() antes de guardar nuevo cuadrante
ui/review/ReviewViewModel.kt solicitar POST_NOTIFICATIONS antes de confirmar
```

## Flujo de programación

1. Usuario confirma alarmas en `AlarmConfirmDialog`
2. `ReviewViewModel.confirmAlarms(plans)` → por cada plan activo:
   - `AlarmManagerScheduler.schedule(plan)`
3. `AlarmManagerScheduler.schedule(plan)`:
   - Convierte `plan.date + plan.time` a epoch millis usando `ZoneId.systemDefault()`
   - `requestCode = plan.date.toEpochDay().toInt()` (determinista y único por fecha)
   - Construye `PendingIntent.getBroadcast(context, requestCode, alarmIntent, FLAG_IMMUTABLE)`
   - Llama `alarmManager.setAlarmClock(AlarmClockInfo(triggerMs, showIntent), pendingIntent)`
   - Guarda el plan en `AlarmStore`

## Flujo de cancelación

Activado desde `ImportViewModel` cuando hay un cuadrante previo cargado:

1. `alarmScheduler.cancelAll()`
2. `AlarmManagerScheduler.cancelAll()`:
   - Lee planes de `AlarmStore`
   - Por cada plan, reconstruye el mismo `PendingIntent` (mismo requestCode) y llama `alarmManager.cancel(pi)`
   - Borra `AlarmStore`

## Flujo de disparo

1. `AlarmManager` dispara en el timestamp exacto → `AlarmReceiver.onReceive()`
2. `AlarmReceiver`:
   - Crea canal de notificación `"alarm"` (importancia MAX) si no existe
   - Construye `Notification` con:
     - `setFullScreenIntent(AlarmActivity pendingIntent, true)`
     - `setPriority(PRIORITY_MAX)` + `setCategory(CATEGORY_ALARM)`
     - Título: label del plan (ej. "Turno 10:00–15:00")
   - Publica la notificación
3. `AlarmActivity`:
   - `setShowWhenLocked(true)` + `setTurnScreenOn(true)` + `FLAG_KEEP_SCREEN_ON`
   - Muestra: hora de alarma grande, label del turno, nombre implícito en el label
   - Botón **Apagar**: cancela la notificación + `finish()`
   - Si `USE_FULL_SCREEN_INTENT` no está concedido (API 34+), Android lo muestra como
     heads-up notification — la alarma sigue funcionando, solo cambia la presentación

## Flujo de reboot

1. `BootReceiver` recibe `BOOT_COMPLETED`
2. Lee todos los `AlarmPlan` de `AlarmStore`
3. Filtra los que aún no han pasado (`plan.date.atTime(plan.time) > now`)
4. Llama `AlarmManagerScheduler.schedule(plan)` por cada uno
   (no guarda de nuevo en `AlarmStore` — ya estaban guardados)

## AlarmStore

```kotlin
class AlarmStore(context: Context) {
    // Clave: "epoch_day:hour:minute:label" — no necesita JSON
    fun save(plans: List<AlarmPlan>)
    fun load(): List<AlarmPlan>
    fun clear()
}
```

## Permisos y manifest

### Permisos nuevos
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />       <!-- API 31-32 -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />             <!-- API 33+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />           <!-- API 33+ -->
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />      <!-- API 34+ -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Componentes nuevos en manifest
```xml
<receiver android:name=".alarm.AlarmReceiver" android:exported="false" />
<receiver android:name=".alarm.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
<activity
    android:name=".ui.alarm.AlarmActivity"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:excludeFromRecents="true"
    android:taskAffinity=""
    android:launchMode="singleInstance" />
```

## Gestión de permisos en UI

| Permiso | Cuándo | Qué muestra la UI |
|---|---|---|
| `POST_NOTIFICATIONS` (API 33+) | Primera vez que el usuario pulsa "Programar alarmas" | Diálogo del sistema |
| `SCHEDULE_EXACT_ALARM` (API 31-32) | Al intentar programar si no está concedido | Banner en pantalla de revisión con botón "Ir a Ajustes" |
| `USE_FULL_SCREEN_INTENT` (API 34+) | No se pide activamente | Fallback silencioso a heads-up notification |

## Testing

- **Unitario:** `AlarmStoreTest` — guardar, cargar, limpiar; filtro de pasadas en BootReceiver
- **Instrumentado:** `AlarmManagerSchedulerTest` — verificar que `setAlarmClock` se llama con el timestamp correcto (mockear `AlarmManager`)
- Los golden tests del parser no se tocan
