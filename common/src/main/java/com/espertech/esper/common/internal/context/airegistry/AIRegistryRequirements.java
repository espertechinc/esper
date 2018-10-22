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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.internal.epl.subselect.SubSelectFactory;

import java.util.Map;

public class AIRegistryRequirements {
    private final boolean[] priorFlagsPerStream;
    private final boolean[] previousFlagsPerStream;
    private final AIRegistryRequirementSubquery[] subqueries;
    private final int tableAccessCount;
    private final boolean isRowRecogWithPrevious;

    public static AIRegistryRequirements noRequirements() {
        return new AIRegistryRequirements(null, null, null, 0, false);
    }

    public AIRegistryRequirements(boolean[] priorFlagsPerStream, boolean[] previousFlagsPerStream, AIRegistryRequirementSubquery[] subqueries, int tableAccessCount, boolean isRowRecogWithPrevious) {
        this.priorFlagsPerStream = priorFlagsPerStream;
        this.previousFlagsPerStream = previousFlagsPerStream;
        this.subqueries = subqueries;
        this.tableAccessCount = tableAccessCount;
        this.isRowRecogWithPrevious = isRowRecogWithPrevious;
    }

    public boolean[] getPriorFlagsPerStream() {
        return priorFlagsPerStream;
    }

    public boolean[] getPreviousFlagsPerStream() {
        return previousFlagsPerStream;
    }

    public AIRegistryRequirementSubquery[] getSubqueries() {
        return subqueries;
    }

    public int getTableAccessCount() {
        return tableAccessCount;
    }

    public boolean isRowRecogWithPrevious() {
        return isRowRecogWithPrevious;
    }

    public static AIRegistryRequirementSubquery[] getSubqueryRequirements(Map<Integer, SubSelectFactory> subselects) {
        if (subselects == null || subselects.isEmpty()) {
            return null;
        }
        AIRegistryRequirementSubquery[] subqueries = new AIRegistryRequirementSubquery[subselects.size()];
        for (Map.Entry<Integer, SubSelectFactory> entry : subselects.entrySet()) {
            subqueries[entry.getKey()] = entry.getValue().getRegistryRequirements();
        }
        return subqueries;
    }
}
