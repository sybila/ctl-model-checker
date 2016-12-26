package com.github.sybila.checker

import com.github.sybila.checker.model.asExperiment
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
                val error = it.readLines().joinToString(separator = "\n").asExperiment().invoke()
                if (error != null) {
                    throw AssertionError("Experiment $it failed: $error")
                }
            }
        }
    }
}