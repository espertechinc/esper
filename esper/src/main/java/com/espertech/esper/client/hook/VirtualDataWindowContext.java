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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBeanFactory;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.ViewFactoryContext;

import java.io.Serializable;

/**
 * Context for use with virtual data window factory {@link VirtualDataWindowFactory} provides
 * contextual information about the named window and the type of events held,
 * handle for posting insert and remove streams and factory for event bean instances.
 */
public class VirtualDataWindowContext {

    private final AgentInstanceContext agentInstanceContext;
    private final EventType eventType;
    private final Object[] parameters;
    private final ExprNode[] parameterExpressions;
    private final EventBeanFactory eventFactory;
    private final VirtualDataWindowOutStream outputStream;
    private final String namedWindowName;
    private final ViewFactoryContext viewFactoryContext;
    private final Serializable customConfiguration;

    /**
     * Ctor.
     *
     * @param agentInstanceContext statement services and statement information such as statement name, statement id, EPL expression
     * @param eventType            the event type that the named window is declared to hold.
     * @param parameters           the parameters passed when declaring the named window, for example "create window ABC.my:vdw("10.0.0.1")" passes one paramater here.
     * @param eventFactory         factory for converting row objects to EventBean instances
     * @param outputStream         forward the input and output stream received from the update method here
     * @param namedWindowName      the name of the named window
     * @param parameterExpressions parameter expressions passed to the virtual data window
     * @param viewFactoryContext   context of services
     * @param customConfiguration  additional configuration
     */
    public VirtualDataWindowContext(AgentInstanceContext agentInstanceContext, EventType eventType, Object[] parameters, ExprNode[] parameterExpressions, EventBeanFactory eventFactory, VirtualDataWindowOutStream outputStream, String namedWindowName, ViewFactoryContext viewFactoryContext, Serializable customConfiguration) {
        this.agentInstanceContext = agentInstanceContext;
        this.eventType = eventType;
        this.parameters = parameters;
        this.parameterExpressions = parameterExpressions;
        this.eventFactory = eventFactory;
        this.outputStream = outputStream;
        this.namedWindowName = namedWindowName;
        this.viewFactoryContext = viewFactoryContext;
        this.customConfiguration = customConfiguration;
    }

    /**
     * Returns the statement context which holds statement information (name, expression, id) and statement-level services.
     *
     * @return statement context
     */
    public StatementContext getStatementContext() {
        return agentInstanceContext.getStatementContext();
    }

    /**
     * Returns the event type of the events held in the virtual data window as per declaration of the named window.
     *
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns the parameters passed; for example "create window ABC.my:vdw("10.0.0.1")" passes one paramater here.
     *
     * @return parameters
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Returns the factory for creating instances of EventBean from rows.
     *
     * @return event bean factory
     */
    public EventBeanFactory getEventFactory() {
        return eventFactory;
    }

    /**
     * Returns a handle for use to send insert and remove stream data to consuming statements.
     * <p>
     * Typically use "context.getOutputStream().update(newData, oldData);" in the update method of the virtual data window.
     *
     * @return handle for posting insert and remove stream
     */
    public VirtualDataWindowOutStream getOutputStream() {
        return outputStream;
    }

    /**
     * Returns the name of the named window used in connection with the virtual data window.
     *
     * @return named window
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }

    /**
     * Returns the expressions passed as parameters to the virtual data window.
     *
     * @return parameter expressions
     */
    public ExprNode[] getParameterExpressions() {
        return parameterExpressions;
    }

    /**
     * Returns the engine services context.
     *
     * @return engine services context
     */
    public ViewFactoryContext getViewFactoryContext() {
        return viewFactoryContext;
    }

    /**
     * Returns any additional configuration provided.
     *
     * @return additional config
     */
    public Serializable getCustomConfiguration() {
        return customConfiguration;
    }

    /**
     * Returns the agent instance (context partition) context.
     *
     * @return context
     */
    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceContext;
    }
}
