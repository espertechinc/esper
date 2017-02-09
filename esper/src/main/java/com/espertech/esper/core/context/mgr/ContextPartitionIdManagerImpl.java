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
package com.espertech.esper.core.context.mgr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ContextPartitionIdManagerImpl implements ContextPartitionIdManager {

    private final Set<Integer> cpids = new HashSet<Integer>();
    private int lastAssignedId = -1;

    public void clear() {
        cpids.clear();
    }

    public void addExisting(int contextPartitionId) {
        cpids.add(contextPartitionId);
    }

    public int allocateId() {
        while (true) {
            if (lastAssignedId < Integer.MAX_VALUE) {
                lastAssignedId++;
            } else {
                lastAssignedId = 0;
            }

            if (!cpids.contains(lastAssignedId)) {
                cpids.add(lastAssignedId);
                return lastAssignedId;
            }
        }
    }

    public void removeId(int contextPartitionId) {
        cpids.remove(contextPartitionId);
    }

    public Collection<Integer> getIds() {
        return cpids;
    }
}
