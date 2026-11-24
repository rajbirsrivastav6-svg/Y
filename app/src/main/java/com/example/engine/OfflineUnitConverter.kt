package com.example.engine

import java.text.DecimalFormat

data class UnitConversionResult(
    val sourceSummary: String,
    val targetSummary: String
)

object OfflineUnitConverter {

    private val formatter = DecimalFormat("#,##0.####")

    private val lengthToMeters = mapOf(
        "m" to 1.0, "meter" to 1.0, "meters" to 1.0, "metre" to 1.0, "metres" to 1.0,
        "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0, "kms" to 1000.0,
        "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
        "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
        "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344,
        "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
        "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
        "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144
    )

    private val weightToGrams = mapOf(
        "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
        "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0, "kgs" to 1000.0,
        "mg" to 0.001, "milligram" to 0.001, "milligrams" to 0.001,
        "lb" to 453.59237, "lbs" to 453.59237, "pound" to 453.59237, "pounds" to 453.59237,
        "oz" to 28.349523, "ounce" to 28.349523, "ounces" to 28.349523
    )

    private val dataToMb = mapOf(
        "kb" to 1.0 / 1024.0,
        "mb" to 1.0,
        "gb" to 1024.0,
        "tb" to 1024.0 * 1024.0
    )

    private val speedToMps = mapOf(
        "mps" to 1.0, "m/s" to 1.0,
        "kmh" to 1.0 / 3.6, "km/h" to 1.0 / 3.6, "kph" to 1.0 / 3.6,
        "mph" to 0.44704
    )

    fun tryConvert(rawInput: String): UnitConversionResult? {
        val pattern = Regex(
            """(?:convert\s+)?(-?\d+(?:\.\d+)?)\s*([a-zA-Z/]+)\s+(?:to|in|into)\s+([a-zA-Z/]+)""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(rawInput.trim()) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val from = match.groupValues[2].lowercase()
        val to = match.groupValues[3].lowercase()

        // Temperature
        val tempFrom = normalizeTemp(from)
        val tempTo = normalizeTemp(to)
        if (tempFrom != null && tempTo != null) {
            val celsius = when (tempFrom) {
                "C" -> value
                "F" -> (value - 32.0) * 5.0 / 9.0
                else -> value - 273.15
            }
            val out = when (tempTo) {
                "C" -> celsius
                "F" -> (celsius * 9.0 / 5.0) + 32.0
                else -> celsius + 273.15
            }
            return UnitConversionResult(
                sourceSummary = "${formatter.format(value)} °$tempFrom",
                targetSummary = "${formatter.format(out)} °$tempTo"
            )
        }

        // Length
        if (from in lengthToMeters && to in lengthToMeters) {
            val meters = value * lengthToMeters.getValue(from)
            val result = meters / lengthToMeters.getValue(to)
            return UnitConversionResult(
                sourceSummary = "${formatter.format(value)} $from",
                targetSummary = "${formatter.format(result)} $to"
            )
        }

        // Weight
        if (from in weightToGrams && to in weightToGrams) {
            val grams = value * weightToGrams.getValue(from)
            val result = grams / weightToGrams.getValue(to)
            return UnitConversionResult(
                sourceSummary = "${formatter.format(value)} $from",
                targetSummary = "${formatter.format(result)} $to"
            )
        }

        // Data storage
        if (from in dataToMb && to in dataToMb) {
            val mb = value * dataToMb.getValue(from)
            val result = mb / dataToMb.getValue(to)
            return UnitConversionResult(
                sourceSummary = "${formatter.format(value)} ${from.uppercase()}",
                targetSummary = "${formatter.format(result)} ${to.uppercase()}"
            )
        }

        // Speed
        if (from in speedToMps && to in speedToMps) {
            val mps = value * speedToMps.getValue(from)
            val result = mps / speedToMps.getValue(to)
            return UnitConversionResult(
                sourceSummary = "${formatter.format(value)} $from",
                targetSummary = "${formatter.format(result)} $to"
            )
        }

        return null
    }

    private fun normalizeTemp(unit: String): String? = when (unit) {
        "c", "celsius", "centigrade" -> "C"
        "f", "fahrenheit" -> "F"
        "k", "kelvin" -> "K"
        else -> null
    }
}
