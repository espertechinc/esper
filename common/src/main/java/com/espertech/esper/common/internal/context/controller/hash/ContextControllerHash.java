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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerSelectorUtil;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

import java.util.function.BiConsumer;

public abstract class ContextControllerHash extends ContextControllerBase {

    protected final ContextControllerHashFactory factory;

    public ContextControllerHash(ContextManagerRealization realization, ContextControllerHashFactory factory) {
        super(realization);
        this.factory = factory;
    }

    protected abstract void visitPartitions(IntSeqKey controllerPath, BiConsumer<Integer, Integer> hashAndCPId);

    protected abstract int getSubpathOrCPId(IntSeqKey path, int hash);

    public void visitSelectedPartitions(IntSeqKey path, ContextPartitionSelector selector, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel) {
        if (selector instanceof ContextPartitionSelectorHash) {
            ContextPartitionSelectorHash selectorHash = (ContextPartitionSelectorHash) selector;
            if (selectorHash.getHashes() == null || selectorHash.getHashes().isEmpty()) {
                return;
            }
            for (int hash : selectorHash.getHashes()) {
                int subpathOrCPId = getSubpathOrCPId(path, hash);
                if (subpathOrCPId != -1) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            }
            return;
        }
        if (selector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) selector;
            ContextPartitionIdentifierHash identifierHash = new ContextPartitionIdentifierHash();

            visitPartitions(path, (hash, subpathOrCPId) -> {
                identifierHash.setHash(hash);
                if (factory.getFactoryEnv().isLeaf()) {
                    identifierHash.setContextPartitionId(subpathOrCPId);
                }
                if (filter.filter(identifierHash)) {
                    realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel);
                }
            });
            return;
        }
        if (selector instanceof ContextPartitionSelectorAll) {
            visitPartitions(path, (hash, subpathOrCPId) -> realization.contextPartitionRecursiveVisit(path, subpathOrCPId, this, visitor, selectorPerLevel));
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
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorHash.class}, selector);
    }

    public AgentInstanceContext getAgentInstanceContextCreate() {
        return realization.getAgentInstanceContextCreate();
    }

    public ContextControllerHashFactory getFactory() {
        return factory;
    }

    public ContextManagerRealization getRealization() {
        return realization;
    }

    protected int[] activateByPreallocate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent) {
        int granularity = factory.getHashSpec().getGranularity();
        int[] cpOrSubpathIds = new int[granularity];
        for (int i = 0; i < factory.getHashSpec().getGranularity(); i++) {
            ContextPartitionInstantiationResult result = realization.contextPartitionInstantiate(path, i, this, optionalTriggeringEvent, null, parentPartitionKeys, i);
            cpOrSubpathIds[i] = result.getSubpathOrCPId();
        }
        return cpOrSubpathIds;
    }
}
