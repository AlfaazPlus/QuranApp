package com.quranapp.android.locale

private const val ARAB_DIGITS = "٠١٢٣٤٥٦٧٨٩"
private const val ARABEXT_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
private const val ARAB_DECIMAL = '\u066B'

fun String.toFormatted(
    languageTag: String?,
    vararg args: Any,
): String {
    return toFormatted(languageTag ?: "en", null, *args)
}

fun String.toFormatted(
    languageTag: String,
    numeralSystem: NumeralSystem?,
    vararg args: Any,
): String {
    val pattern = this
    val numerals = numeralSystem ?: defaultNumerals(languageTag)
    val digits = when (numerals) {
        NumeralSystem.LATN -> null
        NumeralSystem.ARAB -> ARAB_DIGITS
        NumeralSystem.ARABEXT -> ARABEXT_DIGITS
    }
    val decimal = if (digits == null) '.' else ARAB_DECIMAL

    val out = StringBuilder(pattern.length)
    var index = 0
    var autoArg = 0

    while (index < pattern.length) {
        if (pattern[index] != '%') {
            out.append(pattern[index])
            index++
            continue
        }

        if (index + 1 < pattern.length && pattern[index + 1] == '%') {
            out.append('%')
            index += 2
            continue
        }

        index++
        val numberStart = index
        while (index < pattern.length && pattern[index].isDigit()) index++

        val argIndex = if (index < pattern.length && pattern[index] == '$' && index > numberStart) {
            val explicit = pattern.substring(numberStart, index).toInt() - 1
            index++
            explicit
        } else {
            index = numberStart
            autoArg++
        }

        var zeroPad = false
        if (index < pattern.length && pattern[index] == '0') {
            zeroPad = true
            index++
        }

        var width = 0
        while (index < pattern.length && pattern[index].isDigit()) {
            width = width * 10 + (pattern[index] - '0')
            index++
        }

        var precision: Int? = null
        if (index < pattern.length && pattern[index] == '.') {
            index++
            var parsed = 0
            while (index < pattern.length && pattern[index].isDigit()) {
                parsed = parsed * 10 + (pattern[index] - '0')
                index++
            }
            precision = parsed
        }

        if (index >= pattern.length) break

        val conversion = pattern[index]
        index++

        val arg = args.getOrNull(argIndex)
            ?: throw IllegalArgumentException("Missing format argument ${argIndex + 1}")

        out.append(
            when (conversion) {
                's', 'S' -> formatStringArg(arg, width)
                'd' -> shapeNumber(
                    padNumber(longValue(arg).toString(), width, zeroPad),
                    digits,
                    decimal
                )

                'x', 'X' -> {
                    val hex = longValue(arg).toString(16)
                        .let { if (conversion == 'X') it.uppercase() else it }
                    shapeNumber(padNumber(hex, width, zeroPad), digits, decimal)
                }

                'f' -> shapeNumber(
                    padNumber(formatFixed(doubleValue(arg), precision ?: 6), width, zeroPad),
                    digits,
                    decimal,
                )

                else -> throw IllegalArgumentException("Unsupported format %$conversion")
            },
        )
    }

    return out.toString()
}

private fun defaultNumerals(languageTag: String): NumeralSystem {
    val language = languageTag.substringBefore('-').substringBefore('_').lowercase()
    return when (language) {
        "ar" -> NumeralSystem.ARAB
        "fa" -> NumeralSystem.ARABEXT
        else -> NumeralSystem.LATN
    }
}

private fun longValue(arg: Any): Long = (arg as Number).toLong()

private fun doubleValue(arg: Any): Double = (arg as Number).toDouble()

private fun formatStringArg(arg: Any, width: Int): String {
    val text = arg.toString()
    if (width <= text.length) return text

    return " ".repeat(width - text.length) + text
}

private fun padNumber(text: String, width: Int, zeroPad: Boolean): String {
    if (width <= text.length) return text
    val negative = text.startsWith("-")
    val body = if (negative) text.substring(1) else text
    val pad = width - text.length
    val padding = if (zeroPad) "0".repeat(pad) else " ".repeat(pad)

    return if (negative && zeroPad) "-$padding$body" else padding + if (negative) "-$body" else body
}

private fun shapeNumber(text: String, digits: String?, decimal: Char): String {
    if (digits == null && decimal == '.') return text
    val out = StringBuilder(text.length)

    for (char in text) {
        when {
            char in '0'..'9' && digits != null -> out.append(digits[char - '0'])
            char == '.' -> out.append(decimal)
            else -> out.append(char)
        }
    }

    return out.toString()
}

private fun formatFixed(value: Double, precision: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"

    val negative = value < 0
    var scale = 1L
    repeat(precision) { scale *= 10L }

    val rounded = kotlin.math.floor(kotlin.math.abs(value) * scale + 0.5).toLong()
    val whole = rounded / scale
    val fraction = rounded % scale
    val body = if (precision == 0) {
        whole.toString()
    } else {
        whole.toString() + "." + fraction.toString().padStart(precision, '0')
    }

    return if (negative) "-$body" else body
}
