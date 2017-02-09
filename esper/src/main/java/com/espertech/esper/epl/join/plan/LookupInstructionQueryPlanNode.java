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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.assemble.BaseAssemblyNodeFactory;
import com.espertech.esper.epl.join.exec.base.ExecNode;
import com.espertech.esper.epl.join.exec.base.LookupInstructionExec;
import com.espertech.esper.epl.join.exec.base.LookupInstructionExecNode;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.HistoricalStreamIndexList;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.util.IndentWriter;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    private final List<LookupInstructionPlan> lookupInstructions;
    private final boolean[] requiredPerStream;
    private final List<BaseAssemblyNodeFactory> assemblyInstructionFactories;

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
                                          List<LookupInstructionPlan> lookupInstructions,
                                          List<BaseAssemblyNodeFactory> assemblyInstructionFactories) {
        this.rootStream = rootStream;
        this.rootStreamName = rootStreamName;
        this.lookupInstructions = lookupInstructions;
        this.numStreams = numStreams;
        this.requiredPerStream = requiredPerStream;
        this.assemblyInstructionFactories = assemblyInstructionFactories;
    }

    public ExecNode makeExec(String statementName, int statementId, Annotation[] annotations, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] streamTypes, Viewable[] streamViews, HistoricalStreamIndexList[] historicalStreamIndexLists, VirtualDWView[] viewExternal, Lock[] tableSecondaryIndexLocks) {
        LookupInstructionExec[] execs = new LookupInstructionExec[lookupInstructions.size()];

        int count = 0;
        for (LookupInstructionPlan instruction : lookupInstructions) {
            LookupInstructionExec exec = instruction.makeExec(statementName, statementId, annotations, indexesPerStream, streamTypes, streamViews, historicalStreamIndexLists, viewExternal);
            execs[count] = exec;
            count++;
        }

        return new LookupInstructionExecNode(rootStream, rootStreamName,
                numStreams, execs, requiredPerStream, assemblyInstructionFactories);
    }

    @Override
    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        for (LookupInstructionPlan plan : lookupInstructions) {
            plan.addIndexes(usedIndexes);
        }
    }

    protected void print(IndentWriter writer) {
        writer.println("LookupInstructionQueryPlanNode" +
                " rootStream=" + rootStream +
                " requiredPerStream=" + Arrays.toString(requiredPerStream));

        writer.incrIndent();
        for (int i = 0; i < lookupInstructions.size(); i++) {
            writer.println("lookup step " + i);
            writer.incrIndent();
            lookupInstructions.get(i).print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();

        writer.incrIndent();
        for (int i = 0; i < assemblyInstructionFactories.size(); i++) {
            writer.println("assembly step " + i);
            writer.incrIndent();
            assemblyInstructionFactories.get(i).print(writer);
            writer.decrIndent();
        }
        writer.decrIndent();
    }

    public int getRootStream() {
        return rootStream;
    }

    public String getRootStreamName() {
        return rootStreamName;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public List<LookupInstructionPlan> getLookupInstructions() {
        return lookupInstructions;
    }

    public boolean[] getRequiredPerStream() {
        return requiredPerStream;
    }

    public List<BaseAssemblyNodeFactory> getAssemblyInstructionFactories() {
        return assemblyInstructionFactories;
    }
}
