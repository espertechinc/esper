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

public class ContextManagerNestedInstanceHandle implements ContextControllerInstanceHandle {
    private final int subPathId;
    private final ContextController controller;
    private final int contextPartitionOrPathId;
    private final boolean branch;
    private final ContextControllerTreeAgentInstanceList branchAgentInstances;

    public ContextManagerNestedInstanceHandle(int subPathId, ContextController controller, int contextPartitionOrPathId, boolean branch, ContextControllerTreeAgentInstanceList branchAgentInstances) {
        this.subPathId = subPathId;
        this.controller = controller;
        this.contextPartitionOrPathId = contextPartitionOrPathId;
        this.branch = branch;
        this.branchAgentInstances = branchAgentInstances;
    }

    public int getSubPathId() {
        return subPathId;
    }

    public ContextController getController() {
        return controller;
    }

    public Integer getContextPartitionOrPathId() {
        return contextPartitionOrPathId;
    }

    public boolean isBranch() {
        return branch;
    }

    public ContextControllerTreeAgentInstanceList getInstances() {
        return branchAgentInstances;
    }
}
