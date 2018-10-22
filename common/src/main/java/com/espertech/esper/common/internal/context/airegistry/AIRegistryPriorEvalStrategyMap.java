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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;

import java.util.HashMap;
import java.util.Map;

public class AIRegistryPriorEvalStrategyMap implements AIRegistryPriorEvalStrategy {
    private final Map<Integer, PriorEvalStrategy> services;

    AIRegistryPriorEvalStrategyMap() {
        this.services = new HashMap<>();
    }

    public void assignService(int serviceId, PriorEvalStrategy priorEvalStrategy) {
        services.put(serviceId, priorEvalStrategy);
    }

    public void deassignService(int serviceId) {
        services.remove(serviceId);
    }

    public int getInstanceCount() {
        return services.size();
    }

    public EventBean getSubstituteEvent(EventBean originalEvent, boolean isNewData, int constantIndexNumber, int relativeIndex, ExprEvaluatorContext exprEvaluatorContext, int streamNum) {
        return services.get(exprEvaluatorContext.getAgentInstanceId()).getSubstituteEvent(originalEvent, isNewData, constantIndexNumber, relativeIndex, exprEvaluatorContext, streamNum);
    }
}
