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
package com.espertech.esper.runtime.internal.dataflow.op.filter;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class FilterFactory implements DataFlowOperatorFactory {

    private ExprEvaluator filter;
    private boolean singleOutputPort;
    private EventType eventType;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        return new FilterOp(this, context.getAgentInstanceContext());
    }

    public void setFilter(ExprEvaluator filter) {
        this.filter = filter;
    }

    public void setSingleOutputPort(boolean singleOutputPort) {
        this.singleOutputPort = singleOutputPort;
    }

    public ExprEvaluator getFilter() {
        return filter;
    }

    public boolean isSingleOutputPort() {
        return singleOutputPort;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
