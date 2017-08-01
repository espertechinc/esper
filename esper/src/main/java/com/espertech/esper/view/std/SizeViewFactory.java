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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.*;
import com.espertech.esper.view.stat.StatViewAdditionalProps;

import java.util.List;

/**
 * Factory for {@link SizeView} instances.
 */
public class SizeViewFactory implements ViewFactory {
    protected final static String NAME = "Size";

    private List<ExprNode> viewParameters;
    private int streamNumber;

    protected StatViewAdditionalProps additionalProps;

    protected EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;
        this.streamNumber = viewFactoryContext.getStreamNum();
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, true);
        additionalProps = StatViewAdditionalProps.make(validated, 0, parentEventType, statementContext.getEngineImportService(), statementContext.getStatementName());
        eventType = SizeView.createEventType(statementContext, additionalProps, streamNumber);
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new SizeView(agentInstanceViewFactoryContext.getAgentInstanceContext(), eventType, additionalProps);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof SizeView)) {
            return false;
        }
        if (additionalProps != null) {
            return false;
        }
        return true;
    }

    public String getViewName() {
        return NAME;
    }

    public StatViewAdditionalProps getAdditionalProps() {
        return additionalProps;
    }
}
