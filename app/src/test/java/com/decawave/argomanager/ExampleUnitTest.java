package com.decawave.argomanager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testRunnableMethodReference() {
        acceptRunnable(this::methodA);
        acceptRunnable(this::methodA);
        acceptRunnable(this::methodA);
        acceptRunnable(this::methodB);
        acceptRunnable(this::methodB);
    }

    private void methodA() {
        System.out.println("this is method A");
    }

    private void methodB() {
        System.out.println("this is method B");
    }

    private void acceptRunnable(Runnable runnable) {
        System.out.println("accepted runnable: " + runnable);
        runnable.run();
    }

    @Test
    public void testShortToLong() {
        Long l = 123456789l;
        short s = (short) (l & 0x00000000FFFF);
        //
        assertTrue(l.shortValue() == s);
        assertFalse(l.longValue() == s);
        assertFalse(l.shortValue() == l);
        // different values
        Long l2 = 32000l;
        short s2 = (short) 32000;
        // compare
        assertTrue(l2.shortValue() == s2);
        assertTrue(l2.longValue() == s2);
        assertTrue(l2.shortValue() == l2);
        //

    }

}