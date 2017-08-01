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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.*;

import java.util.List;

public class MyTrendSpotterViewFactory extends ViewFactorySupport {
    private List<ExprNode> viewParameters;

    private ExprNode expression;
    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException {
        this.viewParameters = viewParameters;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate("Trend spotter view", parentEventType, statementContext, viewParameters, false);
        String message = "Trend spotter view accepts a single integer or double value";
        if (validated.length != 1) {
            throw new ViewParameterException(message);
        }
        Class resultType = validated[0].getForge().getEvaluationType();
        if ((resultType != Integer.class) && (resultType != int.class) &&
                (resultType != Double.class) && (resultType != double.class)) {
            throw new ViewParameterException(message);
        }
        expression = validated[0];
        eventType = MyTrendSpotterView.createEventType(statementContext);
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new MyTrendSpotterView(agentInstanceViewFactoryContext, expression);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return "Trend-spotter";
    }
}
