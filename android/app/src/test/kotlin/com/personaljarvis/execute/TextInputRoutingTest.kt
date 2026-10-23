package com.personaljarvis.execute

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextInputRoutingTest {
    @Test
    fun routesChineseThroughClipboardPaste() {
        assertTrue(TextInputRouting.requiresClipboard("小猪搭手技能测试"))
    }

    @Test
    fun keepsAsciiOnTheFastInputPath() {
        assertFalse(TextInputRouting.requiresClipboard("hello 123"))
    }
}
