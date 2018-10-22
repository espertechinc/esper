/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.collection;

import junit.framework.TestCase;

import java.io.*;
import java.util.Arrays;

public class TestIntSeqKey extends TestCase {
    private IntSeqKeyRoot zero = IntSeqKeyRoot.INSTANCE;
    private IntSeqKeyOne one = new IntSeqKeyOne(1);
    private IntSeqKeyTwo two = new IntSeqKeyTwo(2, 3);
    private IntSeqKeyThree three = new IntSeqKeyThree(4, 5, 6);
    private IntSeqKeyFour four = new IntSeqKeyFour(7, 8, 9, 10);
    private IntSeqKeyFive five = new IntSeqKeyFive(11, 12, 13, 14, 15);
    private IntSeqKeySix six = new IntSeqKeySix(16, 17, 18, 19, 20, 21);
    private IntSeqKeyMany seven = new IntSeqKeyMany(new int[]{22, 23, 24, 25, 26, 27, 28});
    private IntSeqKeyMany eight = new IntSeqKeyMany(new int[]{29, 30, 31, 32, 33, 34, 35, 36});

    public void testLast() {
        assertEquals(36, eight.last());
        assertEquals(28, seven.last());
        assertEquals(21, six.last());
        assertEquals(15, five.last());
        assertEquals(10, four.last());
        assertEquals(6, three.last());
        assertEquals(3, two.last());
        assertEquals(1, one.last());

        try {
            zero.last();
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    public void testLength() {
        assertEquals(8, eight.length());
        assertEquals(7, seven.length());
        assertEquals(6, six.length());
        assertEquals(5, five.length());
        assertEquals(4, four.length());
        assertEquals(3, three.length());
        assertEquals(2, two.length());
        assertEquals(1, one.length());
        assertEquals(0, zero.length());
    }

    public void testAsIntArray() {
        assertArray(eight.asIntArray(), 29, 30, 31, 32, 33, 34, 35, 36);
        assertArray(seven.asIntArray(), 22, 23, 24, 25, 26, 27, 28);
        assertArray(six.asIntArray(), 16, 17, 18, 19, 20, 21);
        assertArray(five.asIntArray(), 11, 12, 13, 14, 15);
        assertArray(four.asIntArray(), 7, 8, 9, 10);
        assertArray(three.asIntArray(), 4, 5, 6);
        assertArray(two.asIntArray(), 2, 3);
        assertArray(one.asIntArray(), 1);
        assertArray(zero.asIntArray());
    }

    public void testAddToEnd() {
        assertArray(eight.addToEnd(99).asIntArray(), 29, 30, 31, 32, 33, 34, 35, 36, 99);
        assertArray(seven.addToEnd(99).asIntArray(), 22, 23, 24, 25, 26, 27, 28, 99);
        assertArray(six.addToEnd(99).asIntArray(), 16, 17, 18, 19, 20, 21, 99);
        assertArray(five.addToEnd(99).asIntArray(), 11, 12, 13, 14, 15, 99);
        assertArray(four.addToEnd(99).asIntArray(), 7, 8, 9, 10, 99);
        assertArray(three.addToEnd(99).asIntArray(), 4, 5, 6, 99);
        assertArray(two.addToEnd(99).asIntArray(), 2, 3, 99);
        assertArray(one.addToEnd(99).asIntArray(), 1, 99);
        assertArray(zero.addToEnd(99).asIntArray(), 99);
    }

    public void testRemoveFromEnd() {
        assertArray(eight.removeFromEnd().asIntArray(), 29, 30, 31, 32, 33, 34, 35);
        assertArray(seven.removeFromEnd().asIntArray(), 22, 23, 24, 25, 26, 27);
        assertArray(six.removeFromEnd().asIntArray(), 16, 17, 18, 19, 20);
        assertArray(five.removeFromEnd().asIntArray(), 11, 12, 13, 14);
        assertArray(four.removeFromEnd().asIntArray(), 7, 8, 9);
        assertArray(three.removeFromEnd().asIntArray(), 4, 5);
        assertArray(two.removeFromEnd().asIntArray(), 2);
        assertArray(one.removeFromEnd().asIntArray());

        try {
            zero.removeFromEnd();
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    public void testIsParent() {
        assertTrue(zero.isParentTo(one));
        assertTrue(zero.isParentTo(new IntSeqKeyOne(99)));
        assertFalse(zero.isParentTo(zero));
        assertFalse(zero.isParentTo(two));

        assertTrue(one.isParentTo(one.addToEnd(99)));
        assertTrue(one.isParentTo(new IntSeqKeyTwo(1, 2)));
        assertFalse(one.isParentTo(new IntSeqKeyTwo(2, 2)));
        assertFalse(one.isParentTo(zero));
        assertFalse(one.isParentTo(one));
        assertFalse(one.isParentTo(two));
        assertFalse(one.isParentTo(new IntSeqKeyThree(1, 2, 3)));

        assertTrue(two.isParentTo(two.addToEnd(99)));
        assertTrue(two.isParentTo(new IntSeqKeyThree(2, 3, 5)));
        assertFalse(two.isParentTo(new IntSeqKeyThree(1, 3, 5)));
        assertFalse(two.isParentTo(new IntSeqKeyThree(2, 4, 5)));
        assertFalse(two.isParentTo(zero));
        assertFalse(two.isParentTo(one));
        assertFalse(two.isParentTo(two));
        assertFalse(two.isParentTo(three));
        assertFalse(two.isParentTo(new IntSeqKeyFour(2, 3, 4, 5)));

        assertTrue(three.isParentTo(three.addToEnd(99)));
        assertTrue(three.isParentTo(new IntSeqKeyFour(4, 5, 6, 0)));
        assertFalse(three.isParentTo(new IntSeqKeyFour(3, 5, 6, 0)));
        assertFalse(three.isParentTo(new IntSeqKeyFour(4, 4, 6, 0)));
        assertFalse(three.isParentTo(new IntSeqKeyFour(4, 5, 5, 0)));
        assertFalse(three.isParentTo(zero));
        assertFalse(three.isParentTo(one));
        assertFalse(three.isParentTo(two));
        assertFalse(three.isParentTo(four));
        assertFalse(three.isParentTo(new IntSeqKeyFive(4, 5, 6, 7, 5)));

        assertTrue(four.isParentTo(four.addToEnd(99)));
        assertTrue(four.isParentTo(new IntSeqKeyFive(7, 8, 9, 10, 0)));
        assertFalse(four.isParentTo(new IntSeqKeyFive(6, 8, 9, 10, 0)));
        assertFalse(four.isParentTo(new IntSeqKeyFive(7, 7, 9, 10, 0)));
        assertFalse(four.isParentTo(new IntSeqKeyFive(7, 8, 8, 10, 0)));
        assertFalse(four.isParentTo(new IntSeqKeyFive(7, 8, 9, 9, 0)));
        assertFalse(four.isParentTo(zero));
        assertFalse(four.isParentTo(one));
        assertFalse(four.isParentTo(two));
        assertFalse(four.isParentTo(four));
        assertFalse(four.isParentTo(five));
        assertFalse(four.isParentTo(six));
        assertFalse(four.isParentTo(new IntSeqKeySix(7, 8, 9, 10, 11, 12)));

        assertTrue(five.isParentTo(five.addToEnd(99)));
        assertTrue(five.isParentTo(new IntSeqKeySix(11, 12, 13, 14, 15, 0)));
        assertFalse(five.isParentTo(new IntSeqKeySix(0, 12, 13, 14, 15, 0)));
        assertFalse(five.isParentTo(new IntSeqKeySix(11, 0, 13, 14, 15, 0)));
        assertFalse(five.isParentTo(new IntSeqKeySix(11, 12, 0, 14, 15, 0)));
        assertFalse(five.isParentTo(new IntSeqKeySix(11, 12, 13, 0, 15, 0)));
        assertFalse(five.isParentTo(new IntSeqKeySix(11, 12, 13, 14, 0, 0)));
        assertFalse(five.isParentTo(five));
        assertFalse(five.isParentTo(six));
        assertFalse(five.isParentTo(seven));
        assertFalse(five.isParentTo(new IntSeqKeyMany(new int[]{11, 12, 13, 14, 15, 0, 0})));

        assertTrue(six.isParentTo(six.addToEnd(99)));
        assertTrue(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 18, 19, 20, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{0, 17, 18, 19, 20, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 0, 18, 19, 20, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 0, 19, 20, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 18, 0, 20, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 18, 19, 0, 21, 0})));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 18, 19, 20, 0, 0})));
        assertFalse(six.isParentTo(five));
        assertFalse(six.isParentTo(six));
        assertFalse(six.isParentTo(seven));
        assertFalse(six.isParentTo(eight));
        assertFalse(six.isParentTo(new IntSeqKeyMany(new int[]{16, 17, 18, 19, 20, 21, 0, 0})));

        assertTrue(seven.isParentTo(seven.addToEnd(99)));
        assertTrue(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 25, 26, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{0, 23, 24, 25, 26, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 0, 24, 25, 26, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 0, 25, 26, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 0, 26, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 25, 0, 27, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 25, 26, 0, 28, 0})));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 25, 26, 27, 0, 0})));
        assertFalse(seven.isParentTo(five));
        assertFalse(seven.isParentTo(six));
        assertFalse(seven.isParentTo(seven));
        assertFalse(seven.isParentTo(eight));
        assertFalse(seven.isParentTo(new IntSeqKeyMany(new int[]{22, 23, 24, 25, 26, 27, 28, 0, 0})));
    }

    public void testReadWrite() {
        Writer<IntSeqKeyOne> writerOne = IntSeqKeyOne::write;
        assertReadWrite(one, writerOne, IntSeqKeyOne::read);

        Writer<IntSeqKeyTwo> writerTwo = IntSeqKeyTwo::write;
        assertReadWrite(two, writerTwo, IntSeqKeyTwo::read);

        Writer<IntSeqKeyThree> writerThree = IntSeqKeyThree::write;
        assertReadWrite(three, writerThree, IntSeqKeyThree::read);

        Writer<IntSeqKeyFour> writerFour = IntSeqKeyFour::write;
        assertReadWrite(four, writerFour, IntSeqKeyFour::read);

        Writer<IntSeqKeyFive> writerFive = IntSeqKeyFive::write;
        assertReadWrite(five, writerFive, IntSeqKeyFive::read);

        Writer<IntSeqKeySix> writerSix = IntSeqKeySix::write;
        assertReadWrite(six, writerSix, IntSeqKeySix::read);

        Writer<IntSeqKeyMany> writerMany = IntSeqKeyMany::write;
        assertReadWrite(seven, writerMany, IntSeqKeyMany::read);
        assertReadWrite(eight, writerMany, IntSeqKeyMany::read);
    }

    private void assertReadWrite(IntSeqKey key, Writer write, Reader read) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            write.write(key, dos);
            dos.close();
            baos.close();
        } catch (IOException ex) {
            fail();
        }

        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        IntSeqKey deserialized;
        try {
            deserialized = (IntSeqKey) read.read(dis);
            assertEquals(key, deserialized);
        } catch (IOException ex) {
            fail();
        }
    }

    private void assertArray(int[] result, int... expected) {
        assertTrue(Arrays.equals(expected, result));
    }

    @FunctionalInterface
    public interface Writer<T> {
        void write(T t, DataOutput out) throws IOException;
    }

    @FunctionalInterface
    public interface Reader<T> {
        T read(DataInput input) throws IOException;
    }
}
