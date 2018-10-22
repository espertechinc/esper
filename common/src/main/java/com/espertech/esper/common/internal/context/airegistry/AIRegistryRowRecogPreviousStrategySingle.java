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

public class AIRegistryRowRecogPreviousStrategySingle implements AIRegistryRowRecogPreviousStrategy, RowRecogPreviousStrategy {
    private RowRecogPreviousStrategy service;

    public AIRegistryRowRecogPreviousStrategySingle() {
    }

    public void assignService(int serviceId, RowRecogPreviousStrategy strategy) {
        service = strategy;
    }

    public void deassignService(int serviceId) {
        service = null;
    }

    public RowRecogStateRandomAccess getAccess(ExprEvaluatorContext exprEvaluatorContext) {
        return service.getAccess(exprEvaluatorContext);
    }

    public int getInstanceCount() {
        return service == null ? 0 : 1;
    }
}
