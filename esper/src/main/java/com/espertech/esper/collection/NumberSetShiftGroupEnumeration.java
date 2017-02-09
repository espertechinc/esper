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
package com.espertech.esper.collection;

import java.util.*;

/**
 * Enumeration that first returns a round-shift-left of all numbers
 * and when that is exhausted it returns number sets using grouped algo until exhausted.
 */
public class NumberSetShiftGroupEnumeration implements Enumeration<int[]> {
    private final int[] numberSet;
    private boolean isShiftComplete;
    private int shiftCount;
    private PermutationEnumeration permutationEnumeration;
    private Map<Integer, List<Integer>> buckets;

    /**
     * Ctor.
     *
     * @param numberSet - set of integer numbers to permutate and provide each combination.
     */
    public NumberSetShiftGroupEnumeration(int[] numberSet) {
        if (numberSet.length < 6) {
            throw new IllegalArgumentException("Only supported for at least 6-number sets");
        }
        this.numberSet = numberSet;
    }

    public boolean hasMoreElements() {
        if (!isShiftComplete) {
            return true;
        }
        initPermutation();
        return permutationEnumeration.hasMoreElements();
    }

    public int[] nextElement() {
        if (!isShiftComplete) {
            int[] result = new int[numberSet.length];
            int count = shiftCount++;
            for (int i = 0; i < numberSet.length; i++) {
                int index = count + i;
                if (index >= numberSet.length) {
                    index -= numberSet.length;
                }
                result[i] = numberSet[index];
            }
            if (shiftCount == numberSet.length) {
                isShiftComplete = true;
            }
            return result;
        }
        initPermutation();
        return translate(permutationEnumeration.nextElement());
    }

    private void initPermutation() {
        if (permutationEnumeration != null) {
            return;
        }

        // simply always make 4 buckets
        buckets = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < numberSet.length; i++) {
            int bucketNum = i % 4;
            List<Integer> bucket = buckets.get(bucketNum);
            if (bucket == null) {
                bucket = new ArrayList<Integer>();
                buckets.put(bucketNum, bucket);
            }
            bucket.add(numberSet[i]);
        }

        permutationEnumeration = new PermutationEnumeration(4);
        permutationEnumeration.nextElement();   // we throw the first one away, it is the same as a shift result
    }

    private int[] translate(int[] bucketsPermuted) {
        int[] result = new int[numberSet.length];
        int count = 0;
        for (int i = 0; i < bucketsPermuted.length; i++) {
            List<Integer> bucket = buckets.get(bucketsPermuted[i]);
            for (int j : bucket) {
                result[count++] = j;
            }
        }
        return result;
    }


}