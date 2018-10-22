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
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRandomAccess;

public class AIRegistryRowRecogPreviousStrategyMultiPerm implements RowRecogPreviousStrategy, AIRegistryRowRecogPreviousStrategy {
    private final ArrayWrap<RowRecogPreviousStrategy> services;
    private int count;

    AIRegistryRowRecogPreviousStrategyMultiPerm() {
        this.services = new ArrayWrap<>(RowRecogPreviousStrategy.class, 2);
    }

    public void assignService(int serviceId, RowRecogPreviousStrategy service) {
        AIRegistryUtil.checkExpand(serviceId, services);
        services.getArray()[serviceId] = service;
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

    public RowRecogStateRandomAccess getAccess(ExprEvaluatorContext exprEvaluatorContext) {
        return services.getArray()[exprEvaluatorContext.getAgentInstanceId()].getAccess(exprEvaluatorContext);
    }
}
