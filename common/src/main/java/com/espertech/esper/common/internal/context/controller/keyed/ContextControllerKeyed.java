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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerSelectorUtil;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

import java.util.function.BiConsumer;

public abstract class ContextControllerKeyed extends ContextControllerBase {

    protected final ContextControllerKeyedFactory factory;
    protected EventBean lastTerminatingEvent;

    protected abstract void visitPartitions(IntSeqKey path, BiConsumer<Object, Integer> keyAndSubpathOrCPId);

    protected abstract int getSubpathOrCPId(IntSeqKey path, Object keyForLookup);

    public ContextControllerKeyed(ContextManagerRealization realization, ContextControllerKeyedFactory factory) {
        super(realization);
        this.factory = factory;
    }

    public AgentInstanceContext getAgentInstanceContextCreate() {
        return realization.getAgentInstanceContextCreate();
    }

    public ContextControllerKeyedFactory getFactory() {
        return factory;
    }

    public ContextManagerRealization getRealization() {
        return realization;
    }

    public void visitSelectedPartitions(IntSeqKey path, ContextPartitionSelector selector, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel) {
        if (selector instanceof ContextPartitionSelectorSegmented) {
            ContextPartitionSelectorSegmented partitioned = (ContextPartitionSelectorSegmented) selector;
            if (partitioned.getPartitionKeys() == null || partitioned.getPartitionKeys().isEmpty()) {
                return;
            }
            for (Object[] key : partitioned.getPartitionKeys()) {
                Object keyForLookup = factory.getKeyedSpec().getMultiKeyFromObjectArray().from(key);
                int subpathOrCPId = getSubpathOrCPId(path, keyForLookup);
                if (subpathOrCPId != -1) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            }
            return;
        } else if (selector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filtered = (ContextPartitionSelectorFiltered) selector;
            ContextPartitionIdentifierPartitioned identifier = new ContextPartitionIdentifierPartitioned();
            visitPartitions(path, (key, subpathOrCPId) -> {
                if (factory.getFactoryEnv().isLeaf()) {
                    identifier.setContextPartitionId(subpathOrCPId);
                }
                Object[] keys = ContextControllerKeyedUtil.unpackKey(key);
                identifier.setKeys(keys);
                if (filtered.filter(identifier)) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            });
            return;
        } else if (selector instanceof ContextPartitionSelectorAll) {
            visitPartitions(path, (key, subpathOrCPId) -> realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel));
            return;
        } else if (selector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById ids = (ContextPartitionSelectorById) selector;
            visitPartitions(path, (key, subpathOrCPId) -> {
                if (ids.getContextPartitionIds().contains(subpathOrCPId)) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            });
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorSegmented.class}, selector);
    }
}
