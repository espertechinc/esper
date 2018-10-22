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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.NestedIterationExecNode;
import com.espertech.esper.common.internal.epl.join.strategy.ExecNode;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Plan to perform a nested iteration over child nodes.
 */
public class NestedIterationNode extends QueryPlanNode {
    private final QueryPlanNode[] childNodes;
    private final int[] nestingOrder;

    public NestedIterationNode(QueryPlanNode[] childNodes, int[] nestingOrder) {
        this.childNodes = childNodes;
        this.nestingOrder = nestingOrder;
    }

    public ExecNode makeExec(AgentInstanceContext agentInstanceContext, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] streamTypes, Viewable[] streamViews, VirtualDWView[] viewExternal, Lock[] tableSecondaryIndexLocks) {
        NestedIterationExecNode execNode = new NestedIterationExecNode(nestingOrder);
        for (QueryPlanNode child : childNodes) {
            ExecNode childExec = child.makeExec(agentInstanceContext, indexesPerStream, streamTypes, streamViews, viewExternal, tableSecondaryIndexLocks);
            execNode.addChildNode(childExec);
        }
        return execNode;
    }
}
