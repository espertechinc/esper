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

public class IntSeqKeyFive implements IntSeqKey {
    private final int one;
    private final int two;
    private final int three;
    private final int four;
    private final int five;

    public IntSeqKeyFive(int one, int two, int three, int four, int five) {
        this.one = one;
        this.two = two;
        this.three = three;
        this.four = four;
        this.five = five;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 6) {
            return false;
        }
        IntSeqKeySix o = (IntSeqKeySix) other;
        return one == o.getOne() && two == o.getTwo() && three == o.getThree() && four == o.getFour() && five == o.getFive();
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeySix(one, two, three, four, five, num);
    }

    public IntSeqKey removeFromEnd() {
        return new IntSeqKeyFour(one, two, three, four);
    }

    public int length() {
        return 5;
    }

    public int last() {
        return five;
    }

    public int[] asIntArray() {
        return new int[]{one, two, three, four, five};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyFive that = (IntSeqKeyFive) o;

        if (one != that.one) return false;
        if (two != that.two) return false;
        if (three != that.three) return false;
        if (four != that.four) return false;
        return five == that.five;
    }

    public int hashCode() {
        int result = one;
        result = 31 * result + two;
        result = 31 * result + three;
        result = 31 * result + four;
        result = 31 * result + five;
        return result;
    }

    public int getOne() {
        return one;
    }

    public int getTwo() {
        return two;
    }

    public int getThree() {
        return three;
    }

    public int getFour() {
        return four;
    }

    public int getFive() {
        return five;
    }

    public static void write(IntSeqKeyFive key, DataOutput output) throws IOException {
        output.writeInt(key.one);
        output.writeInt(key.two);
        output.writeInt(key.three);
        output.writeInt(key.four);
        output.writeInt(key.five);
    }

    public static IntSeqKeyFive read(DataInput input) throws IOException {
        return new IntSeqKeyFive(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt());
    }
}
