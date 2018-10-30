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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ContextDeployment {
    private static final Logger log = LoggerFactory.getLogger(ContextDeployment.class);

    private final Map<String, ContextManager> contexts = new HashMap<>(4);

    public void add(ContextDefinition contextDefinition, EPStatementInitServices services) {
        String contextName = contextDefinition.getContextName();
        ContextManager mgr = contexts.get(contextName);
        if (mgr != null) {
            throw new EPException("Context by name '" + contextDefinition.getContextName() + "' already exists");
        }

        ContextManagerResident contextManager = new ContextManagerResident(services.getDeploymentId(), contextDefinition);
        contexts.put(contextName, contextManager);
    }

    public ContextManager getContextManager(String contextName) {
        return contexts.get(contextName);
    }

    public int getContextCount() {
        return contexts.size();
    }

    public void destroyContext(String deploymentIdCreateContext, String contextName) {
        ContextManager entry = contexts.get(contextName);
        if (entry == null) {
            log.warn("Destroy for context '" + contextName + "' deployment-id '" + deploymentIdCreateContext + "' failed to locate");
            return;
        }
        entry.destroyContext();
        contexts.remove(contextName);
    }

    public Map<String, ContextManager> getContexts() {
        return contexts;
    }
}
