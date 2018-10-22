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

public class ContextControllerKeyedSvcLevelOne implements ContextControllerKeyedSvc {
    private final static Object[] EMPTY_PARTITION_KEYS = new Object[0];

    private int currentSubpathId = 0;
    private ContextControllerFilterEntry[] filterEntries;
    private final Map<Object, ContextControllerKeyedSvcEntry> keys = new HashMap<>();

    public Object[] mgmtGetPartitionKeys(IntSeqKey controllerPath) {
        return EMPTY_PARTITION_KEYS;
    }

    public int mgmtGetIncSubpath(IntSeqKey controllerPath) {
        int subpathId = currentSubpathId;
        currentSubpathId++;
        return subpathId;
    }

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        // no action, parent partition keys always empty
    }

    public void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries) {
        this.filterEntries = filterEntries;
    }

    public ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath) {
        return filterEntries;
    }

    public boolean keyHasSeen(IntSeqKey controllerPath, Object key) {
        return keys.containsKey(key);
    }

    public void keyAdd(IntSeqKey controllerPath, Object key, int subpathIdOrCPId, ContextControllerConditionNonHA terminationCondition) {
        keys.put(key, new ContextControllerKeyedSvcEntry(subpathIdOrCPId, terminationCondition));
    }

    public ContextControllerKeyedSvcEntry keyRemove(IntSeqKey controllerPath, Object key) {
        return keys.remove(key);
    }

    public List<ContextControllerConditionNonHA> keyGetTermConditions(IntSeqKey controllerPath) {
        List<ContextControllerConditionNonHA> conditions = new ArrayList<>();
        for (Map.Entry<Object, ContextControllerKeyedSvcEntry> entry : keys.entrySet()) {
            conditions.add(entry.getValue().getTerminationCondition());
        }
        return conditions;
    }

    public void keyVisit(IntSeqKey controllerPath, BiConsumer<Object, Integer> keyAndSubpathOrCPId) {
        for (Map.Entry<Object, ContextControllerKeyedSvcEntry> entry : keys.entrySet()) {
            keyAndSubpathOrCPId.accept(entry.getKey(), entry.getValue().getSubpathOrCPId());
        }
    }

    public int keyGetSubpathOrCPId(IntSeqKey controllerPath, Object key) {
        ContextControllerKeyedSvcEntry entry = keys.get(key);
        return entry == null ? -1 : entry.getSubpathOrCPId();
    }

    public Collection<Integer> deactivate(IntSeqKey controllerPath) {
        List<Integer> result = new ArrayList<>(keys.size());
        for (Map.Entry<Object, ContextControllerKeyedSvcEntry> entry : keys.entrySet()) {
            result.add(entry.getValue().getSubpathOrCPId());
        }
        destroy();
        return result;
    }

    public void destroy() {
        keys.clear();
        filterEntries = null;
    }
}
