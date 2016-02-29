package com.github.sybila.checker

interface WithStats {
    fun getStats(): Map<String, Any>
    fun resetStats()
}