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
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceUtil;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

public class ContextControllerHashImpl extends ContextControllerHash {

    private final ContextControllerHashSvc hashSvc;

    public ContextControllerHashImpl(ContextControllerHashFactory factory, ContextManagerRealization realization) {
        super(realization, factory);
        hashSvc = ContextControllerHashUtil.makeService(factory, realization);
    }

    public void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern) {
        hashSvc.mgmtCreate(path, parentPartitionKeys);

        if (factory.getHashSpec().isPreallocate()) {
            int[] subpathOrCPIds = activateByPreallocate(path, parentPartitionKeys, optionalTriggeringEvent);
            hashSvc.mgmtSetSubpathOrCPIdsWhenPreallocate(path, subpathOrCPIds);
            return;
        }

        ContextControllerDetailHashItem[] hashItems = factory.getHashSpec().getItems();
        ContextControllerFilterEntry[] filterEntries = new ContextControllerFilterEntry[hashItems.length];

        for (int i = 0; i < hashItems.length; i++) {
            ContextControllerDetailHashItem item = hashItems[i];
            filterEntries[i] = new ContextControllerHashFilterEntry(this, path, item, parentPartitionKeys);

            if (optionalTriggeringEvent != null) {
                boolean match = AgentInstanceUtil.evaluateFilterForStatement(optionalTriggeringEvent, realization.getAgentInstanceContextCreate(), filterEntries[i].getFilterHandle());

                if (match) {
                    matchFound(item, optionalTriggeringEvent, path);
                }
            }
        }
        hashSvc.mgmtSetFilters(path, filterEntries);
    }

    public void deactivate(IntSeqKey path, boolean terminateChildContexts) {
        if (factory.getHashSpec().isPreallocate() && terminateChildContexts) {
            int[] subpathOrCPIds = hashSvc.mgmtGetSubpathOrCPIdsWhenPreallocate(path);
            for (int i = 0; i < factory.getHashSpec().getGranularity(); i++) {
                realization.contextPartitionTerminate(path, subpathOrCPIds[i], this, null, false, null);
            }
            return;
        }

        ContextControllerFilterEntry[] filters = hashSvc.mgmtGetFilters(path);
        if (filters != null) {
            for (ContextControllerFilterEntry callback : filters) {
                ((ContextControllerHashFilterEntry) callback).destroy();
            }
        }

        Collection<Integer> subpathOrCPIds = hashSvc.deactivate(path);
        for (int id : subpathOrCPIds) {
            realization.contextPartitionTerminate(path, id, this, null, false, null);
        }
    }

    public void matchFound(ContextControllerDetailHashItem item, EventBean theEvent, IntSeqKey controllerPath) {
        int value = (Integer) item.getLookupable().getGetter().get(theEvent);

        if (hashSvc.hashHasSeenPartition(controllerPath, value)) {
            return;
        }

        Object[] parentPartitionKeys = hashSvc.mgmtGetParentPartitionKeys(controllerPath);
        ContextPartitionInstantiationResult result = realization.contextPartitionInstantiate(controllerPath, value, this, theEvent, null, parentPartitionKeys, value);
        int subpathIdOrCPId = result.getSubpathOrCPId();
        hashSvc.hashAddPartition(controllerPath, value, subpathIdOrCPId);

        // update the filter version for this handle
        long filterVersion = realization.getAgentInstanceContextCreate().getFilterService().getFiltersVersion();
        realization.getAgentInstanceContextCreate().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filterVersion);
    }

    protected void visitPartitions(IntSeqKey controllerPath, BiConsumer<Integer, Integer> hashAndCPId) {
        hashSvc.hashVisit(controllerPath, hashAndCPId);
    }

    protected int getSubpathOrCPId(IntSeqKey path, int hash) {
        return hashSvc.hashGetSubpathOrCPId(path, hash);
    }

    public void destroy() {
        hashSvc.destroy();
    }
}
