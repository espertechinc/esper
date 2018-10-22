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
package com.espertech.esper.common.internal.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntArrayUtil {

    public static final int[] EMPTY_ARRAY = new int[0];

    public static int[] getParentPath(int[] path) {
        int[] parent = new int[path.length - 1];
        for (int i = 0; i < path.length - 1; i++) {
            parent[i] = path[i];
        }
        return parent;
    }

    public static void writeOptionalArray(int[] ints, DataOutput output) throws IOException {
        if (ints == null) {
            output.writeBoolean(false);
            return;
        }
        output.writeBoolean(true);
        writeArray(ints, output);
    }

    public static void writeArray(int[] ints, DataOutput output) throws IOException {
        output.writeInt(ints.length);
        for (int value : ints) {
            output.writeInt(value);
        }
    }

    public static int[] readOptionalArray(DataInput input) throws IOException {
        boolean hasValue = input.readBoolean();
        if (!hasValue) {
            return null;
        }
        return readArray(input);
    }

    public static int[] readArray(DataInput input) throws IOException {
        int size = input.readInt();
        int[] stamps = new int[size];
        for (int i = 0; i < size; i++) {
            stamps[i] = input.readInt();
        }
        return stamps;
    }

    public static Iterator<Integer> toIterator(int[] array) {
        return new IntArrayUtilIntIterator(array);
    }

    public static int[] append(int[] array, int value) {
        int[] newArray = new int[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = value;
        return newArray;
    }

    public static int[] copy(int[] src) {
        int[] copy = new int[src.length];
        System.arraycopy(src, 0, copy, 0, src.length);
        return copy;
    }

    public static int[] toArray(Collection<Integer> collection) {
        int[] values = new int[collection.size()];
        int index = 0;
        for (Integer value : collection) {
            values[index++] = value;
        }
        return values;
    }

    public static Integer[] toBoxedArray(Collection<Integer> collection) {
        Integer[] values = new Integer[collection.size()];
        int index = 0;
        for (Integer value : collection) {
            values[index++] = value;
        }
        return values;
    }

    public static boolean compareParentKey(int[] key, int[] parentKey) {
        if (key.length - 1 != parentKey.length) {
            return false;
        }
        for (int i = 0; i < parentKey.length; i++) {
            if (key[i] != parentKey[i]) {
                return false;
            }
        }
        return true;
    }

    private static class IntArrayUtilIntIterator implements Iterator<Integer> {
        private final int[] array;
        private int position;

        public IntArrayUtilIntIterator(int[] array) {
            this.array = array;
        }

        public boolean hasNext() {
            return position < array.length;
        }

        public Integer next() {
            if (array.length <= position) {
                throw new NoSuchElementException();
            }
            int value = array[position];
            position++;
            return value;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
