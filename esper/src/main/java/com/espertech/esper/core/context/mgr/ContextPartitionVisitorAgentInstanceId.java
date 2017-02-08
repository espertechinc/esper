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

import java.util.ArrayList;

public class ContextPartitionVisitorAgentInstanceId implements ContextPartitionVisitor {

    private final int maxNestingLevel;
    private final ArrayList<Integer> agentInstanceIds = new ArrayList<Integer>();

    public ContextPartitionVisitorAgentInstanceId(int maxNestingLevel) {
        this.maxNestingLevel = maxNestingLevel;
    }

    public void visit(int nestingLevel, int pathId, ContextStatePathValueBinding binding, Object payload, ContextController contextController, ContextControllerInstanceHandle instanceHandle) {
        if (nestingLevel == maxNestingLevel) {
            agentInstanceIds.add(instanceHandle.getContextPartitionOrPathId());
        }
    }

    public ArrayList<Integer> getAgentInstanceIds() {
        return agentInstanceIds;
    }
}
