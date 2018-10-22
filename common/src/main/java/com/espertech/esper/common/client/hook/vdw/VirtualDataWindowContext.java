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
package com.espertech.esper.common.client.hook.vdw;

import com.espertech.esper.common.client.EventBeanFactory;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactory;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDataWindowOutStreamImpl;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;

/**
 * Context for use with virtual data window factory {@link VirtualDataWindowFactory} provides
 * contextual information about the named window and the type of events held,
 * handle for posting insert and remove streams and factory for event bean instances.
 */
public class VirtualDataWindowContext {

    private final VirtualDWViewFactory factory;
    private final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final EventBeanFactory eventBeanFactory;
    private final VirtualDataWindowOutStreamImpl outputStream;

    /**
     * Ctor.
     *
     * @param factory                         factory
     * @param agentInstanceViewFactoryContext context
     * @param eventBeanFactory                event bean factory
     * @param outputStream                    output stream
     */
    public VirtualDataWindowContext(VirtualDWViewFactory factory, AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, EventBeanFactory eventBeanFactory, VirtualDataWindowOutStreamImpl outputStream) {
        this.factory = factory;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.eventBeanFactory = eventBeanFactory;
        this.outputStream = outputStream;
    }

    /**
     * Returns the statement context which holds statement information (name, expression, id) and statement-level services.
     *
     * @return statement context
     */
    public StatementContext getStatementContext() {
        return agentInstanceViewFactoryContext.getAgentInstanceContext().getStatementContext();
    }

    /**
     * Returns the event type of the events held in the virtual data window as per declaration of the named window.
     *
     * @return event type
     */
    public EventType getEventType() {
        return factory.getEventType();
    }

    /**
     * Returns the parameters passed; for example "create window ABC.my:vdw("10.0.0.1")" passes one paramater here.
     *
     * @return parameters
     */
    public Object[] getParameters() {
        return factory.getParameters();
    }

    /**
     * Returns the factory for creating instances of EventBean from rows.
     *
     * @return event bean factory
     */
    public EventBeanFactory getEventFactory() {
        return eventBeanFactory;
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
        return factory.getNamedWindowName();
    }

    /**
     * Returns the expressions passed as parameters to the virtual data window.
     *
     * @return parameter expressions
     */
    public ExprEvaluator[] getParameterExpressions() {
        return factory.getParameterExpressions();
    }

    /**
     * Returns the agent instance (context partition) context.
     *
     * @return context
     */
    public AgentInstanceContext getAgentInstanceContext() {
        return agentInstanceViewFactoryContext.getAgentInstanceContext();
    }

    /**
     * Returns the factory
     *
     * @return factory
     */
    public VirtualDWViewFactory getFactory() {
        return factory;
    }

    /**
     * Returns the agent instance context
     *
     * @return agent instance context
     */
    public AgentInstanceViewFactoryChainContext getAgentInstanceViewFactoryContext() {
        return agentInstanceViewFactoryContext;
    }
}
