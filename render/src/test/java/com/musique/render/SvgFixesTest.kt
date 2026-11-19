package com.musique.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgFixesTest {
    @Test
    fun hidesPlaceholderGroupsOnly() {
        val svg = """<g id="a" class="section placeholder systemMilestone"><ellipse cx="1" cy="1"/></g>""" +
            """<g id="b" class="staff"><path d="M0 0"/></g>"""
        val out = SvgFixes.prepare(svg)
        assertTrue(out.contains("""<g id="a" class="section placeholder systemMilestone" display="none">"""))
        assertTrue(out.contains("""<g id="b" class="staff">"""))
    }

    @Test
    fun recognisesVerovioTextFamilies() {
        assertTrue(SvgFixes.isTimesFamily("Times"))
        assertTrue(SvgFixes.isTimesFamily(" 'Times New Roman' "))
        assertFalse(SvgFixes.isTimesFamily("serif"))
        assertFalse(SvgFixes.isTimesFamily("Leipzig"))
    }

    @Test
    fun picksTheFaceFromWeightAndStyle() {
        assertEquals("fonts/LiberationSerif-Regular.ttf", SvgFixes.timesFaceAsset(400, "Normal"))
        assertEquals("fonts/LiberationSerif-Bold.ttf", SvgFixes.timesFaceAsset(700, null))
        assertEquals("fonts/LiberationSerif-Italic.ttf", SvgFixes.timesFaceAsset(400, "Italic"))
        assertEquals("fonts/LiberationSerif-BoldItalic.ttf", SvgFixes.timesFaceAsset(700, "italic"))
    }
}
