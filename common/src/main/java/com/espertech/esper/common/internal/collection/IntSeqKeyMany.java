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

import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class IntSeqKeyMany implements IntSeqKey {
    private final int[] array;

    public IntSeqKeyMany(int[] array) {
        if (array.length < 7) {
            throw new IllegalArgumentException("Array size less than 7");
        }
        this.array = array;
    }

    public boolean isParentTo(IntSeqKey other) {
        if (!(other instanceof IntSeqKeyMany)) {
            return false;
        }
        return IntArrayUtil.compareParentKey(((IntSeqKeyMany) other).array, array);
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyMany(IntArrayUtil.append(array, num));
    }

    public IntSeqKey removeFromEnd() {
        if (array.length > 7) {
            return new IntSeqKeyMany(IntArrayUtil.getParentPath(array));
        }
        return new IntSeqKeySix(array[0], array[1], array[2], array[3], array[4], array[5]);
    }

    public int length() {
        return array.length;
    }

    public int last() {
        return array[array.length - 1];
    }

    public int[] asIntArray() {
        return array;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntSeqKeyMany that = (IntSeqKeyMany) o;

        return Arrays.equals(array, that.array);
    }

    public int hashCode() {
        return Arrays.hashCode(array);
    }

    public int[] getArray() {
        return array;
    }

    public static void write(IntSeqKeyMany key, DataOutput output) throws IOException {
        int[] array = key.getArray();
        output.writeInt(array.length);
        for (int i : array) {
            output.writeInt(i);
        }
    }

    public static IntSeqKeyMany read(DataInput input) throws IOException {
        int size = input.readInt();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = input.readInt();
        }
        return new IntSeqKeyMany(array);
    }
}
