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

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;

import java.util.*;
import java.util.function.BiConsumer;

public class ContextControllerInitTermSvcLevelAny implements ContextControllerInitTermSvc {
    private Map<IntSeqKey, NestedEntry> mgmt = new HashMap<>();
    private Map<IntSeqKey, ContextControllerInitTermSvcEntry> endConditions = new HashMap<>();

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        NestedEntry existing = mgmt.putIfAbsent(controllerPath, new NestedEntry(0, null, parentPartitionKeys));
        if (existing != null) {
            throw new IllegalStateException("Unexpected existing entry for path");
        }
    }

    public Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath) {
        NestedEntry entry = mgmt.get(controllerPath);
        return entry == null ? null : entry.parentPartitionKeys;
    }

    public ContextControllerConditionNonHA mgmtDelete(IntSeqKey controllerPath) {
        NestedEntry existing = mgmt.remove(controllerPath);
        return existing == null ? null : existing.startCondition;
    }

    public ContextControllerConditionNonHA mgmtUpdClearStartCondition(IntSeqKey controllerPath) {
        NestedEntry existing = mgmt.get(controllerPath);
        ContextControllerConditionNonHA tmp = null;
        if (existing != null) {
            tmp = existing.startCondition;
            existing.startCondition = null;
        }
        return tmp;
    }

    public void mgmtUpdSetStartCondition(IntSeqKey controllerPath, ContextControllerConditionNonHA startCondition) {
        NestedEntry existing = mgmt.get(controllerPath);
        if (existing != null) {
            existing.startCondition = startCondition;
        }
    }

    public int mgmtUpdIncSubpath(IntSeqKey controllerPath) {
        NestedEntry existing = mgmt.get(controllerPath);
        if (existing == null) {
            throw new IllegalStateException("Unexpected no-entry-found for path");
        }
        return existing.currentSubpath++;
    }

    public void endCreate(IntSeqKey endConditionPath, int subpathIdOrCPId, ContextControllerConditionNonHA endCondition, ContextControllerInitTermPartitionKey partitionKey) {
        endConditions.put(endConditionPath, new ContextControllerInitTermSvcEntry(subpathIdOrCPId, endCondition, partitionKey));
    }

    public ContextControllerInitTermSvcEntry endDelete(IntSeqKey conditionPath) {
        return endConditions.remove(conditionPath);
    }

    public Collection<ContextControllerInitTermSvcEntry> endDeleteByParentPath(IntSeqKey controllerPath) {
        List<ContextControllerInitTermSvcEntry> entries = new ArrayList<>();
        Iterator<Map.Entry<IntSeqKey, ContextControllerInitTermSvcEntry>> it = endConditions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IntSeqKey, ContextControllerInitTermSvcEntry> entry = it.next();
            if (controllerPath.isParentTo(entry.getKey())) {
                entries.add(entry.getValue());
                it.remove();
            }
        }
        return entries;
    }

    public void endVisit(IntSeqKey controllerPath, BiConsumer<ContextControllerInitTermPartitionKey, Integer> partKeyAndCPId) {
        for (Map.Entry<IntSeqKey, ContextControllerInitTermSvcEntry> entry : endConditions.entrySet()) {
            if (controllerPath.isParentTo(entry.getKey())) {
                partKeyAndCPId.accept(entry.getValue().getPartitionKey(), entry.getValue().getSubpathIdOrCPId());
            }
        }
    }

    public void destroy() {
        mgmt = null;
        endConditions = null;
    }

    private static class NestedEntry {
        int currentSubpath;
        ContextControllerConditionNonHA startCondition;
        Object[] parentPartitionKeys;

        NestedEntry(int currentSubpath, ContextControllerConditionNonHA startCondition, Object[] parentPartitionKeys) {
            this.currentSubpath = currentSubpath;
            this.startCondition = startCondition;
            this.parentPartitionKeys = parentPartitionKeys;
        }
    }
}
