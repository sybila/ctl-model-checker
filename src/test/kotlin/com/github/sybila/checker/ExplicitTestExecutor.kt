package com.github.sybila.checker

import com.github.sybila.checker.model.asSharedExperiment
import org.junit.Test
import java.io.File


class ExplicitTestExecutor {


    @Test
    fun runTests() {
        runTests(File("tests"))
    }

    private fun runTests(dir: File) {
        dir.listFiles().forEach {
            if (it.isDirectory) runTests(it)
            else if (it.name.endsWith(".model")) {
                println("Executing $it")
                try {
                    it.readLines().joinToString(separator = "\n").asSharedExperiment().invoke()
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw AssertionError("Experiment $it failed: $e")
                }
            }
        }
    }
}