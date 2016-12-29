package com.github.sybila.checker

import com.github.sybila.huctl.*
import java.util.*

fun Formula.canonicalReferences(): Formula {
    var lastName = 0
    val names = HashMap<String, String>()
    return this.mapLeafs { atom ->
        if (atom is Formula.Atom.Reference) {
            val realName = atom.name
            if (realName in names) {
                Formula.Atom.Reference(names[realName]!!)
            } else {
                lastName += 1
                val newName = lastName.toString()
                names[realName] = newName
                Formula.Atom.Reference(newName)
            }
        } else atom
    }
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