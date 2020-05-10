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

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanComputeHelper.computeFixedLength;

public class FilterSpecPlanComputeConditionalTopOnly extends FilterSpecPlanComputeConditional {

    public static final FilterSpecPlanComputeConditionalTopOnly INSTANCE = new FilterSpecPlanComputeConditionalTopOnly();

    protected FilterValueSetParam[][] compute(EventBean[] eventsPerStream, FilterSpecPlan plan, MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        if (plan.filterNegate != null) {
            Boolean controlResult = (Boolean) plan.filterNegate.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (controlResult != null && !controlResult) {
                return null;
            }
        }
        if (plan.filterConfirm != null) {
            Boolean controlResult = (Boolean) plan.filterConfirm.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (controlResult != null && controlResult) {
                return FilterValueSetParam.EMPTY;
            }
        }
        return computeFixedLength(plan.paths, matchedEvents, exprEvaluatorContext, filterEvalEnv);
    }
}
