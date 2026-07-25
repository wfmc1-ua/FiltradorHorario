package com.wesley.cuadrantes.alarm

import com.wesley.cuadrantes.model.AlarmPlan
import java.time.LocalDate
import java.time.LocalTime

object AlarmPlanCodec {

    private const val SEP = "|"

    fun encode(plan: AlarmPlan): String =
        listOf(
            plan.date.toEpochDay(),
            plan.time.hour,
            plan.time.minute,
            plan.label.replace(SEP, "\\|"),
        ).joinToString(SEP)

    fun decode(raw: String): AlarmPlan? {
        val parts = raw.split(SEP)
        if (parts.size < 4) return null
        return runCatching {
            AlarmPlan(
                date = LocalDate.ofEpochDay(parts[0].toLong()),
                time = LocalTime.of(parts[1].toInt(), parts[2].toInt()),
                label = parts.drop(3).joinToString(SEP).replace("\\|", SEP),
            )
        }.getOrNull()
    }
}
