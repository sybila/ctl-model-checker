package com.github.sybila.checker

import com.github.sybila.huctl.*

fun Formula.bindReference(name: String, value: Int): Formula {
    return this.fold({
        if (this is Formula.Atom.Reference && this.name == name) {
            Formula.Atom.Reference(value.toString())
        } else this
    }, { inner ->
        if (this is Formula.Hybrid.At && this.name == name) {
            Formula.Hybrid.At(value.toString(), inner)
        } else this.copy(inner)
    }, Formula.Binary<*>::copy)
}

fun DirectionFormula.Atom.Proposition.negate(): DirectionFormula.Atom.Proposition {
    return if (this.facet == Facet.POSITIVE) this.name.decreaseProp() else this.name.increaseProp()
}

fun DirectionFormula.eval(at: DirectionFormula.Atom): Boolean = when (this) {
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

fun PathQuantifier.invertCardinality(): PathQuantifier = when (this) {
    PathQuantifier.A -> PathQuantifier.E
    PathQuantifier.E -> PathQuantifier.A
    PathQuantifier.pA -> PathQuantifier.pE
    PathQuantifier.pE -> PathQuantifier.pA
}

fun PathQuantifier.isExistential(): Boolean = this == PathQuantifier.E || this == PathQuantifier.pE
fun PathQuantifier.isNormalTimeFlow(): Boolean = this == PathQuantifier.E || this == PathQuantifier.A

inline fun <T> T?.assuming(predicate: (T) -> Boolean): T? {
    return if (this != null && predicate(this)) this else null
}

inline fun <T> T.byTheWay(action: () -> Unit): T {
    action()
    return this
}