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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;

public class ContextControllerFactoryContext {
    private final String outermostContextName;
    private final String contextName;
    private final EPServicesContext servicesContext;
    private final AgentInstanceContext agentInstanceContextCreate;
    private final int nestingLevel;
    private final int numNestingLevels;
    private final boolean isRecoveringResilient;
    private final ContextStateCache stateCache;

    public ContextControllerFactoryContext(String outermostContextName, String contextName, EPServicesContext servicesContext, AgentInstanceContext agentInstanceContextCreate, int nestingLevel, int numNestingLevels, boolean isRecoveringResilient, ContextStateCache stateCache) {
        this.outermostContextName = outermostContextName;
        this.contextName = contextName;
        this.servicesContext = servicesContext;
        this.agentInstanceContextCreate = agentInstanceContextCreate;
        this.nestingLevel = nestingLevel;
        this.numNestingLevels = numNestingLevels;
        this.isRecoveringResilient = isRecoveringResilient;
        this.stateCache = stateCache;
    }

    public String getOutermostContextName() {
        return outermostContextName;
    }

    public String getContextName() {
        return contextName;
    }

    public EPServicesContext getServicesContext() {
        return servicesContext;
    }

    public AgentInstanceContext getAgentInstanceContextCreate() {
        return agentInstanceContextCreate;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public boolean isRecoveringResilient() {
        return isRecoveringResilient;
    }

    public ContextStateCache getStateCache() {
        return stateCache;
    }

    public int getNumNestingLevels() {
        return numNestingLevels;
    }
}
