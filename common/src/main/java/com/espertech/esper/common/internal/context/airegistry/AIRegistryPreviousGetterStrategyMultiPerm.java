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

import com.espertech.esper.common.internal.collection.ArrayWrap;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class AIRegistryPreviousGetterStrategyMultiPerm implements AIRegistryPreviousGetterStrategy {
    private final ArrayWrap<PreviousGetterStrategy> services;
    private int count;

    AIRegistryPreviousGetterStrategyMultiPerm() {
        this.services = new ArrayWrap<>(PreviousGetterStrategy.class, 2);
    }

    public void assignService(int serviceId, PreviousGetterStrategy previousGetterStrategy) {
        AIRegistryUtil.checkExpand(serviceId, services);
        services.getArray()[serviceId] = previousGetterStrategy;
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

    public PreviousGetterStrategy getStrategy(ExprEvaluatorContext ctx) {
        return services.getArray()[ctx.getAgentInstanceId()].getStrategy(ctx);
    }
}
