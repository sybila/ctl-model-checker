package com.github.sybila.checker.uctl

import com.github.sybila.ctl.Atom

//Direction formulas

interface DFormula

data class Direction(
        val dimension: Int,
        val positive: Boolean
) : DFormula

data class DAnd(
        val left: DFormula,
        val right: DFormula
) : DFormula

data class DNot(
        val inner: DFormula
) : DFormula

val anyDirection = Direction(-1, true)
val noDirection = Direction(-1, false)

//UCTL Formulas

interface UFormula;

data class UProposition(
        val proposition: Atom
) : UFormula

data class UName(
        val name: String
) : UFormula

data class UNot(
        val formula: UFormula
) : UFormula

data class UAnd(
        val left: UFormula,
        val right: UFormula
) : UFormula

data class UExists(
        val name: String,
        val inner: UFormula
) : UFormula

data class UAt(
        val name: String,
        val inner: UFormula
) : UFormula

data class UBind(
        val name: String,
        val inner: UFormula
) : UFormula

data class UEU(
        val forward: Boolean,
        val path: UFormula,
        val reach: UFormula,
        val pathDirection: DFormula,
        val reachDirection: DFormula
) : UFormula

data class UAU(
        val forward: Boolean,
        val path: UFormula,
        val reach: UFormula,
        val pathDirection: DFormula,
        val reachDirection: DFormula
) : UFormula

data class UEW(
        val forward: Boolean,
        val path: UFormula,
        val reach: UFormula,
        val pathDirection: DFormula,
        val reachDirection: DFormula
) : UFormula

data class UAW(
        val forward: Boolean,
        val path: UFormula,
        val reach: UFormula,
        val pathDirection: DFormula,
        val reachDirection: DFormula
) : UFormula