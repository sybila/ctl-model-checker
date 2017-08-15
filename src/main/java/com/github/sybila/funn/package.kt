package com.github.sybila.funn

import com.github.sybila.fold
import com.github.sybila.huctl.*
import com.github.sybila.huctl.dsl.toReference
import com.github.sybila.map

// compute the set of quantified names present in this formula (bind, forall and exists are quantifiers)
internal val Formula.quantifiedNames: Set<String>
    get() = this.fold(atom = {
        emptySet()
    }, unary = {
        if (this is Formula.Bind) it + setOf(this.name) else it
    }, binary = { l, r ->
        val n = when (this) {
            is Formula.Exists -> setOf(this.name)
            is Formula.ForAll -> setOf(this.name)
            else -> emptySet()
        }
        l + r + n
    })

// compute a string representation of this formula which has the names of variables transformed into a
// canonical format (so for example, (bind x: EX x) and (bind z: EX z) are considered equal)
internal val Formula.canonicalKey: String
    get() {
        val nameMapping = this.quantifiedNames.sorted().mapIndexed { i, n -> n to "_var$i" }.toMap()
        return this.map(atom = {
            if (this is Formula.Reference && this.name in nameMapping) {
                nameMapping[this.name]!!.toReference()
            } else this
        }, unary = { inner ->
            when {
                this is Formula.Bind -> bind(nameMapping[this.name]!!, inner)
                this is Formula.At && this.name in nameMapping -> at(nameMapping[this.name]!!, inner)
                else -> this.copy(inner)
            }
        }, binary = { l, r ->
            when {
                this is Formula.Exists -> exists(nameMapping[this.name]!!, l, r)
                this is Formula.ForAll -> forall(nameMapping[this.name]!!, l, r)
                else -> this.copy(l, r)
            }
        }).toString()
    }