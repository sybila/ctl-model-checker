package com.github.sybila.checker

import com.github.sybila.huctl.HUCTLParser
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

class ProgressTest {

    @Test
    fun progressPrinter() {
        val parser = HUCTLParser()
        val temporal = parser.formula("EX AX EF AF true")
        val hybrid = parser.formula("bind z: exists x: forall y: at x: z")
        val bool = parser.formula("(true || true) && !true")
        val model = ReachModel(1,1)

        val bytes = ByteArrayOutputStream()
        val printTo = PrintStream(bytes)
        printTo.println()
        Checker(model, progress = printTo).run {
            this.verify(temporal)
            this.verify(hybrid)
            this.verify(bool)
        }

        val printed = String(bytes.toByteArray(), StandardCharsets.UTF_8)
        assertTrue(printed.matches(
"""
Start formula: \{true}EX \{true}AX \{true}EF \{true}AF true
Start operator: AU
Start operator: EU
Start operator: AX
Start operator: EX
Start formula: \(bind z : \(exists x in true : \(forall y in true : \(at x : z\)\)\)\)
Start operator: Bind
Start operator: Exists
Start operator: Exists
Start operator: At
Start operator: Not
Start operator: Not
Start formula: \(\(true \|\| true\) && !true\)
Start operator: Not
Start operator: Or
Start operator: And
""".toRegex()
        ))

    }
}