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
import com.espertech.esper.client.context.ContextPartitionIdentifierNested;
import com.espertech.esper.client.context.ContextPartitionState;

import java.util.*;

public class ContextPartitionVisitorStateWithPath implements ContextPartitionVisitorWithPath {

    private final ContextControllerFactory[] nestedFactories;
    private final Map<ContextController, ContextControllerTreeEntry> subcontexts;

    private final TreeMap<ContextStatePathKey, ContextStatePathValue> states = new TreeMap<ContextStatePathKey, ContextStatePathValue>();
    private final ArrayList<Integer> subpaths = new ArrayList<Integer>();
    private final Map<ContextController, List<LeafDesc>> controllerAgentInstances = new HashMap<ContextController, List<LeafDesc>>();
    private final Map<Integer, ContextPartitionDescriptor> agentInstanceInfo = new HashMap<Integer, ContextPartitionDescriptor>();

    public ContextPartitionVisitorStateWithPath(ContextControllerFactory[] nestedFactories, Map<ContextController, ContextControllerTreeEntry> subcontexts) {
        this.nestedFactories = nestedFactories;
        this.subcontexts = subcontexts;
    }

    public void visit(int nestingLevel, int pathId, ContextStatePathValueBinding binding, Object payload, ContextController contextController, ContextControllerInstanceHandle instanceHandle) {
        ContextStatePathKey key = new ContextStatePathKey(nestingLevel, pathId, instanceHandle.getSubPathId());
        int maxNestingLevel = nestedFactories.length;

        int contextPartitionOrSubPath = instanceHandle.getContextPartitionOrPathId();

        if (contextController.getFactory().getFactoryContext().getNestingLevel() == maxNestingLevel) {
            List<LeafDesc> agentInstances = controllerAgentInstances.get(contextController);
            if (agentInstances == null) {
                agentInstances = new ArrayList<LeafDesc>();
                controllerAgentInstances.put(contextController, agentInstances);
            }
            ContextStatePathValue value = new ContextStatePathValue(contextPartitionOrSubPath, binding.toByteArray(payload), instanceHandle.getInstances().getState());
            agentInstances.add(new LeafDesc(key, value));

            // generate a nice payload text from the paths of keys
            ContextControllerTreeEntry entry = subcontexts.get(contextController);
            ContextPartitionIdentifier[] keys = ContextManagerNested.getTreeCompositeKey(nestedFactories, payload, entry, subcontexts);
            ContextPartitionDescriptor descriptor = new ContextPartitionDescriptor(contextPartitionOrSubPath, new ContextPartitionIdentifierNested(keys), value.getState());
            agentInstanceInfo.put(contextPartitionOrSubPath, descriptor);
            states.put(key, value);
        } else {
            // handle non-leaf
            subpaths.add(contextPartitionOrSubPath);
            states.put(key, new ContextStatePathValue(contextPartitionOrSubPath, binding.toByteArray(payload), ContextPartitionState.STARTED));
        }
    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getStates() {
        return states;
    }

    public void resetSubPaths() {
        subpaths.clear();
    }

    public Collection<Integer> getSubpaths() {
        return subpaths;
    }

    public Map<ContextController, List<LeafDesc>> getControllerAgentInstances() {
        return controllerAgentInstances;
    }

    public Map<Integer, ContextPartitionDescriptor> getAgentInstanceInfo() {
        return agentInstanceInfo;
    }

    public static class LeafDesc {
        private final ContextStatePathKey key;
        private final ContextStatePathValue value;

        public LeafDesc(ContextStatePathKey key, ContextStatePathValue value) {
            this.key = key;
            this.value = value;
        }

        public ContextStatePathKey getKey() {
            return key;
        }

        public ContextStatePathValue getValue() {
            return value;
        }
    }
}
