/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link FirstLengthWindowView}.
 */
public class FirstLengthWindowViewFactory implements AsymetricDataWindowViewFactory
{
    /**
     * Size of length first window.
     */
    protected int size;

    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException
    {
        List<Object> viewParameters = ViewFactorySupport.validateAndEvaluate(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters);
        if (viewParameters.size() != 1)
        {
            throw new ViewParameterException(getViewParamMessage());
        }

        Object parameter = viewParameters.get(0);
        if (!(parameter instanceof Number))
        {
            throw new ViewParameterException(getViewParamMessage());
        }
        Number numParam = (Number) parameter;
        if ( (JavaClassHelper.isFloatingPointNumber(numParam)) ||
             (numParam instanceof Long))
        {
            throw new ViewParameterException(getViewParamMessage());
        }

        size =  numParam.intValue();
        if (size <= 0)
        {
            throw new ViewParameterException(getViewName() + " view requires a positive number");
        }
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException
    {
        this.eventType = parentEventType;
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext)
    {
        return new FirstLengthWindowView(agentInstanceViewFactoryContext, this, size);
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public boolean canReuse(View view)
    {
        if (!(view instanceof FirstLengthWindowView))
        {
            return false;
        }

        FirstLengthWindowView myView = (FirstLengthWindowView) view;
        if (myView.getSize() != size)
        {
            return false;
        }

        return myView.isEmpty();
    }

    public String getViewName() {
        return "First-Length";
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires an integer-type size parameter";
    }

    public int getSize() {
        return size;
    }
}
