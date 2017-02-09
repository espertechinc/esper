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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link CorrelationView} instances.
 */
public class CorrelationViewFactory implements ViewFactory {
    private List<ExprNode> viewParameters;
    private int streamNumber;

    /**
     * Property name of X field.
     */
    protected ExprNode expressionX;

    /**
     * Property name of Y field.
     */
    protected ExprNode expressionY;

    /**
     * Additional properties.
     */
    protected StatViewAdditionalProps additionalProps;

    /**
     * Event type.
     */
    protected EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;
        this.streamNumber = viewFactoryContext.getStreamNum();
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, true);
        if (validated.length < 2) {
            throw new ViewParameterException(getViewParamMessage());
        }
        if ((!JavaClassHelper.isNumeric(validated[0].getExprEvaluator().getType())) || (!JavaClassHelper.isNumeric(validated[1].getExprEvaluator().getType()))) {
            throw new ViewParameterException(getViewParamMessage());
        }

        expressionX = validated[0];
        expressionY = validated[1];

        additionalProps = StatViewAdditionalProps.make(validated, 2, parentEventType);
        eventType = CorrelationView.createEventType(statementContext, additionalProps, streamNumber);
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new CorrelationView(this, agentInstanceViewFactoryContext.getAgentInstanceContext(), expressionX, expressionY, eventType, additionalProps);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof CorrelationView)) {
            return false;
        }

        if (additionalProps != null) {
            return false;
        }

        CorrelationView other = (CorrelationView) view;
        if (!ExprNodeUtility.deepEquals(other.getExpressionX(), expressionX) ||
                (!ExprNodeUtility.deepEquals(other.getExpressionY(), expressionY))) {
            return false;
        }

        return true;
    }

    public String getViewName() {
        return "Correlation";
    }

    public StatViewAdditionalProps getAdditionalProps() {
        return additionalProps;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires two expressions providing x and y values as properties";
    }
}
