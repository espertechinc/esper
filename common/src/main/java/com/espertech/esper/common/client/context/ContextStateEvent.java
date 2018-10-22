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
package com.espertech.esper.common.client.context;

/**
 * Context state event.
 */
public abstract class ContextStateEvent {
    private final String runtimeURI;
    private final String contextDeploymentId;
    private final String contextName;

    /**
     * Ctor.
     *
     * @param runtimeURI          runtime URI
     * @param contextDeploymentId deployment id of create-context statement
     * @param contextName         context name
     */
    public ContextStateEvent(String runtimeURI, String contextDeploymentId, String contextName) {
        this.runtimeURI = runtimeURI;
        this.contextDeploymentId = contextDeploymentId;
        this.contextName = contextName;
    }

    /**
     * Returns the context name
     *
     * @return context name
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Returns the deployment id
     *
     * @return deployment id
     */
    public String getContextDeploymentId() {
        return contextDeploymentId;
    }

    /**
     * Returns the runtime URI
     *
     * @return runtime URI
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }
}
