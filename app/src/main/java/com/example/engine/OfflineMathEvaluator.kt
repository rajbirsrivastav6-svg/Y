package com.example.engine

import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class MathEvaluation(
    val expression: String,
    val formattedResult: String,
    val numericValue: Double
)

object OfflineMathEvaluator {

    private val formatter = DecimalFormat("#,##0.######")

    fun tryEvaluate(rawInput: String): MathEvaluation? {
        val cleaned = rawInput.trim()
        if (cleaned.isEmpty()) return null

        // Check percentage phrasing like "25% of 400" or "what is 15% of 1200"
        val percentRegex = Regex(
            """(?:what\s+is\s+|calculate\s+|solve\s+)?(\d+(?:\.\d+)?)\s*%\s+of\s+(\d+(?:\.\d+)?)""",
            RegexOption.IGNORE_CASE
        )
        percentRegex.find(cleaned)?.let { match ->
            val pct = match.groupValues[1].toDoubleOrNull() ?: return null
            val base = match.groupValues[2].toDoubleOrNull() ?: return null
            val value = (pct / 100.0) * base
            return MathEvaluation(
                expression = "${formatNum(pct)}% of ${formatNum(base)}",
                formattedResult = formatNum(value),
                numericValue = value
            )
        }

        // Normalize natural language prefixes and operators
        var expr = cleaned
            .replace(Regex("""^(what\s+is|calculate|compute|solve|eval|evaluate|kitna\s+hoga|hisab\s+karo)\s+""", RegexOption.IGNORE_CASE), "")
            .replace("?", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(Regex("""\bplus\b""", RegexOption.IGNORE_CASE), "+")
            .replace(Regex("""\bminus\b""", RegexOption.IGNORE_CASE), "-")
            .replace(Regex("""\btimes\b|\bmultiplied\s+by\b""", RegexOption.IGNORE_CASE), "*")
            .replace(Regex("""\bdivided\s+by\b""", RegexOption.IGNORE_CASE), "/")
            .replace(" ", "")

        // Must contain at least one digit and one math operator or supported function
        val hasDigit = expr.any { it.isDigit() }
        val hasOperatorOrFunc = expr.any { it in charArrayOf('+', '-', '*', '/', '^', '%') } ||
            listOf("sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(").any {
                expr.contains(it, ignoreCase = true)
            }
        if (!hasDigit || !hasOperatorOrFunc) return null

        // Allow only valid math characters
        val allowed = Regex("""^[0-9a-zA-Z+\-*/^%().]+$""")
        if (!allowed.matches(expr)) return null

        return try {
            val parser = Parser(expr.lowercase())
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null
            else MathEvaluation(
                expression = cleaned.trimEnd('?').trim(),
                formattedResult = formatNum(result),
                numericValue = result
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun formatNum(value: Double): String {
        val rounded = if (abs(value - value.toLong()) < 1e-9) value.toLong().toDouble() else value
        return formatter.format(rounded)
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = 0

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char: ${ch.toChar()}")
            return x
        }

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%'.code) -> {
                        val mod = parseFactor()
                        if (mod == 0.0) throw ArithmeticException("Modulo by zero")
                        x %= mod
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                x = when (func) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    else -> {
                        if (!eat('('.code)) throw IllegalArgumentException("Expected '(' after $func")
                        val arg = parseExpression()
                        if (!eat(')'.code)) throw IllegalArgumentException("Missing ')' after $func")
                        when (func) {
                            "sqrt" -> sqrt(arg)
                            "sin" -> sin(Math.toRadians(arg))
                            "cos" -> cos(Math.toRadians(arg))
                            "tan" -> tan(Math.toRadians(arg))
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            "abs" -> abs(arg)
                            else -> throw IllegalArgumentException("Unknown function: $func")
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character")
            }

            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }
            return x
        }
    }
}
