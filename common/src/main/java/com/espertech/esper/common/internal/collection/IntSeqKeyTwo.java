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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IntSeqKeyTwo implements IntSeqKey {
    private final int one;
    private final int two;

    public IntSeqKeyTwo(int one, int two) {
        this.one = one;
        this.two = two;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 3) {
            return false;
        }
        IntSeqKeyThree o = (IntSeqKeyThree) other;
        return one == o.getOne() && two == o.getTwo();
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyThree(one, two, num);
    }

    public IntSeqKey removeFromEnd() {
        return new IntSeqKeyOne(one);
    }

    public int length() {
        return 2;
    }

    public int last() {
        return two;
    }

    public int[] asIntArray() {
        return new int[]{one, two};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyTwo that = (IntSeqKeyTwo) o;

        if (one != that.one) return false;
        return two == that.two;
    }

    public int hashCode() {
        int result = one;
        result = 31 * result + two;
        return result;
    }

    public int getOne() {
        return one;
    }

    public int getTwo() {
        return two;
    }

    public static void write(IntSeqKeyTwo key, DataOutput output) throws IOException {
        output.writeInt(key.one);
        output.writeInt(key.two);
    }

    public static IntSeqKeyTwo read(DataInput input) throws IOException {
        return new IntSeqKeyTwo(input.readInt(), input.readInt());
    }

    /* Comment-in for testing memory and performance
    public static void main(String[] args) {
        Map<IntArrayKey, Boolean> map = new HashMap<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 1000000; j++) {
                map.put(new IntArrayKey(new int[i]), true);
            }
            System.out.println("At " + i);
        }
        long delta = System.currentTimeMillis() - start;
        System.out.println(delta);
    }
    */
}
