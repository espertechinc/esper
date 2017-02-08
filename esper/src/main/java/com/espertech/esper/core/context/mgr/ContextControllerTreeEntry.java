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

import java.util.Map;

public class ContextControllerTreeEntry {

    private ContextController parent;
    private Object initPartitionKey;
    private Map<String, Object> initContextProperties;
    private Map<Integer, ContextControllerTreeAgentInstanceList> agentInstances;
    private Map<Integer, ContextController> childContexts;

    public ContextControllerTreeEntry(ContextController parent, Map<Integer, ContextController> childContexts, Object initPartitionKey, Map<String, Object> initContextProperties) {
        this.parent = parent;
        this.childContexts = childContexts;
        this.initPartitionKey = initPartitionKey;
        this.initContextProperties = initContextProperties;
    }

    public ContextController getParent() {
        return parent;
    }

    public Map<Integer, ContextController> getChildContexts() {
        return childContexts;
    }

    public void setChildContexts(Map<Integer, ContextController> childContexts) {
        this.childContexts = childContexts;
    }

    public Object getInitPartitionKey() {
        return initPartitionKey;
    }

    public Map<Integer, ContextControllerTreeAgentInstanceList> getAgentInstances() {
        return agentInstances;
    }

    public void setAgentInstances(Map<Integer, ContextControllerTreeAgentInstanceList> agentInstances) {
        this.agentInstances = agentInstances;
    }

    public Map<String, Object> getInitContextProperties() {
        return initContextProperties;
    }
}
