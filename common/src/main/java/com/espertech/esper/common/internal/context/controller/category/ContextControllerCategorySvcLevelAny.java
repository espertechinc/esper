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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.internal.collection.IntSeqKey;

import java.util.HashMap;
import java.util.Map;

public class ContextControllerCategorySvcLevelAny implements ContextControllerCategorySvc {
    private final Map<IntSeqKey, ContextControllerCategorySvcLevelAnyEntry> mgmt = new HashMap<>();

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys, int[] subpathOrCPId) {
        ContextControllerCategorySvcLevelAnyEntry existing = mgmt.putIfAbsent(controllerPath, new ContextControllerCategorySvcLevelAnyEntry(parentPartitionKeys, subpathOrCPId));
        if (existing != null) {
            throw new IllegalStateException("Existing entry found");
        }
    }

    public int[] mgmtGetSubpathOrCPIds(IntSeqKey controllerPath) {
        ContextControllerCategorySvcLevelAnyEntry existing = mgmt.get(controllerPath);
        return existing == null ? null : existing.getSubpathOrCPids();
    }

    public int[] mgmtDelete(IntSeqKey controllerPath) {
        ContextControllerCategorySvcLevelAnyEntry entry = mgmt.remove(controllerPath);
        return entry == null ? null : entry.getSubpathOrCPids();
    }

    public void destroy() {
        mgmt.clear();
    }

    private static class ContextControllerCategorySvcLevelAnyEntry {
        private final Object[] parentPartitionKeys;
        private final int[] subpathOrCPids;

        public ContextControllerCategorySvcLevelAnyEntry(Object[] parentPartitionKeys, int[] subpathOrCPids) {
            this.parentPartitionKeys = parentPartitionKeys;
            this.subpathOrCPids = subpathOrCPids;
        }

        public Object[] getParentPartitionKeys() {
            return parentPartitionKeys;
        }

        public int[] getSubpathOrCPids() {
            return subpathOrCPids;
        }
    }
}
