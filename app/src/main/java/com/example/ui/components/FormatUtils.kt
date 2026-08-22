package com.example.ui.components

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    val numberFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
}
