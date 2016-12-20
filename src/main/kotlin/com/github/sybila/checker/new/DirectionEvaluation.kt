package com.github.sybila.checker.new

import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Facet

fun DirectionFormula.eval(at: DirectionFormula.Atom.Proposition): Boolean = when (this) {
    is DirectionFormula.Atom.True -> true
    is DirectionFormula.Atom.Proposition -> this == at
    is DirectionFormula.Bool.And -> this.left.eval(at) && this.right.eval(at)
    is DirectionFormula.Bool.Or -> this.left.eval(at) || this.right.eval(at)
    is DirectionFormula.Bool.Implies -> !this.left.eval(at) || this.right.eval(at)
    is DirectionFormula.Bool.Equals -> this.left.eval(at) == this.right.eval(at)
    is DirectionFormula.Not -> !this.inner.eval(at)
    else -> false
}

fun String.increaseProp(): DirectionFormula.Atom.Proposition = DirectionFormula.Atom.Proposition(this, Facet.POSITIVE)
fun String.decreaseProp(): DirectionFormula.Atom.Proposition = DirectionFormula.Atom.Proposition(this, Facet.NEGATIVE)
