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
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory for union-views.
 */
public class UnionViewFactory implements ViewFactory, DataWindowViewFactory, ViewFactoryContainer {
    /**
     * The event type.
     */
    protected EventType parentEventType;

    /**
     * The view factories.
     */
    protected List<ViewFactory> viewFactories;

    /**
     * Ctor.
     * Dependencies injected after reflective instantiation.
     */
    public UnionViewFactory() {
    }

    /**
     * Sets the parent event type.
     *
     * @param parentEventType type
     */
    public void setParentEventType(EventType parentEventType) {
        this.parentEventType = parentEventType;
    }

    /**
     * Sets the view factories.
     *
     * @param viewFactories factories
     */
    public void setViewFactories(List<ViewFactory> viewFactories) {
        this.viewFactories = viewFactories;
    }

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException {
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        boolean hasAsymetric = false;
        List<View> views = new ArrayList<View>();
        for (ViewFactory viewFactory : viewFactories) {
            views.add(viewFactory.makeView(agentInstanceViewFactoryContext));
            hasAsymetric |= viewFactory instanceof AsymetricDataWindowViewFactory;
        }
        if (hasAsymetric) {
            return new UnionAsymetricView(agentInstanceViewFactoryContext, this, parentEventType, views);
        }
        return new UnionView(agentInstanceViewFactoryContext, this, parentEventType, views);
    }

    public EventType getEventType() {
        return parentEventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        return false;
    }

    public String getViewName() {
        return IntersectViewFactory.getViewNameUnionIntersect(false, viewFactories);
    }

    public Collection<ViewFactory> getViewFactoriesContained() {
        return viewFactories;
    }
}
