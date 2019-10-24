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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptor;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorPattern;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.mgr.ContextControllerSelectorUtil;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.Map;
import java.util.function.BiConsumer;

public abstract class ContextControllerInitTerm implements ContextController {
    protected final ContextControllerInitTermFactory factory;
    protected final ContextManagerRealization realization;

    public ContextControllerInitTerm(ContextControllerInitTermFactory factory, ContextManagerRealization realization) {
        this.factory = factory;
        this.realization = realization;
    }

    protected abstract void visitPartitions(IntSeqKey controllerPath, BiConsumer<ContextControllerInitTermPartitionKey, Integer> partKeyAndCPId);

    public ContextControllerInitTermFactory getFactory() {
        return factory;
    }

    public ContextManagerRealization getRealization() {
        return realization;
    }

    public void visitSelectedPartitions(IntSeqKey path, ContextPartitionSelector selector, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel) {
        if (selector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) selector;
            visitPartitions(path, (partitionKey, subpathOrCPIds) -> {
                ContextPartitionIdentifierInitiatedTerminated identifier = ContextControllerInitTermUtil.keyToIdentifier(subpathOrCPIds, partitionKey, this);
                if (filter.filter(identifier)) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPIds, this, visitor, selectorPerLevel);
                }
            });
            return;
        }
        if (selector instanceof ContextPartitionSelectorAll) {
            visitPartitions(path, (partitionKey, subpathOrCPIds) -> {
                realization.contextPartitionRecursiveVisit(path, subpathOrCPIds, this, visitor, selectorPerLevel);
            });
            return;
        }
        if (selector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) selector;
            visitPartitions(path, (hash, subpathOrCPId) -> {
                if (byId.getContextPartitionIds().contains(subpathOrCPId)) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            });
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[0], selector);
    }

    public void populateEndConditionFromTrigger(MatchedEventMap map, EventBean triggeringEvent) {
        // compute correlated termination
        ContextConditionDescriptor start = factory.getInitTermSpec().getStartCondition();
        if (!(start instanceof ContextConditionDescriptorFilter)) {
            return;
        }
        ContextConditionDescriptorFilter filter = (ContextConditionDescriptorFilter) start;
        if (filter.getOptionalFilterAsName() == null) {
            return;
        }
        int tag = map.getMeta().getTagFor(filter.getOptionalFilterAsName());
        if (tag == -1) {
            return;
        }
        map.add(tag, triggeringEvent);
    }

    public void populateEndConditionFromTrigger(MatchedEventMap map, Map<String, Object> matchedEventMap) {
        // compute correlated termination
        ContextConditionDescriptor start = factory.getInitTermSpec().getStartCondition();
        if (!(start instanceof ContextConditionDescriptorPattern)) {
            return;
        }
        ContextConditionDescriptorPattern pattern = (ContextConditionDescriptorPattern) start;
        for (String tagged : pattern.getTaggedEvents()) {
            populatePattern(tagged, map, matchedEventMap);
        }
        for (String array : pattern.getArrayEvents()) {
            populatePattern(array, map, matchedEventMap);
        }
    }

    private void populatePattern(String tagged, MatchedEventMap map, Map<String, Object> matchedEventMap) {
        Object value = matchedEventMap.get(tagged);
        if (value == null) {
            return;
        }
        int tag = map.getMeta().getTagFor(tagged);
        if (tag == -1) {
            return;
        }
        map.add(tag, value);
    }
}
