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
package com.espertech.esper.example.ohlc;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

public class OHLCBarPlugInViewFactory implements ViewFactory {

    private ExprEvaluator timestampExpression;
    private ExprEvaluator valueExpression;
    private EventType eventType;

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {

    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new OHLCBarPlugInView(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return OHLCBarPlugInView.class.getSimpleName();
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ExprEvaluator getTimestampExpression() {
        return timestampExpression;
    }

    public void setTimestampExpression(ExprEvaluator timestampExpression) {
        this.timestampExpression = timestampExpression;
    }

    public ExprEvaluator getValueExpression() {
        return valueExpression;
    }

    public void setValueExpression(ExprEvaluator valueExpression) {
        this.valueExpression = valueExpression;
    }
}
