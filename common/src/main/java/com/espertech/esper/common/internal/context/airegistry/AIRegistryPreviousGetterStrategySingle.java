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

public class AIRegistryPreviousGetterStrategySingle implements AIRegistryPreviousGetterStrategy {
    private PreviousGetterStrategy service;

    public void assignService(int serviceId, PreviousGetterStrategy previousGetterStrategy) {
        service = previousGetterStrategy;
    }

    public void deassignService(int serviceId) {
        service = null;
    }

    public int getInstanceCount() {
        return service == null ? 0 : 1;
    }

    public PreviousGetterStrategy getStrategy(ExprEvaluatorContext ctx) {
        return service.getStrategy(ctx);
    }
}
