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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Provides a N! (n-faculty) number of permutations for N elements.
 * Example: for 3 elements provides 6 permutations exactly as follows:
 * {0, 1, 2}
 * {0, 2, 1}
 * {1, 0, 2}
 * {1, 2, 0}
 * {2, 0, 1}
 * {2, 1, 0}
 */
public class PermutationEnumeration implements Enumeration<int[]> {
    private final int[] factors;
    private final int numElements;
    private final int maxNumPermutation;
    private int currentPermutation;

    /**
     * Ctor.
     *
     * @param numElements - number of elements in each permutation.
     */
    public PermutationEnumeration(int numElements) {
        if (numElements < 1) {
            throw new IllegalArgumentException("Invalid element number of 1");
        }
        this.numElements = numElements;
        this.factors = getFactors(numElements);
        this.maxNumPermutation = faculty(numElements);
    }

    public boolean hasMoreElements() {
        if (currentPermutation == maxNumPermutation) {
            return false;
        }
        return true;
    }

    public int[] nextElement() {
        if (currentPermutation == maxNumPermutation) {
            throw new NoSuchElementException();
        }
        int[] element = getPermutation(numElements, currentPermutation, factors);
        currentPermutation++;
        return element;
    }

    /**
     * Returns permutation.
     *
     * @param numElements - number of elements in each permutation
     * @param permutation - number of permutation to compute, between 0 and numElements!
     * @param factors     - factors for each index
     * @return permutation
     */
    protected static int[] getPermutation(int numElements, int permutation, int[] factors) {
        /*
        Example:
            numElements = 4
            permutation = 21
            factors = {6, 2, 1, 0}

        Init:   out {0, 1, 2, 3}

        21 / 6                      == index 3 -> result {3}, out {0, 1, 2}
        remainder 21 - 3 * 6        == 3
        3 / 2 = second number       == index 1 -> result {3, 1}, out {0, 2}
        remainder 3 - 1 * 2         == 1
                                    == index 1 -> result {3, 1, 2} out {0}
        */

        int[] result = new int[numElements];
        List<Integer> outList = new ArrayList<Integer>();
        for (int i = 0; i < numElements; i++) {
            outList.add(i);
        }
        int currentVal = permutation;

        for (int position = 0; position < numElements - 1; position++) {
            int factor = factors[position];
            int index = currentVal / factor;
            result[position] = outList.get(index);
            outList.remove(index);
            currentVal -= index * factor;
        }
        result[numElements - 1] = outList.get(0);

        return result;
    }

    /**
     * Returns factors for computing the permutation.
     *
     * @param numElements - number of factors to compute
     * @return factors list
     */
    protected static int[] getFactors(int numElements) {
        int[] facultyFactors = new int[numElements];

        for (int i = 0; i < numElements - 1; i++) {
            facultyFactors[i] = faculty(numElements - i - 1);
        }

        return facultyFactors;
    }

    /**
     * Computes faculty of N.
     *
     * @param num to compute faculty for
     * @return N!
     */
    protected static int faculty(int num) {
        if (num == 0) {
            return 0;
        }

        int fac = 1;
        for (int i = 1; i <= num; i++) {
            fac *= i;
        }
        return fac;
    }
}
