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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

import java.io.Serializable;

/**
 * Context for use with virtual data window factory {@link VirtualDataWindowFactory} provides
 * contextual information as well as the handle for posting insert and remove streams and factory for event bean instances.
 */
public class VirtualDataWindowFactoryContext {
    private final EventType eventType;
    private final Object[] parameters;
    private final ExprEvaluator[] parameterExpressions;
    private final String namedWindowName;
    private final Serializable customConfiguration;
    private final ViewFactoryContext viewFactoryContext;
    private final EPStatementInitServices services;

    /**
     * Ctor.
     *
     * @param eventType            event type
     * @param parameters           parameter values
     * @param parameterExpressions parameter expressions
     * @param namedWindowName      named window name
     * @param customConfiguration  custom configuration object that is passed along
     * @param viewFactoryContext   view context
     * @param services             services
     */
    public VirtualDataWindowFactoryContext(EventType eventType, Object[] parameters, ExprEvaluator[] parameterExpressions, String namedWindowName, Serializable customConfiguration, ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        this.eventType = eventType;
        this.parameters = parameters;
        this.parameterExpressions = parameterExpressions;
        this.namedWindowName = namedWindowName;
        this.customConfiguration = customConfiguration;
        this.viewFactoryContext = viewFactoryContext;
        this.services = services;
    }

    /**
     * Returns the event type
     *
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns parameters
     *
     * @return parameters
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Returns parameter expressions as expression evaluators
     *
     * @return expression evaluators
     */
    public ExprEvaluator[] getParameterExpressions() {
        return parameterExpressions;
    }

    /**
     * Returns the named window name
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }

    /**
     * Returns the view factory context
     *
     * @return view factory context
     */
    public ViewFactoryContext getViewFactoryContext() {
        return viewFactoryContext;
    }

    /**
     * Returns initialization-time services
     *
     * @return services
     */
    public EPStatementInitServices getServices() {
        return services;
    }

    /**
     * Returns the custom configuration object that gets passed along
     *
     * @return configuration object
     */
    public Serializable getCustomConfiguration() {
        return customConfiguration;
    }
}
