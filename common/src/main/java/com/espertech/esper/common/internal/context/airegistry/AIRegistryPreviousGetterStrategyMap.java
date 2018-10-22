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

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.HashMap;
import java.util.Map;

public class AIRegistryPreviousGetterStrategyMap implements AIRegistryPreviousGetterStrategy {
    private final Map<Integer, PreviousGetterStrategy> services;

    AIRegistryPreviousGetterStrategyMap() {
        this.services = new HashMap<>();
    }

    public void assignService(int serviceId, PreviousGetterStrategy previousGetterStrategy) {
        services.put(serviceId, previousGetterStrategy);
    }

    public void deassignService(int serviceId) {
        services.remove(serviceId);
    }

    public int getInstanceCount() {
        return services.size();
    }

    public PreviousGetterStrategy getStrategy(ExprEvaluatorContext ctx) {
        return services.get(ctx.getAgentInstanceId()).getStrategy(ctx);
    }
}
