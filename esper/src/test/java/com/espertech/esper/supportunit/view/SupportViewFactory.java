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
package com.espertech.esper.supportunit.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewParameterException;

import java.util.List;

public abstract class SupportViewFactory implements ViewFactory {
    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {

    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public EventType getEventType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
