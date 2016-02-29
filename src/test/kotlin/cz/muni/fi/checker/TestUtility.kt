package cz.muni.fi.checker

/**
 * Add here everything you find useful for testing, but can't quite see in the main package
 * (it's unsafe/slow/ugly or just too good for anyone to use!)
 */

fun <T> T.repeat(n: Int): List<T> = (1..n).map { this }

fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1;
    if ( b == 1)        return a;
    if ( b % 2 == 0)    return     pow ( a * a, b/2); //even a=(a^2)^b/2
    else                return a * pow ( a * a, b/2); //odd  a=a*(a^2)^b/2
}