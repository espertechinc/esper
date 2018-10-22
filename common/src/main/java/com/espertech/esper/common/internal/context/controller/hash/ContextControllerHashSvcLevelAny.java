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

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;

import java.util.*;
import java.util.function.BiConsumer;

public class ContextControllerHashSvcLevelAny implements ContextControllerHashSvc {
    private final Map<IntSeqKey, MgmtInfo> mgmt = new HashMap<>();
    private Map<IntSeqKey, Integer> optionalHashes;

    ContextControllerHashSvcLevelAny(boolean preallocate) {
        if (!preallocate) {
            optionalHashes = new HashMap<>();
        }
    }

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        mgmt.put(controllerPath, new MgmtInfo(null, parentPartitionKeys));
    }

    public int[] mgmtGetSubpathOrCPIdsWhenPreallocate(IntSeqKey path) {
        return mgmt.get(path).getSubpathOrCPIdsPreallocate();
    }

    public void mgmtSetSubpathOrCPIdsWhenPreallocate(IntSeqKey path, int[] subpathOrCPIds) {
        mgmt.get(path).setSubpathOrCPIdsPreallocate(subpathOrCPIds);
    }

    public void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries) {
        mgmt.get(controllerPath).setFilterEntries(filterEntries);
    }

    public Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath) {
        return mgmt.get(controllerPath).getParentPartitionKeys();
    }

    public ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath) {
        return mgmt.get(controllerPath).getFilterEntries();
    }

    public boolean hashHasSeenPartition(IntSeqKey controllerPath, int value) {
        return optionalHashes.containsKey(controllerPath.addToEnd(value));
    }

    public void hashAddPartition(IntSeqKey controllerPath, int value, int subpathIdOrCPId) {
        optionalHashes.put(controllerPath.addToEnd(value), subpathIdOrCPId);
    }

    public void hashVisit(IntSeqKey controllerPath, BiConsumer<Integer, Integer> hashAndCPId) {
        if (optionalHashes == null) {
            MgmtInfo mgmtInfo = mgmt.get(controllerPath);
            if (mgmtInfo == null || mgmtInfo.getSubpathOrCPIdsPreallocate() == null) {
                return;
            }
            int[] subpathOrCPIds = mgmtInfo.getSubpathOrCPIdsPreallocate();
            for (int i = 0; i < subpathOrCPIds.length; i++) {
                hashAndCPId.accept(i, subpathOrCPIds[i]);
            }
            return;
        }

        for (Map.Entry<IntSeqKey, Integer> entry : optionalHashes.entrySet()) {
            if (controllerPath.isParentTo(entry.getKey())) {
                hashAndCPId.accept(entry.getKey().last(), entry.getValue());
            }
        }
    }

    public int hashGetSubpathOrCPId(IntSeqKey controllerPath, int hash) {
        if (optionalHashes == null) {
            MgmtInfo mgmtInfo = mgmt.get(controllerPath);
            return mgmtInfo.getSubpathOrCPIdsPreallocate()[hash];
        }

        Integer found = optionalHashes.get(controllerPath.addToEnd(hash));
        return found == null ? -1 : found;
    }

    public Collection<Integer> deactivate(IntSeqKey controllerPath) {
        MgmtInfo mgmtInfo = mgmt.remove(controllerPath);

        if (optionalHashes == null) {
            return mgmtInfoToIds(mgmtInfo);
        }

        Iterator<Map.Entry<IntSeqKey, Integer>> it = optionalHashes.entrySet().iterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<IntSeqKey, Integer> entry = it.next();
            if (controllerPath.isParentTo(entry.getKey())) {
                result.add(entry.getValue());
                it.remove();
            }
        }
        return result;
    }

    public void destroy() {
        mgmt.clear();
        if (optionalHashes != null) {
            optionalHashes.clear();
        }
    }

    private Collection<Integer> mgmtInfoToIds(MgmtInfo mgmtInfo) {
        int[] subpathOrCPIdsPreallocate = mgmtInfo.getSubpathOrCPIdsPreallocate();
        List<Integer> ids = new ArrayList<>(subpathOrCPIdsPreallocate.length);
        for (int id : subpathOrCPIdsPreallocate) {
            ids.add(id);
        }
        return ids;
    }

    private static class MgmtInfo {
        private ContextControllerFilterEntry[] filterEntries;
        private Object[] parentPartitionKeys;
        private int[] subpathOrCPIdsPreallocate;

        MgmtInfo(ContextControllerFilterEntry[] filterEntries, Object[] parentPartitionKeys) {
            this.filterEntries = filterEntries;
            this.parentPartitionKeys = parentPartitionKeys;
        }

        ContextControllerFilterEntry[] getFilterEntries() {
            return filterEntries;
        }

        public Object[] getParentPartitionKeys() {
            return parentPartitionKeys;
        }

        void setFilterEntries(ContextControllerFilterEntry[] filterEntries) {
            this.filterEntries = filterEntries;
        }

        int[] getSubpathOrCPIdsPreallocate() {
            return subpathOrCPIdsPreallocate;
        }

        void setSubpathOrCPIdsPreallocate(int[] subpathOrCPIdsPreallocate) {
            this.subpathOrCPIdsPreallocate = subpathOrCPIdsPreallocate;
        }
    }
}
