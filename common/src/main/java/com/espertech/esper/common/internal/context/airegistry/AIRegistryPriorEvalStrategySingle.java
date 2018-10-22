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

public class AIRegistryPriorEvalStrategySingle implements AIRegistryPriorEvalStrategy {
    private PriorEvalStrategy service;

    public void assignService(int serviceId, PriorEvalStrategy priorEvalStrategy) {
        service = priorEvalStrategy;
    }

    public void deassignService(int serviceId) {
        service = null;
    }

    public int getInstanceCount() {
        return service == null ? 0 : 1;
    }

    public EventBean getSubstituteEvent(EventBean originalEvent, boolean isNewData, int constantIndexNumber, int relativeIndex, ExprEvaluatorContext exprEvaluatorContext, int streamNum) {
        return service.getSubstituteEvent(originalEvent, isNewData, constantIndexNumber, relativeIndex, exprEvaluatorContext, streamNum);
    }
}
