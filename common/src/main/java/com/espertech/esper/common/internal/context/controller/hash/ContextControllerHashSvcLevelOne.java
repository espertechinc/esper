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

public class ContextControllerHashSvcLevelOne implements ContextControllerHashSvc {
    private final static Object[] EMPTY_PARENT_PARTITION_KEYS = new Object[0];

    private ContextControllerFilterEntry[] filterEntries;
    private int[] subpathOrCPIdsPreallocate;
    private Map<Integer, Integer> optionalHashes;

    public ContextControllerHashSvcLevelOne(boolean preallocate) {
        if (!preallocate) {
            optionalHashes = new HashMap<>();
        }
    }

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        // can ignore, parent partition keys always empty
    }

    public void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries) {
        this.filterEntries = filterEntries;
    }

    public int[] mgmtGetSubpathOrCPIdsWhenPreallocate(IntSeqKey path) {
        return subpathOrCPIdsPreallocate;
    }

    public void mgmtSetSubpathOrCPIdsWhenPreallocate(IntSeqKey path, int[] subpathOrCPIds) {
        subpathOrCPIdsPreallocate = subpathOrCPIds;
    }

    public Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath) {
        return EMPTY_PARENT_PARTITION_KEYS;
    }

    public ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath) {
        return filterEntries;
    }

    public boolean hashHasSeenPartition(IntSeqKey controllerPath, int value) {
        return optionalHashes.containsKey(value);
    }

    public void hashAddPartition(IntSeqKey controllerPath, int value, int subpathIdOrCPId) {
        optionalHashes.put(value, subpathIdOrCPId);
    }

    public void hashVisit(IntSeqKey controllerPath, BiConsumer<Integer, Integer> hashAndCPId) {
        if (optionalHashes == null) {
            if (subpathOrCPIdsPreallocate == null) {
                return;
            }
            for (int i = 0; i < subpathOrCPIdsPreallocate.length; i++) {
                hashAndCPId.accept(i, subpathOrCPIdsPreallocate[i]);
            }
            return;
        }

        for (Map.Entry<Integer, Integer> entry : optionalHashes.entrySet()) {
            hashAndCPId.accept(entry.getKey(), entry.getValue());
        }
    }

    public int hashGetSubpathOrCPId(IntSeqKey controllerPath, int hash) {
        if (optionalHashes == null) {
            if (hash >= subpathOrCPIdsPreallocate.length) {
                return -1;
            }
            return subpathOrCPIdsPreallocate[hash];
        }

        Integer entry = optionalHashes.get(hash);
        return entry == null ? -1 : entry;
    }

    public Collection<Integer> deactivate(IntSeqKey controllerPath) {
        if (optionalHashes == null) {
            List<Integer> ids = new ArrayList<>(subpathOrCPIdsPreallocate.length);
            for (int id : subpathOrCPIdsPreallocate) {
                ids.add(id);
            }
            return ids;
        }

        List<Integer> ids = new ArrayList<>(optionalHashes.values());
        optionalHashes.clear();
        return ids;
    }

    public void destroy() {
        if (optionalHashes != null) {
            optionalHashes.clear();
        }
        subpathOrCPIdsPreallocate = null;
        filterEntries = null;
    }
}
