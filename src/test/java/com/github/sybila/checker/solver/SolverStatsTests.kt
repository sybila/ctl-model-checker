package com.github.sybila.checker.solver


import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

class SolverStatsTest {

    @Test
    fun bitSetSolverTest() {
        BitSetSolver(1).run {
            //dry run
            SolverStats.reset(null)
            tt.isSat()
            SolverStats.printGlobal()
            //actually print something
            val bytes = ByteArrayOutputStream()
            val printTo = PrintStream(bytes)
            printTo.println()   //prettier result regex
            SolverStats.reset(printTo)
            tt.isSat()
            SolverStats.printGlobal()
            val printed = String(bytes.toByteArray(), StandardCharsets.UTF_8)
            assertTrue(printed.matches(
                    """\r?
Started solver throughput measuring\. Metric: No\. of solver calls per second\.\r?
Total elapse time: \d+ ms\r?
Total solver calls: 1\r?
Average throughput: 1/s\r?
""".toRegex()
            ))
        }
    }

    @Test
    fun intSetSolverTest() {
        IntSetSolver(setOf(0,1)).run {
            //dry run
            SolverStats.reset(null)
            tt.isSat()
            SolverStats.printGlobal()
            //actually print something
            val bytes = ByteArrayOutputStream()
            val printTo = PrintStream(bytes)
            printTo.println()   //prettier result regex
            SolverStats.reset(printTo)
            tt.isSat()
            SolverStats.printGlobal()
            val printed = String(bytes.toByteArray(), StandardCharsets.UTF_8)
            println(printed)
            assertTrue(printed.matches(
                    """\r?
Started solver throughput measuring\. Metric: No\. of solver calls per second\.\r?
Total elapse time: \d+ ms\r?
Total solver calls: 1\r?
Average throughput: 1/s\r?
""".toRegex()
            ))
        }
    }


}