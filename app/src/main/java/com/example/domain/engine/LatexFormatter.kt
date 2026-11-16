package com.example.domain.engine

/**
 * Lightweight deterministic LaTeX-to-Unicode math renderer for Compose text display
 * in the Formula Bank and Revision Session quick-sheet.
 */
object LatexFormatter {
    private val replacements = listOf(
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\Delta" to "Δ",
        "\\epsilon" to "ε",
        "\\varepsilon" to "ε",
        "\\theta" to "θ",
        "\\lambda" to "λ",
        "\\mu" to "μ",
        "\\nu" to "ν",
        "\\pi" to "π",
        "\\rho" to "ρ",
        "\\sigma" to "σ",
        "\\Sigma" to "Σ",
        "\\tau" to "τ",
        "\\phi" to "φ",
        "\\omega" to "ω",
        "\\Omega" to "Ω",
        "\\int" to "∫",
        "\\sum" to "∑",
        "\\prod" to "∏",
        "\\infty" to "∞",
        "\\partial" to "∂",
        "\\nabla" to "∇",
        "\\rightarrow" to "→",
        "\\to" to "→",
        "\\leftarrow" to "←",
        "\\leftrightarrow" to "↔",
        "\\Rightarrow" to "⇒",
        "\\rightleftharpoons" to "⇌",
        "\\le" to "≤",
        "\\leq" to "≤",
        "\\ge" to "≥",
        "\\geq" to "≥",
        "\\ne" to "≠",
        "\\neq" to "≠",
        "\\approx" to "≈",
        "\\propto" to "∝",
        "\\pm" to "±",
        "\\times" to "×",
        "\\cdot" to "·",
        "\\sqrt" to "√",
        "\\degree" to "°"
    )

    private val fracRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
    private val supMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', 'n' to 'ⁿ', 'i' to 'ⁱ'
    )
    private val subMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋'
    )

    fun formatForDisplay(rawLatex: String): String {
        if (rawLatex.isBlank()) return ""
        var out = rawLatex.trim().removePrefix("$$").removeSuffix("$$").removePrefix("$").removeSuffix("$")
        // Convert \frac{a}{b} -> (a)/(b)
        repeat(3) {
            out = fracRegex.replace(out) { match ->
                "(${match.groupValues[1]}) / (${match.groupValues[2]})"
            }
        }
        for ((token, symbol) in replacements) {
            out = out.replace(token, symbol)
        }
        out = Regex("""\^\{([^{}]+)\}""").replace(out) { m ->
            m.groupValues[1].map { supMap[it] ?: it }.joinToString("")
        }
        out = Regex("""\^([0-9n+-])""").replace(out) { m ->
            val ch = m.groupValues[1].first()
            (supMap[ch] ?: "^$ch").toString()
        }
        out = Regex("""_\{([^{}]+)\}""").replace(out) { m ->
            m.groupValues[1].map { subMap[it] ?: it }.joinToString("")
        }
        out = Regex("""_([0-9+-])""").replace(out) { m ->
            val ch = m.groupValues[1].first()
            (subMap[ch] ?: "_$ch").toString()
        }
        return out.replace("{", "").replace("}", "")
    }
}
