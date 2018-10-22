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

public class IntSeqKeyOne implements IntSeqKey {
    private int one;

    public IntSeqKeyOne(int one) {
        this.one = one;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (other.length() != 2) {
            return false;
        }
        IntSeqKeyTwo o = (IntSeqKeyTwo) other;
        return one == o.getOne();
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyTwo(one, num);
    }

    public IntSeqKey removeFromEnd() {
        return IntSeqKeyRoot.INSTANCE;
    }

    public int length() {
        return 1;
    }

    public int getOne() {
        return one;
    }

    public int last() {
        return one;
    }

    public int[] asIntArray() {
        return new int[]{one};
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyOne that = (IntSeqKeyOne) o;

        return one == that.one;
    }

    public int hashCode() {
        return one;
    }

    public static void write(IntSeqKeyOne key, DataOutput output) throws IOException {
        output.writeInt(key.one);
    }

    public static IntSeqKeyOne read(DataInput input) throws IOException {
        return new IntSeqKeyOne(input.readInt());
    }
}
