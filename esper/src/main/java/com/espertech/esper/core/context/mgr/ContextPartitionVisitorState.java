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

import com.espertech.esper.client.context.ContextPartitionDescriptor;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionState;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ContextPartitionVisitorState implements ContextPartitionVisitor {

    private final TreeMap<ContextStatePathKey, ContextStatePathValue> states = new TreeMap<ContextStatePathKey, ContextStatePathValue>();
    private final Map<Integer, ContextPartitionDescriptor> contextPartitionInfo = new HashMap<Integer, ContextPartitionDescriptor>();

    public ContextPartitionVisitorState() {
    }

    public void visit(int nestingLevel, int pathId, ContextStatePathValueBinding binding, Object payload, ContextController contextController, ContextControllerInstanceHandle instanceHandle) {
        ContextStatePathKey key = new ContextStatePathKey(nestingLevel, pathId, instanceHandle.getSubPathId());
        int agentInstanceId = instanceHandle.getContextPartitionOrPathId();
        states.put(key, new ContextStatePathValue(agentInstanceId, binding.toByteArray(payload), instanceHandle.getInstances().getState()));

        ContextPartitionState state = instanceHandle.getInstances().getState();
        ContextPartitionIdentifier identifier = contextController.getFactory().keyPayloadToIdentifier(payload);
        ContextPartitionDescriptor descriptor = new ContextPartitionDescriptor(agentInstanceId, identifier, state);
        contextPartitionInfo.put(agentInstanceId, descriptor);
    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getStates() {
        return states;
    }

    public Map<Integer, ContextPartitionDescriptor> getContextPartitionInfo() {
        return contextPartitionInfo;
    }
}
