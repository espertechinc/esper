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

public class IntSeqKeySix implements IntSeqKey {
    private final int one;
    private final int two;
    private final int three;
    private final int four;
    private final int five;
    private final int six;

    public IntSeqKeySix(int one, int two, int three, int four, int five, int six) {
        this.one = one;
        this.two = two;
        this.three = three;
        this.four = four;
        this.five = five;
        this.six = six;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 7) {
            return false;
        }
        IntSeqKeyMany o = (IntSeqKeyMany) other;
        int[] array = o.getArray();
        return one == array[0] && two == array[1] && three == array[2] && four == array[3] && five == array[4] && six == array[5];
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyMany(new int[]{one, two, three, four, five, six, num});
    }

    public IntSeqKey removeFromEnd() {
        return new IntSeqKeyFive(one, two, three, four, five);
    }

    public int length() {
        return 6;
    }

    public int last() {
        return six;
    }

    public int[] asIntArray() {
        return new int[]{one, two, three, four, five, six};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeySix that = (IntSeqKeySix) o;

        if (one != that.one) return false;
        if (two != that.two) return false;
        if (three != that.three) return false;
        if (four != that.four) return false;
        if (five != that.five) return false;
        return six == that.six;
    }

    public int hashCode() {
        int result = one;
        result = 31 * result + two;
        result = 31 * result + three;
        result = 31 * result + four;
        result = 31 * result + five;
        result = 31 * result + six;
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

    public int getSix() {
        return six;
    }

    public static void write(IntSeqKeySix key, DataOutput output) throws IOException {
        output.writeInt(key.one);
        output.writeInt(key.two);
        output.writeInt(key.three);
        output.writeInt(key.four);
        output.writeInt(key.five);
        output.writeInt(key.six);
    }

    public static IntSeqKeySix read(DataInput input) throws IOException {
        return new IntSeqKeySix(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt());
    }
}
