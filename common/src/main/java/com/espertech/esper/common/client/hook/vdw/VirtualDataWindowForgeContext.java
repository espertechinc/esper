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
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.io.Serializable;

/**
 * Context for use with virtual data window forge {@link com.espertech.esper.common.client.hook.vdw.VirtualDataWindowForge} provides
 * contextual information about the named window and the type of events held.
 */
public class VirtualDataWindowForgeContext {

    private final EventType eventType;
    private final Object[] parameters;
    private final ExprNode[] parameterExpressions;
    private final String namedWindowName;
    private final ViewForgeEnv viewForgeEnv;
    private final Serializable customConfiguration;

    /**
     * Ctor.
     *
     * @param eventType            the event type that the named window is declared to hold.
     * @param parameters           the parameters passed when declaring the named window, for example "create window ABC.my:vdw("10.0.0.1")" passes one paramater here.
     * @param namedWindowName      the name of the named window
     * @param parameterExpressions parameter expressions passed to the virtual data window
     * @param customConfiguration  additional configuration
     * @param viewForgeEnv         view forge environment
     */
    public VirtualDataWindowForgeContext(EventType eventType, Object[] parameters, ExprNode[] parameterExpressions, String namedWindowName, ViewForgeEnv viewForgeEnv, Serializable customConfiguration) {
        this.eventType = eventType;
        this.parameters = parameters;
        this.parameterExpressions = parameterExpressions;
        this.namedWindowName = namedWindowName;
        this.viewForgeEnv = viewForgeEnv;
        this.customConfiguration = customConfiguration;
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
     * Returns any additional configuration provided.
     *
     * @return additional config
     */
    public Serializable getCustomConfiguration() {
        return customConfiguration;
    }

    /**
     * Returns the view forge environment
     *
     * @return view forge environment
     */
    public ViewForgeEnv getViewForgeEnv() {
        return viewForgeEnv;
    }
}
