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
package com.espertech.esper.common.internal.context.cpidsvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContextPartitionIdServiceImpl implements ContextPartitionIdService {
    private final Map<Integer, Object[]> cpids = new HashMap<Integer, Object[]>();
    private int lastAssignedId = -1;

    public ContextPartitionIdServiceImpl() {
    }

    public void clear() {
        cpids.clear();
        lastAssignedId = -1;
    }

    public Object[] getPartitionKeys(int id) {
        return cpids.get(id);
    }

    public int allocateId(Object[] partitionKeys) {
        while (true) {
            if (lastAssignedId < Integer.MAX_VALUE) {
                lastAssignedId++;
            } else {
                lastAssignedId = 0;
            }

            if (!cpids.containsKey(lastAssignedId)) {
                cpids.put(lastAssignedId, partitionKeys);
                return lastAssignedId;
            }
        }
    }

    public void removeId(int contextPartitionId) {
        cpids.remove(contextPartitionId);
    }

    public Collection<Integer> getIds() {
        return new ArrayList<>(cpids.keySet());
    }

    public void destroy() {
        // no action, service discarded and GC'd
    }

    public void clearCaches() {
        // not memory-managed
    }

    public long getCount() {
        return cpids.size();
    }
}
