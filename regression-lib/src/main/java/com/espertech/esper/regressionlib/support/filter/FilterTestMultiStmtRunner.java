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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.internal.collection.PermutationEnumeration;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.junit.Assert;

import java.util.*;

public class FilterTestMultiStmtRunner {

    public static List<? extends RegressionExecution> computePermutations(Class originator, PermutationSpec permutationSpec, List<FilterTestMultiStmtPermutable> cases) {

        // For each permutable test
        List<FilterTestMultiStmtExecution> executions = new ArrayList<>();
        for (FilterTestMultiStmtPermutable permutableCase : cases) {
            List<FilterTestMultiStmtExecution> execs = computePermutationsCase(originator, permutationSpec, permutableCase);
            executions.addAll(execs);
        }
        return executions;
    }

    private static List<FilterTestMultiStmtExecution> computePermutationsCase(Class originator, PermutationSpec permutationSpec, FilterTestMultiStmtPermutable permutableCase) {

        if (!permutationSpec.isAll()) {
            return Collections.singletonList(caseOf(originator, permutationSpec.getSpecific(), permutableCase));
        } else {
            // determine that filters is different
            Set<String> filtersUnique = new HashSet<String>(Arrays.asList(permutableCase.getFilters()));
            if (filtersUnique.size() == 1 && permutableCase.getFilters().length > 1) {
                Assert.fail("Filters are all the same, specify a single permutation instead");
            }

            List<FilterTestMultiStmtExecution> executions = new ArrayList<>();
            PermutationEnumeration permutationEnumeration = new PermutationEnumeration(permutableCase.getFilters().length);
            while (permutationEnumeration.hasMoreElements()) {
                int[] permutation = permutationEnumeration.nextElement();
                executions.add(caseOf(originator, permutation, permutableCase));
            }
            return executions;
        }
    }

    private static FilterTestMultiStmtExecution caseOf(Class originator, int[] permutation, FilterTestMultiStmtPermutable permutableCase) {
        FilterTestMultiStmtCase theCase = computePermutation(permutableCase, permutation);
        return new FilterTestMultiStmtExecution(originator, theCase);
    }

    private static FilterTestMultiStmtCase computePermutation(FilterTestMultiStmtPermutable permutableCase, int[] permutation) {

        // permute filters
        String[] filtersPermuted = new String[permutableCase.getFilters().length];
        for (int i = 0; i < permutation.length; i++) {
            filtersPermuted[i] = permutableCase.getFilters()[permutation[i]];
        }

        // permute expected values
        List<FilterTestMultiStmtAssertItem> itemsPermuted = new ArrayList<FilterTestMultiStmtAssertItem>();
        for (FilterTestMultiStmtAssertItem items : permutableCase.getItems()) {
            boolean[] expectedPermuted = new boolean[items.getExpectedPerStmt().length];
            for (int i = 0; i < permutation.length; i++) {
                expectedPermuted[i] = items.getExpectedPerStmt()[permutation[i]];
            }
            itemsPermuted.add(new FilterTestMultiStmtAssertItem(items.getBean(), expectedPermuted));
        }

        // find stats for this permutation
        FilterTestMultiStmtAssertStats statsPermuted = null;
        for (FilterTestMultiStmtAssertStats stats : permutableCase.getStatsPerPermutation()) {
            if (Arrays.equals(stats.getPermutation(), permutation)) {
                if (statsPermuted != null) {
                    throw new IllegalStateException("Permutation " + Arrays.toString(permutation) + " exists twice");
                }
                statsPermuted = stats;
            }
        }
        if (statsPermuted == null) {
            throw new IllegalStateException("Failed to find stats for permutation " + Arrays.toString(permutation));
        }

        return new FilterTestMultiStmtCase(filtersPermuted, statsPermuted.getStats(), itemsPermuted);
    }
}
