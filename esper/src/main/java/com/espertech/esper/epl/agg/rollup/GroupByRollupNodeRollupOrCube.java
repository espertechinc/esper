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
package com.espertech.esper.epl.agg.rollup;

import com.espertech.esper.collection.NumberAscCombinationEnumeration;
import com.espertech.esper.util.CollectionUtil;

import java.util.*;

public class GroupByRollupNodeRollupOrCube extends GroupByRollupNodeBase {

    private final boolean cube;

    public GroupByRollupNodeRollupOrCube(boolean cube) {
        this.cube = cube;
    }

    public List<int[]> evaluate(GroupByRollupEvalContext context) throws GroupByRollupDuplicateException {
        int[][] childIndexes = evaluateChildNodes(context);

        // find duplicate entries among child expressions
        for (int i = 0; i < childIndexes.length; i++) {
            for (int j = i + 1; j < childIndexes.length; j++) {
                validateCompare(childIndexes[i], childIndexes[j]);
            }
        }

        List<int[]> rollup;
        if (cube) {
            rollup = handleCube(childIndexes);
        } else {
            rollup = handleRollup(childIndexes);
        }
        rollup.add(new int[0]);
        return rollup;
    }

    private static void validateCompare(int[] one, int[] other) throws GroupByRollupDuplicateException {
        if (Arrays.equals(one, other)) {
            throw new GroupByRollupDuplicateException(one);
        }
    }

    private List<int[]> handleCube(int[][] childIndexes) {
        List<int[]> enumerationSorted = new ArrayList<int[]>();
        int size = getChildNodes().size();
        NumberAscCombinationEnumeration e = new NumberAscCombinationEnumeration(size);
        for (; e.hasMoreElements(); ) {
            enumerationSorted.add(e.nextElement());
        }
        Collections.sort(enumerationSorted, new Comparator<int[]>() {
            public int compare(int[] o1, int[] o2) {
                int shared = Math.min(o1.length, o2.length);
                for (int i = 0; i < shared; i++) {
                    if (o1[i] < o2[i]) {
                        return -1;
                    }
                    if (o1[i] > o2[i]) {
                        return 1;
                    }
                }
                if (o1.length > o2.length) {
                    return -1;
                }
                if (o1.length < o2.length) {
                    return 1;
                }
                return 0;
            }
        });

        List<int[]> rollup = new ArrayList<int[]>(enumerationSorted.size() + 1);
        Set<Integer> keys = new LinkedHashSet<Integer>();
        for (int[] item : enumerationSorted) {
            keys.clear();
            for (int index : item) {
                int[] childIndex = childIndexes[index];
                for (int childIndexItem : childIndex) {
                    keys.add(childIndexItem);
                }
            }
            rollup.add(CollectionUtil.intArray(keys));
        }
        return rollup;
    }

    private List<int[]> handleRollup(int[][] childIndexes) {

        int size = getChildNodes().size();
        List<int[]> rollup = new ArrayList<int[]>(size + 1);
        Set<Integer> keyset = new LinkedHashSet<Integer>();

        for (int i = 0; i < size; i++) {
            keyset.clear();

            for (int j = 0; j < size - i; j++) {
                int[] childIndex = childIndexes[j];
                for (int aChildIndex : childIndex) {
                    keyset.add(aChildIndex);
                }
            }
            rollup.add(CollectionUtil.intArray(keyset));
        }
        return rollup;
    }

    private int[][] evaluateChildNodes(GroupByRollupEvalContext context) throws GroupByRollupDuplicateException {
        int size = getChildNodes().size();
        int[][] childIndexes = new int[size][];
        for (int i = 0; i < size; i++) {
            List<int[]> childIndex = getChildNodes().get(i).evaluate(context);
            if (childIndex.size() != 1) {
                throw new IllegalStateException();
            }
            childIndexes[i] = childIndex.get(0);
        }
        return childIndexes;
    }
}
