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
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRandomAccess;

import java.util.HashMap;
import java.util.Map;

public class AIRegistryRowRecogPreviousStrategyMap implements AIRegistryRowRecogPreviousStrategy {
    private final Map<Integer, RowRecogPreviousStrategy> services;

    AIRegistryRowRecogPreviousStrategyMap() {
        this.services = new HashMap<>();
    }

    public void assignService(int serviceId, RowRecogPreviousStrategy service) {
        services.put(serviceId, service);
    }

    public void deassignService(int serviceId) {
        services.remove(serviceId);
    }

    public int getInstanceCount() {
        return services.size();
    }

    public RowRecogStateRandomAccess getAccess(ExprEvaluatorContext exprEvaluatorContext) {
        return services.get(exprEvaluatorContext.getAgentInstanceId()).getAccess(exprEvaluatorContext);
    }
}
