package com.github.sybila.checker

import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.mapLeafs
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

