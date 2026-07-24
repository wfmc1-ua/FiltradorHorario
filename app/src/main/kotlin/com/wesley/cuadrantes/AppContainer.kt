package com.wesley.cuadrantes

import android.content.Context
import com.wesley.cuadrantes.alarm.AlarmPlanner
import com.wesley.cuadrantes.alarm.AlarmScheduler
import com.wesley.cuadrantes.alarm.IntentAlarmScheduler
import com.wesley.cuadrantes.model.Cuadrante
import com.wesley.cuadrantes.parser.CuadranteParser
import com.wesley.cuadrantes.parser.NameMatcher
import com.wesley.cuadrantes.parser.PdfBoxTextExtractor
import com.wesley.cuadrantes.parser.PdfTextExtractor
import com.wesley.cuadrantes.util.Clock
import com.wesley.cuadrantes.util.SystemClock

class AppContainer(context: Context) {
    val clock: Clock = SystemClock()
    val extractor: PdfTextExtractor = PdfBoxTextExtractor()
    val parser: CuadranteParser = CuadranteParser()
    val nameMatcher: NameMatcher = NameMatcher()
    val alarmScheduler: AlarmScheduler = IntentAlarmScheduler(context)
    val alarmPlanner: AlarmPlanner = AlarmPlanner(clock)

    var currentCuadrante: Cuadrante? = null
}
