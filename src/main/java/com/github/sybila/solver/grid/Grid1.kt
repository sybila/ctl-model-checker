package com.github.sybila.solver.grid

import java.util.*

class Grid1(val thresholds: DoubleArray, val values: BitSet) {

    companion object {
        val EMPTY = Grid1(DoubleArray(0), BitSet())
    }

    fun cut(other: Grid1): Grid1 = this.cut(other.thresholds)

    fun cut(cut: DoubleArray): Grid1 {
        val new = (thresholds + cut).toSet().sorted().toDoubleArray()
        return if (Arrays.equals(new, thresholds)) this else {
            val newValues = BitSet(values.size())
            values.stream().forEach { value ->
                val newLow = new.binarySearch(thresholds[value])
                val newHigh = new.binarySearch(thresholds[value + 1])
                newValues.set(newLow, newHigh)  // no need to -1, since set method is right exclusive
            }
            Grid1(new, newValues)
        }
    }

    fun simplify(): Grid1 {
        return if (values.isEmpty) Grid1.EMPTY
        else {
            // prune thresholds
            val new = ArrayList<Double>(thresholds.size)
            val lastFieldIndex = thresholds.size - 2
            // first threshold is valid if first grid field is set
            if (values[0]) new.add(thresholds[0])
            for (t in 1..lastFieldIndex) {
                if (values[t-1] != values[t]) new.add(thresholds[t])
            }
            if (values[lastFieldIndex]) new.add(thresholds.last())

            val newThresholds = new.toDoubleArray()

            if (Arrays.equals(thresholds, newThresholds)) this
            else {
                val newValues = BitSet()
                values.stream().forEach { value ->
                    val newLow = newThresholds.binarySearch(thresholds[value])
                    // If reduction is correct, only one grid field at a time will be set (otherwise they would merge)
                    // Merged fields won't be in the array and hence newLow < 0 for them.
                    if (newLow >= 0) {
                        newValues.set(newLow, newLow+1)
                    }
                }
                Grid1(newThresholds, newValues)
            }
        }
    }

    fun copy(thresholds: DoubleArray = this.thresholds, values: BitSet = this.values) = Grid1(thresholds, values)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Grid1

        if (!Arrays.equals(thresholds, other.thresholds)) return false
        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(thresholds)
        result = 31 * result + values.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        var first = true
        values.stream().forEach { value ->
            if (!first) {
                append(", ")
                first = false
            }
            append("[${thresholds[value]}, ${thresholds[value+1]}]")
        }
    }

}