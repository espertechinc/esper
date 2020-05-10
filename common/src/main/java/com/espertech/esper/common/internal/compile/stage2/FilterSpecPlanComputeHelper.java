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

import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class FilterSpecPlanComputeHelper {

    protected static FilterValueSetParam[][] computeFixedLength(FilterSpecPlanPath[] paths, MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        FilterValueSetParam[][] valueList = new FilterValueSetParam[paths.length][];
        for (int i = 0; i < paths.length; i++) {
            FilterSpecPlanPath path = paths[i];
            valueList[i] = new FilterValueSetParam[path.getTriplets().length];
            populateValueSet(valueList[i], matchedEvents, path.getTriplets(), exprEvaluatorContext, filterEvalEnv);
        }
        return valueList;
    }

    protected static void populateValueSet(FilterValueSetParam[] valueList, MatchedEventMap matchedEvents, FilterSpecPlanPathTriplet[] triplets, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        int count = 0;
        for (FilterSpecPlanPathTriplet specParam : triplets) {
            FilterValueSetParam valueParam = specParam.getParam().getFilterValue(matchedEvents, exprEvaluatorContext, filterEvalEnv);
            valueList[count] = valueParam;
            count++;
        }
    }
}
