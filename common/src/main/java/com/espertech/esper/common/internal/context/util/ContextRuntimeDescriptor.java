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
package com.espertech.esper.common.internal.context.util;

public class ContextRuntimeDescriptor {
    private final String contextName;
    private final String contextDeploymentId;
    private final ContextIteratorHandler iteratorHandler;

    public ContextRuntimeDescriptor(String contextName, String contextDeploymentId, ContextIteratorHandler iteratorHandler) {
        this.contextName = contextName;
        this.contextDeploymentId = contextDeploymentId;
        this.iteratorHandler = iteratorHandler;
    }

    public ContextIteratorHandler getIteratorHandler() {
        return iteratorHandler;
    }

    public String getContextName() {
        return contextName;
    }

    public String getContextDeploymentId() {
        return contextDeploymentId;
    }
}
