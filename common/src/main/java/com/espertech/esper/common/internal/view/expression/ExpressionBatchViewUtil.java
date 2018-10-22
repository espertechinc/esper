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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;

public class ExpressionBatchViewUtil {
    public static boolean evaluate(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext, ExpressionViewFactoryBase factory, AggregationService aggregationService) {
        // Evaluation with aggregation requires a lock on the factory as the aggregation-field is assigned per-factory
        if (aggregationService != null) {
            synchronized (factory) {
                factory.getAggregationResultFutureAssignable().assign(aggregationService);
                Boolean result = (Boolean) factory.getExpiryEval().evaluate(eventsPerStream, true, agentInstanceContext);
                if (result == null) {
                    return false;
                }
                return result;
            }
        }

        Boolean result = (Boolean) factory.getExpiryEval().evaluate(eventsPerStream, true, agentInstanceContext);
        if (result == null) {
            return false;
        }
        return result;
    }
}
