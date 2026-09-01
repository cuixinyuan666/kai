package com.inspiredandroid.kai.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelPairLabelTest {

    @Test
    fun `splitModelPairLabel splits parent and child`() {
        val (parent, child) = splitModelPairLabel("OpenCode / hy3")
        assertEquals("OpenCode", parent)
        assertEquals("hy3", child)
    }

    @Test
    fun `splitModelPairLabel keeps single token as parent`() {
        val (parent, child) = splitModelPairLabel("hy3")
        assertEquals("hy3", parent)
        assertEquals(null, child)
    }
}
