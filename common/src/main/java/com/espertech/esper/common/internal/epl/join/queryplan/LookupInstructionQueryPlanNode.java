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
import com.espertech.esper.common.internal.epl.join.assemble.BaseAssemblyNodeFactory;
import com.espertech.esper.common.internal.epl.join.exec.outer.LookupInstructionExec;
import com.espertech.esper.common.internal.epl.join.exec.outer.LookupInstructionExecNode;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlan;
import com.espertech.esper.common.internal.epl.join.strategy.ExecNode;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Query plan for executing a set of lookup instructions and assembling an end result via
 * a set of assembly instructions.
 */
public class LookupInstructionQueryPlanNode extends QueryPlanNode {
    private final int rootStream;
    private final String rootStreamName;
    private final int numStreams;
    private final LookupInstructionPlan[] lookupInstructions;
    private final boolean[] requiredPerStream;
    private final BaseAssemblyNodeFactory[] assemblyInstructionFactories;

    /**
     * Ctor.
     *
     * @param rootStream                   is the stream supplying the lookup event
     * @param rootStreamName               is the name of the stream supplying the lookup event
     * @param numStreams                   is the number of streams
     * @param lookupInstructions           is a list of lookups to perform
     * @param requiredPerStream            indicates which streams are required and which are optional in the lookup
     * @param assemblyInstructionFactories is the bottom-up assembly factory nodes to assemble a lookup result nodes
     */
    public LookupInstructionQueryPlanNode(int rootStream,
                                          String rootStreamName,
                                          int numStreams,
                                          boolean[] requiredPerStream,
                                          LookupInstructionPlan[] lookupInstructions,
                                          BaseAssemblyNodeFactory[] assemblyInstructionFactories) {
        this.rootStream = rootStream;
        this.rootStreamName = rootStreamName;
        this.lookupInstructions = lookupInstructions;
        this.numStreams = numStreams;
        this.requiredPerStream = requiredPerStream;
        this.assemblyInstructionFactories = assemblyInstructionFactories;
    }

    public ExecNode makeExec(AgentInstanceContext agentInstanceContext, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] streamTypes, Viewable[] streamViews, VirtualDWView[] viewExternal, Lock[] tableSecondaryIndexLocks) {
        LookupInstructionExec[] execs = new LookupInstructionExec[lookupInstructions.length];

        int count = 0;
        for (LookupInstructionPlan instruction : lookupInstructions) {
            LookupInstructionExec exec = instruction.makeExec(agentInstanceContext, indexesPerStream, streamTypes, streamViews, viewExternal);
            execs[count] = exec;
            count++;
        }

        return new LookupInstructionExecNode(rootStream, rootStreamName,
                numStreams, execs, requiredPerStream, assemblyInstructionFactories);
    }

    public int getNumStreams() {
        return numStreams;
    }

    public LookupInstructionPlan[] getLookupInstructions() {
        return lookupInstructions;
    }
}
