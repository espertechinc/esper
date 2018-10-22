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

public class IntSeqKeyFour implements IntSeqKey {
    private final int one;
    private final int two;
    private final int three;
    private final int four;

    public IntSeqKeyFour(int one, int two, int three, int four) {
        this.one = one;
        this.two = two;
        this.three = three;
        this.four = four;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 5) {
            return false;
        }
        IntSeqKeyFive o = (IntSeqKeyFive) other;
        return one == o.getOne() && two == o.getTwo() && three == o.getThree() && four == o.getFour();
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyFive(one, two, three, four, num);
    }

    public IntSeqKey removeFromEnd() {
        return new IntSeqKeyThree(one, two, three);
    }

    public int length() {
        return 4;
    }

    public int last() {
        return four;
    }

    public int[] asIntArray() {
        return new int[]{one, two, three, four};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyFour that = (IntSeqKeyFour) o;

        if (one != that.one) return false;
        if (two != that.two) return false;
        if (three != that.three) return false;
        return four == that.four;
    }

    public int hashCode() {
        int result = one;
        result = 31 * result + two;
        result = 31 * result + three;
        result = 31 * result + four;
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

    public static void write(IntSeqKeyFour key, DataOutput output) throws IOException {
        output.writeInt(key.one);
        output.writeInt(key.two);
        output.writeInt(key.three);
        output.writeInt(key.four);
    }

    public static IntSeqKeyFour read(DataInput input) throws IOException {
        return new IntSeqKeyFour(input.readInt(), input.readInt(), input.readInt(), input.readInt());
    }
}
