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
import com.espertech.esper.common.internal.collection.ArrayWrap;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;

public class AIRegistryPriorEvalStrategyMultiPerm implements AIRegistryPriorEvalStrategy {
    private final ArrayWrap<PriorEvalStrategy> services;
    private int count;

    AIRegistryPriorEvalStrategyMultiPerm() {
        this.services = new ArrayWrap<>(PriorEvalStrategy.class, 2);
    }

    public void assignService(int serviceId, PriorEvalStrategy priorEvalStrategy) {
        AIRegistryUtil.checkExpand(serviceId, services);
        services.getArray()[serviceId] = priorEvalStrategy;
        count++;
    }

    public void deassignService(int serviceId) {
        if (serviceId >= services.getArray().length) {
            // possible since it may not have been assigned as there was nothing to assign
            return;
        }
        services.getArray()[serviceId] = null;
        count--;
    }

    public int getInstanceCount() {
        return count;
    }

    public EventBean getSubstituteEvent(EventBean originalEvent, boolean isNewData, int constantIndexNumber, int relativeIndex, ExprEvaluatorContext exprEvaluatorContext, int streamNum) {
        return services.getArray()[exprEvaluatorContext.getAgentInstanceId()].getSubstituteEvent(originalEvent, isNewData, constantIndexNumber, relativeIndex, exprEvaluatorContext, streamNum);
    }
}
