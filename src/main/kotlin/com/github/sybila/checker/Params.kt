package com.github.sybila.checker

import java.util.*

/**
 * Empty interface that anyone can implement and use
 * to represent parametric sets.
 */
interface Params

/**
 * Working with parameters:
 *
 * If method takes/returns Params, the value should be a non empty set.
 * If method allows Params?, the argument/result can be null and unless stated otherwise,
 * the null value represents empty (unsatisfiable) parameters.
 *
 * Params object should implement a syntactic equality so that they can be stored in sets/maps.
 * Semantic equality is then implemented using a provided solver.
 *
 * Basic (non-solver) operations on params should always be as fast as possible (linear or better).
 */

/**
 * Logical conjunction of several parametric sets.
 */
data class And(val args: List<Params>) : Params {
    override fun toString(): String = "(and ${args.joinToString(separator = " ")})"
    constructor(vararg args: Params) : this(args.toList())
}

/**
 * Logical disjunction of several parametric sets.
 */
data class Or(val args: List<Params>) : Params {
    override fun toString(): String = "(or ${args.joinToString(separator = " ")})"
    constructor(vararg args: Params) : this(args.toList())
}

/**
 * Logical negation (with respect to bounds) of
 * a specific parametric set.
 */
data class Not(val inner: Params) : Params {
    override fun toString(): String = "(not $inner)"
}

/**
 * Tautology / Universe
 */
object TT : Params {
    override fun toString(): String = "(true)"
}

/*
    Utility functions for manipulating parameters:
 */

fun Params?.not(): Params? = when (this) {
    null -> TT
    TT -> null
    is Not -> this.inner
    else -> Not(this)
}

fun List<Params>.asConjunction(): Params? = when {
    this.isEmpty() -> TT
    this.size == 1 -> this.first()
    else -> And(this)
}

fun List<Params>.asDisjunction(): Params? = when {
    this.isEmpty() -> null
    this.size == 1 -> this.first()
    else -> Or(this)
}

fun Sequence<Params?>.asConjunction(): Params? {
    val result = ArrayList<Params>()
    for (p in this) {
        when (p) {
            null -> return null
            TT -> Unit
            is And -> result.addAll(p.args)
            else -> result.add(p)
        }
    }
    return when {
        result.size == 0 -> TT
        result.size == 1 -> result.first()
        else -> And(result)
    }
}

fun Sequence<Params?>.asDisjunction(): Params? {
    val result = ArrayList<Params>()
    for (p in this) {
        when (p) {
            TT -> return TT
            null -> Unit
            is Or -> result.addAll(p.args)
            else -> result.add(p)
        }
    }
    return when {
        result.size == 0 -> null
        result.size == 1 -> result.first()
        else -> Or(result)
    }
}

infix fun Params?.and(other: Params?): Params? = when {
    this === null || other === null -> null
    this === TT -> other
    other === TT -> this
    this is And && other is And -> And(this.args + other.args)
    this is And -> And(this.args + other)
    other is And -> And(other.args + this)
    else -> And(listOf(this, other))
}

infix fun Params?.or(other: Params?): Params? = when {
    this === null && other === null -> null
    this === null -> other
    other === null -> this
    this === TT || other == TT -> TT
    this is Or && other is Or -> Or(this.args + other.args)
    this is Or -> Or(this.args + other)
    other is Or -> Or(other.args + this)
    else -> Or(listOf(this, other))
}