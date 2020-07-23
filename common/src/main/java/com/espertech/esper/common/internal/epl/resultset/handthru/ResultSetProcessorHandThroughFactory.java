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
package com.espertech.esper.common.internal.epl.resultset.handthru;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactory;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;

public class ResultSetProcessorHandThroughFactory implements ResultSetProcessorFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ResultSetProcessorHandThroughFactory.class);

    private final SelectExprProcessor selectExprProcessor;
    private final EventType resultEventType;
    private final boolean rstream;

    public ResultSetProcessorHandThroughFactory(SelectExprProcessor selectExprProcessor, EventType resultEventType, boolean rstream) {
        this.selectExprProcessor = selectExprProcessor;
        this.resultEventType = resultEventType;
        this.rstream = rstream;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, ExprEvaluatorContext exprEvaluatorContext) {
        return new ResultSetProcessorHandThroughImpl(this, exprEvaluatorContext);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor() {
        return selectExprProcessor;
    }

    public boolean isRstream() {
        return rstream;
    }
}
