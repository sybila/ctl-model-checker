package com.github.sybila.checker
/*
/**
 * This is a utility file for all the goodies you won't find anywhere else.
 * Remember, with great power comes great responsibility.
 *
 * If something in here exceeds ~100 lines or is connected strictly to something else,
 * refactor it away!
 */

/**
 * Helper method to reify a generic class at compile time (Since you can't write K<A,B>::class)
 */
inline fun <reified T: Any> genericClass(): Class<T> = T::class.java
*/