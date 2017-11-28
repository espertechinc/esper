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
package com.espertech.esper.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.internal.ViewGroupDelegate;

import java.util.List;

/**
 * Factory interface for a factory responsible for creating a {@link View} instance and for determining
 * if an existing view meets requirements.
 */
public interface ViewFactory {
    /**
     * Indicates user EPL query view parameters to the view factory.
     *
     * @param viewFactoryContext supplied context information for the view factory
     * @param viewParameters     is the objects representing the view parameters
     * @throws ViewParameterException if the parameters don't match view parameter needs
     */
    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException;

    /**
     * Attaches the factory to a parent event type such that the factory can validate
     * attach requirements and determine an event type for resulting views.
     *
     * @param parentEventType       is the parent event stream's or view factory's event type
     * @param statementContext      contains the services needed for creating a new event type
     * @param optionalParentFactory is null when there is no parent view factory, or contains the
     *                              parent view factory
     * @param parentViewFactories   is a list of all the parent view factories or empty list if there are none
     * @throws ViewParameterException is thrown to indicate that this view factories's view would not play
     *                                with the parent view factories view
     */
    public void attach(EventType parentEventType,
                       StatementContext statementContext,
                       ViewFactory optionalParentFactory,
                       List<ViewFactory> parentViewFactories) throws ViewParameterException;

    /**
     * Create a new view.
     *
     * @param agentInstanceViewFactoryContext context
     * @return view
     */
    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext);

    /**
     * Returns the event type that the view that is created by the view factory would create for events posted
     * by the view.
     *
     * @return event type of view's created by the view factory
     */
    public EventType getEventType();

    /**
     * Determines if the given view could be used instead of creating a new view,
     * requires the view factory to compare view type, parameters and other capabilities provided.
     *
     * @param view                 is the candidate view to compare to
     * @param agentInstanceContext agent instance context
     * @return true if the given view can be reused instead of creating a new view, or false to indicate
     * the view is not right for reuse
     */
    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext);

    /**
     * Returns the name of the view, not namespace+name but readable name.
     *
     * @return readable name
     */
    public String getViewName();

    default public View makeViewGroupDelegate() {
        return new ViewGroupDelegate(this);
    }
}
