package com.musique.render

/**
 * Text-level fixes applied to Verovio's SVG before androidsvg draws it. Pure string code, so
 * it is unit-tested on the JVM.
 */
object SvgFixes {
    // Verovio marks layout milestones as `<g class="... placeholder ...">` and hides them with an
    // embedded CSS rule that androidsvg does not apply: they showed up as stray dots.
    private val placeholderGroup = Regex("""<g(\s[^>]*?\bclass="[^"]*\bplaceholder\b[^"]*")""")

    fun prepare(svg: String): String = placeholderGroup.replace(svg) { "<g${it.groupValues[1]} display=\"none\"" }

    /**
     * Font families Verovio writes for text ("Times, serif"). Verovio lays lyrics and directions out
     * with Times metrics; Android's serif is wider, which made neighbouring words run together.
     */
    fun isTimesFamily(family: String): Boolean =
        family.trim().trim('"', '\'').lowercase() in setOf("times", "times new roman", "liberation", "liberation serif")

    /** Asset name of the Times-metric face for a CSS weight and style. */
    fun timesFaceAsset(weight: Int, style: String?): String {
        val bold = weight >= 600
        val italic = style != null && (style.equals("italic", true) || style.equals("oblique", true))
        val face = when {
            bold && italic -> "BoldItalic"
            bold -> "Bold"
            italic -> "Italic"
            else -> "Regular"
        }
        return "fonts/LiberationSerif-$face.ttf"
    }
}
