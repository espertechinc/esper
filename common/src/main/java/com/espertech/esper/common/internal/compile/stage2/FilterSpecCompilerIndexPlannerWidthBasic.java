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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.collection.CombinationEnumeration;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.ops.ExprOrNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.decomposePopulateConsolidate;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlannerHelper.makeRemainingNode;

public class FilterSpecCompilerIndexPlannerWidthBasic {
    protected static FilterSpecPlanForge planRemainingNodesBasic(FilterSpecParaForgeMap overallExpressions, FilterSpecCompilerArgs args, int filterServiceMaxFilterWidth)
        throws ExprValidationException {
        List<ExprNode> unassigned = overallExpressions.getUnassignedExpressions();
        List<ExprOrNode> orNodes = new ArrayList<>(unassigned.size());

        for (ExprNode node : unassigned) {
            if (node instanceof ExprOrNode) {
                orNodes.add((ExprOrNode) node);
            }
        }

        FilterSpecParaForgeMap expressionsWithoutOr = new FilterSpecParaForgeMap();
        expressionsWithoutOr.add(overallExpressions);

        // first dimension: or-node index
        // second dimension: or child node index
        FilterSpecParaForgeMap[][] orNodesMaps = new FilterSpecParaForgeMap[orNodes.size()][];
        int countOr = 0;
        int sizeFactorized = 1;
        int[] sizePerOr = new int[orNodes.size()];
        for (ExprOrNode orNode : orNodes) {
            expressionsWithoutOr.removeNode(orNode);
            orNodesMaps[countOr] = new FilterSpecParaForgeMap[orNode.getChildNodes().length];
            int len = orNode.getChildNodes().length;

            for (int i = 0; i < len; i++) {
                FilterSpecParaForgeMap map = new FilterSpecParaForgeMap();
                orNodesMaps[countOr][i] = map;
                List<ExprNode> nodes = Collections.singletonList(orNode.getChildNodes()[i]);
                decomposePopulateConsolidate(map, false, nodes, args);
            }

            sizePerOr[countOr] = len;
            sizeFactorized = sizeFactorized * len;
            countOr++;
        }

        // we become too large
        if (sizeFactorized > filterServiceMaxFilterWidth) {
            return null;
        }

        // combine
        FilterSpecPlanPathForge[] result = new FilterSpecPlanPathForge[sizeFactorized];
        CombinationEnumeration permutations = CombinationEnumeration.fromZeroBasedRanges(sizePerOr);
        int count = 0;
        for (; permutations.hasMoreElements(); ) {
            Object[] permutation = permutations.nextElement();
            result[count] = computePermutation(expressionsWithoutOr, permutation, orNodesMaps, args);
            count++;
        }
        return new FilterSpecPlanForge(result, null, null, null);
    }

    private static FilterSpecPlanPathForge computePermutation(FilterSpecParaForgeMap filterParamExprMap, Object[] permutation, FilterSpecParaForgeMap[][] orNodesMaps, FilterSpecCompilerArgs args)
        throws ExprValidationException {
        FilterSpecParaForgeMap mapAll = new FilterSpecParaForgeMap();
        mapAll.add(filterParamExprMap);

        // combine
        for (int orNodeNum = 0; orNodeNum < permutation.length; orNodeNum++) {
            int orChildNodeNum = (Integer) permutation[orNodeNum];
            FilterSpecParaForgeMap mapOrSub = orNodesMaps[orNodeNum][orChildNodeNum];
            mapAll.add(mapOrSub);
        }

        // consolidate across
        FilterSpecCompilerConsolidateUtil.consolidate(mapAll, args.statementRawInfo.getStatementName());

        List<FilterSpecPlanPathTripletForge> filterParams = new ArrayList<>(mapAll.getTriplets());
        int countUnassigned = mapAll.countUnassignedExpressions();

        if (countUnassigned != 0) {
            FilterSpecPlanPathTripletForge node = makeRemainingNode(mapAll.getUnassignedExpressions(), args);
            filterParams.add(node);
        }

        FilterSpecPlanPathTripletForge[] triplets = filterParams.toArray(new FilterSpecPlanPathTripletForge[0]);
        return new FilterSpecPlanPathForge(triplets, null);
    }
}
