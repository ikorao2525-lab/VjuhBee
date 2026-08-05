package com.vjuhbee.beecalc.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** «05.08.2026» — одинаково понятно на любом устройстве. */
fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
