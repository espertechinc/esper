/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpec;
import com.espertech.esper.epl.agg.access.AggregationStateSortedSpec;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.util.CollectionUtil;

import java.util.Comparator;

public class SortedAggregationStateFactoryFactory {

    private final MethodResolutionService methodResolutionService;
    private final ExprEvaluator[] evaluators;
    private final boolean[] sortDescending;
    private final boolean ever;
    private final int streamNum;
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;

    public SortedAggregationStateFactoryFactory(MethodResolutionService methodResolutionService, ExprEvaluator[] evaluators, boolean[] sortDescending, boolean ever, int streamNum, ExprAggMultiFunctionSortedMinMaxByNode parent) {
        this.methodResolutionService = methodResolutionService;
        this.evaluators = evaluators;
        this.sortDescending = sortDescending;
        this.ever = ever;
        this.streamNum = streamNum;
        this.parent = parent;
    }

    public AggregationStateFactory makeFactory() {
        boolean sortUsingCollator = methodResolutionService.isSortUsingCollator();
        Comparator<Object> comparator = CollectionUtil.getComparator(evaluators, sortUsingCollator, sortDescending);

        if (ever) {
            AggregationStateMinMaxByEverSpec spec = new AggregationStateMinMaxByEverSpec(streamNum, evaluators, parent.isMax(), comparator);
            return methodResolutionService.getAggregationFactoryFactory().makeMinMaxEver(parent, spec);
        }

        AggregationStateSortedSpec spec = new AggregationStateSortedSpec(streamNum, evaluators, comparator);
        return methodResolutionService.getAggregationFactoryFactory().makeSorted(parent, spec);
    }
}
