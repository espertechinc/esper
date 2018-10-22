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

public class SubselectAggregatorViewFilteredGrouped extends SubselectAggregatorViewBase {

    public SubselectAggregatorViewFilteredGrouped(AggregationService aggregationService, ExprEvaluator optionalFilterExpr, ExprEvaluatorContext exprEvaluatorContext, ExprEvaluator groupKeys) {
        super(aggregationService, optionalFilterExpr, exprEvaluatorContext, groupKeys);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        exprEvaluatorContext.getInstrumentationProvider().qSubselectAggregation();

        if (newData != null) {
            for (EventBean theEvent : newData) {
                eventsPerStream[0] = theEvent;
                boolean isPass = filter(true);
                if (isPass) {
                    Object groupKey = generateGroupKey(true);
                    aggregationService.applyEnter(eventsPerStream, groupKey, exprEvaluatorContext);
                }
            }
        }

        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                eventsPerStream[0] = theEvent;
                boolean isPass = filter(false);
                if (isPass) {
                    Object groupKey = generateGroupKey(false);
                    aggregationService.applyLeave(eventsPerStream, groupKey, exprEvaluatorContext);
                }
            }
        }

        exprEvaluatorContext.getInstrumentationProvider().aSubselectAggregation();
    }
}
