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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public abstract class SubselectAggregationPreprocessorBase {

    protected final AggregationService aggregationService;
    protected final ExprEvaluator filterEval;
    protected final ExprEvaluator groupKeys;

    public SubselectAggregationPreprocessorBase(AggregationService aggregationService, ExprEvaluator filterEval, ExprEvaluator groupKeys) {
        this.aggregationService = aggregationService;
        this.filterEval = filterEval;
        this.groupKeys = groupKeys;
    }

    public abstract void evaluate(EventBean[] eventsPerStream, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    protected Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return groupKeys.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
