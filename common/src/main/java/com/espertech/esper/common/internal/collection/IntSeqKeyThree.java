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

public class IntSeqKeyThree implements IntSeqKey {
    private final int one;
    private final int two;
    private final int three;

    public IntSeqKeyThree(int one, int two, int three) {
        this.one = one;
        this.two = two;
        this.three = three;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 4) {
            return false;
        }
        IntSeqKeyFour o = (IntSeqKeyFour) other;
        return one == o.getOne() && two == o.getTwo() && three == o.getThree();
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyFour(one, two, three, num);
    }

    public IntSeqKey removeFromEnd() {
        return new IntSeqKeyTwo(one, two);
    }

    public int length() {
        return 3;
    }

    public int last() {
        return three;
    }

    public int[] asIntArray() {
        return new int[]{one, two, three};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyThree that = (IntSeqKeyThree) o;

        if (one != that.one) return false;
        if (two != that.two) return false;
        return three == that.three;
    }

    public int hashCode() {
        int result = one;
        result = 31 * result + two;
        result = 31 * result + three;
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

    public static void write(IntSeqKeyThree key, DataOutput output) throws IOException {
        output.writeInt(key.one);
        output.writeInt(key.two);
        output.writeInt(key.three);
    }

    public static IntSeqKeyThree read(DataInput input) throws IOException {
        return new IntSeqKeyThree(input.readInt(), input.readInt(), input.readInt());
    }
}
