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

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;

import java.util.*;
import java.util.function.BiConsumer;

public class ContextControllerKeyedSvcLevelAny implements ContextControllerKeyedSvc {
    private final Map<IntSeqKey, MgmtInfo> mgmt = new HashMap<>();
    private final Map<ContextControllerKeyedCompositeKey, ContextControllerKeyedSvcEntry> keys = new HashMap<>();

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        mgmt.put(controllerPath, new MgmtInfo(0, null, parentPartitionKeys));
    }

    public void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries) {
        MgmtInfo entry = mgmt.get(controllerPath);
        entry.setFilterEntries(filterEntries);
    }

    public Object[] mgmtGetPartitionKeys(IntSeqKey controllerPath) {
        return mgmt.get(controllerPath).parentPartitionKeys;
    }

    public int mgmtGetIncSubpath(IntSeqKey controllerPath) {
        MgmtInfo entry = mgmt.get(controllerPath);
        int subpathId = entry.currentSubpathId;
        entry.currentSubpathId++;
        return subpathId;
    }

    public ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath) {
        return mgmt.get(controllerPath).filterEntries;
    }

    public boolean keyHasSeen(IntSeqKey controllerPath, Object key) {
        return keys.containsKey(new ContextControllerKeyedCompositeKey(controllerPath, key));
    }

    public void keyAdd(IntSeqKey controllerPath, Object key, int subpathIdOrCPId, ContextControllerConditionNonHA terminationCondition) {
        keys.put(new ContextControllerKeyedCompositeKey(controllerPath, key), new ContextControllerKeyedSvcEntry(subpathIdOrCPId, terminationCondition));
    }

    public ContextControllerKeyedSvcEntry keyRemove(IntSeqKey controllerPath, Object key) {
        return keys.remove(new ContextControllerKeyedCompositeKey(controllerPath, key));
    }

    public List<ContextControllerConditionNonHA> keyGetTermConditions(IntSeqKey controllerPath) {
        List<ContextControllerConditionNonHA> conditions = new ArrayList<>();
        for (Map.Entry<ContextControllerKeyedCompositeKey, ContextControllerKeyedSvcEntry> entry : keys.entrySet()) {
            if (controllerPath.equals(entry.getKey().getPath())) {
                conditions.add(entry.getValue().getTerminationCondition());
            }
        }
        return conditions;
    }

    public void keyVisit(IntSeqKey controllerPath, BiConsumer<Object, Integer> keyAndSubpathOrCPId) {
        for (Map.Entry<ContextControllerKeyedCompositeKey, ContextControllerKeyedSvcEntry> entry : keys.entrySet()) {
            if (controllerPath.equals(entry.getKey().getPath())) {
                keyAndSubpathOrCPId.accept(entry.getKey().getKey(), entry.getValue().getSubpathOrCPId());
            }
        }
    }

    public int keyGetSubpathOrCPId(IntSeqKey controllerPath, Object key) {
        ContextControllerKeyedSvcEntry entry = keys.get(new ContextControllerKeyedCompositeKey(controllerPath, key));
        return entry == null ? -1 : entry.getSubpathOrCPId();
    }

    public Collection<Integer> deactivate(IntSeqKey controllerPath) {
        List<Integer> ids = new ArrayList<>();
        Iterator<Map.Entry<ContextControllerKeyedCompositeKey, ContextControllerKeyedSvcEntry>> iterator = keys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ContextControllerKeyedCompositeKey, ContextControllerKeyedSvcEntry> entry = iterator.next();
            if (controllerPath.equals(entry.getKey().getPath())) {
                ids.add(entry.getValue().getSubpathOrCPId());
                iterator.remove();
            }
        }
        return ids;
    }

    public void destroy() {
        mgmt.clear();
        keys.clear();
    }

    private static class MgmtInfo {
        private int currentSubpathId;
        private ContextControllerFilterEntry[] filterEntries;
        private Object[] parentPartitionKeys;

        public MgmtInfo(int currentSubpathId, ContextControllerFilterEntry[] filterEntries, Object[] parentPartitionKeys) {
            this.currentSubpathId = currentSubpathId;
            this.filterEntries = filterEntries;
            this.parentPartitionKeys = parentPartitionKeys;
        }

        public int getCurrentSubpathId() {
            return currentSubpathId;
        }

        public ContextControllerFilterEntry[] getFilterEntries() {
            return filterEntries;
        }

        public Object[] getParentPartitionKeys() {
            return parentPartitionKeys;
        }

        public void setFilterEntries(ContextControllerFilterEntry[] filterEntries) {
            this.filterEntries = filterEntries;
        }
    }
}
