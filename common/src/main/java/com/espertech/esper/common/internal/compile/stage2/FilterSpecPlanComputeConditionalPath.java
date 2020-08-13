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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanComputeHelper.populateValueSet;

public class FilterSpecPlanComputeConditionalPath extends FilterSpecPlanComputeConditional {

    public static final FilterSpecPlanComputeConditionalPath INSTANCE = new FilterSpecPlanComputeConditionalPath();

    protected FilterValueSetParam[][] compute(EventBean[] eventsPerStream, FilterSpecPlan plan, MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        if (plan.filterNegate != null) {
            Boolean controlResult = (Boolean) plan.filterNegate.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (controlResult == null || !controlResult) {
                return null;
            }
        }
        if (plan.filterConfirm != null) {
            Boolean controlResult = (Boolean) plan.filterConfirm.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (controlResult != null && controlResult) {
                return FilterValueSetParam.EMPTY;
            }
        }
        return computePathsWithNegate(eventsPerStream, plan, matchedEvents, exprEvaluatorContext, filterEvalEnv);
    }

    private FilterValueSetParam[][] computePathsWithNegate(EventBean[] eventsPerStream, FilterSpecPlan plan, MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        FilterSpecPlanPath[] paths = plan.paths;
        List<FilterValueSetParam[]> pathList = new ArrayList<>(paths.length);
        for (FilterSpecPlanPath path : paths) {
            if (path.getPathNegate() != null) {
                Boolean controlResult = (Boolean) path.getPathNegate().evaluate(eventsPerStream, true, exprEvaluatorContext);
                if (controlResult == null || !controlResult) {
                    continue;
                }
            }

            FilterSpecPlanPathTriplet[] triplets = path.getTriplets();
            FilterValueSetParam[] valueList = new FilterValueSetParam[triplets.length];
            populateValueSet(valueList, matchedEvents, path.getTriplets(), exprEvaluatorContext, filterEvalEnv);
            pathList.add(valueList);
        }
        if (pathList.isEmpty()) {
            return null; // all path negated
        }
        return pathList.toArray(new FilterValueSetParam[0][]);
    }
}
