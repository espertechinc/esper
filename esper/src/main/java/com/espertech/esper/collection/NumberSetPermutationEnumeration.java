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

import java.util.Enumeration;

/**
 * Based on the {@link PermutationEnumeration} this enumeration provides, among a set of supplied integer
 * values, all permutations of order these values can come in, ie.
 * Example: {0, 2, 3} results in 6 enumeration values ending in {3, 2, 0}.
 */
public class NumberSetPermutationEnumeration implements Enumeration<int[]> {
    private final int[] numberSet;
    private final PermutationEnumeration permutationEnumeration;

    /**
     * Ctor.
     *
     * @param numberSet - set of integer numbers to permutate and provide each combination.
     */
    public NumberSetPermutationEnumeration(int[] numberSet) {
        this.numberSet = numberSet;
        permutationEnumeration = new PermutationEnumeration(numberSet.length);
    }

    public boolean hasMoreElements() {
        return permutationEnumeration.hasMoreElements();
    }

    public int[] nextElement() {
        int[] permutation = permutationEnumeration.nextElement();

        int[] result = new int[numberSet.length];
        for (int i = 0; i < numberSet.length; i++) {
            result[i] = numberSet[permutation[i]];
        }

        return result;
    }

}
