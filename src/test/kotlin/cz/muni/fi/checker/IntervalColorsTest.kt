package cz.muni.fi.checker

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntervalIntersectionTest {

    @Test
    fun emptyEmpty() {
        assertTrue((Interval.EMPTY intersect Interval.EMPTY).isEmpty())
        assertTrue((Interval.EMPTY intersect Interval(12.4, -3.2)).isEmpty())
        assertTrue((Interval(-1.0, -2.0) intersect Interval(12.4, -3.2)).isEmpty())
    }

    @Test
    fun oneEmpty() {
        assertTrue((Interval(0.0, 1.0) intersect Interval.EMPTY).isEmpty())
        assertTrue((Interval(-3.2, -4.4) intersect Interval(-5.14, -3.3)).isEmpty())
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertEquals(r1, r1 intersect r1)
        val r2 = Interval(-4.3, 13.2)
        assertEquals(r2, r2 intersect r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertEquals(Interval(3.3, 4.5), r1 intersect r2)
        assertEquals(Interval(3.3, 3.8), r2 intersect r3)
        assertEquals(r3, r1 intersect r3)
    }

}

class IntervalMergeTest {

    @Test
    fun emptyEmpty() {
        assertTrue((Interval.EMPTY merge Interval.EMPTY)!!.isEmpty())
        assertTrue((Interval.EMPTY merge Interval(12.4, -3.2))!!.isEmpty())
        assertTrue((Interval(-1.0, -2.0) merge Interval(12.4, -3.2))!!.isEmpty())
    }

    @Test
    fun oneEmpty() {
        assertEquals(Interval(0.0, 1.0), Interval(0.0, 1.0) merge Interval.EMPTY)
        assertEquals(Interval(-5.14, -3.3), Interval(-3.2, -4.4) merge Interval(-5.14, -3.3))
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertEquals(r1, r1 merge r1)
        val r2 = Interval(-4.3, 13.2)
        assertEquals(r2, r2 merge r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertEquals(Interval(-3.2, 6.7), r1 merge r2)
        assertEquals(Interval(1.2, 6.7), r2 merge r3)
        assertEquals(r1, r1 merge r3)

        assertNull(Interval(0.0, 1.0) merge Interval(2.0,3.0))
        assertNull(Interval(2.0, 3.0) merge Interval(0.0,1.0))
        assertEquals(Interval(0.0,3.0), Interval(0.0,1.0) merge Interval(1.0, 3.0))
    }

}

class IntervalEnclosesTest {

    @Test
    fun emptyEmpty() {
        assertTrue(Interval.EMPTY encloses Interval.EMPTY)
        assertTrue(Interval.EMPTY encloses Interval(12.4, -3.2))
        assertTrue(Interval(-1.0, -2.0) encloses Interval(12.4, -3.2))
    }

    @Test
    fun oneEmpty() {
        assertTrue(Interval(0.0, 1.0) encloses Interval.EMPTY)
        assertTrue(Interval(-5.14, -3.3) encloses Interval(-3.2, -4.4))
        assertFalse(Interval.EMPTY encloses Interval(0.0, 1.0))
        assertFalse(Interval(-3.2, -4.4) encloses Interval(-5.14, -3.3))
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertTrue(r1 encloses r1)
        val r2 = Interval(-4.3, 13.2)
        assertTrue(r2 encloses r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertFalse(r1 encloses r2)
        assertFalse(r2 encloses r1)
        assertFalse(r2 encloses r3)
        assertFalse(r3 encloses r2)
        assertTrue(r1 encloses r3)
        assertFalse(r3 encloses r1)
    }

}

class IntervalMinusTest {

    @Test
    fun emptyEmpty() {
        assertTrue((Interval.EMPTY minus Interval.EMPTY).isEmpty())
        assertTrue((Interval.EMPTY minus Interval(1.0,-2.0)).isEmpty())
        assertTrue((Interval(-3.5, -4.4) minus Interval(1.0,-2.0)).isEmpty())
    }

    @Test
    fun oneEmpty() {
        val i = Interval(1.0,10.0)
        val fakeEmpty = Interval(1.0, -2.0)
        assertEquals(i, i - Interval.EMPTY)
        assertTrue((Interval.EMPTY - i).isEmpty())
        assertEquals(i, i - fakeEmpty)
        assertTrue((fakeEmpty - i).isEmpty())
    }

    @Test
    fun identity() {
        val i = Interval(1.0, 10.0)
        assertTrue((i - i).isEmpty())
    }

    @Test
    fun complex() {
        assertEquals(Interval(0.0, 5.0), Interval(0.0,10.0) - Interval(5.0, 10.0))
        assertEquals(Interval(5.0, 10.0), Interval(0.0,10.0) - Interval(0.0, 5.0))
        assertEquals(Interval(0.0, 5.0), Interval(0.0,7.0) - Interval(5.0, 10.0))
        assertEquals(Interval(5.0, 10.0), Interval(3.0,10.0) - Interval(0.0, 5.0))
        assertEquals(Interval(0.0, 5.0), Interval(0.0,5.0) - Interval(5.0, 10.0))
        assertEquals(Interval(5.0, 10.0), Interval(5.0,10.0) - Interval(0.0, 5.0))
    }

}

class IntervalColorsTest {

    @Test
    fun isEmptyTest() {
        assert(IntervalColors().isEmpty())
        assert(IntervalColors(0 to 10).isNotEmpty())
    }

    @Test
    fun plusTest() {
        assertEquals(IntervalColors(0 to 10), IntervalColors(0 to 10) + IntervalColors())
        assertEquals(IntervalColors(0 to 10), IntervalColors() + IntervalColors(0 to 10))
        assertEquals(IntervalColors(0 to 10, 20 to 30), IntervalColors(0 to 10) + IntervalColors(20 to 30))
        assertEquals(IntervalColors(0 to 10, 20 to 30), IntervalColors(20 to 30) + IntervalColors(0 to 10))
        assertEquals(IntervalColors(0 to 30), IntervalColors(0 to 10) + IntervalColors(20 to 30, 10 to 20))
        assertEquals(IntervalColors(0 to 30), IntervalColors(0 to 5, 5 to 15, 25 to 30) + IntervalColors(3 to 10, 10 to 14, 15 to 28))
    }

    @Test
    fun minusTest() {
        assertEquals(IntervalColors(0 to 2, 8 to 10), IntervalColors(0 to 10) - IntervalColors(2 to 8))
        assertEquals(IntervalColors(0 to 2, 4 to 6, 8 to 10), IntervalColors(0 to 10) - IntervalColors(2 to 4, 6 to 8))
    }

}

infix fun Int.to(b: Int) = Interval(this.toDouble(), b.toDouble())