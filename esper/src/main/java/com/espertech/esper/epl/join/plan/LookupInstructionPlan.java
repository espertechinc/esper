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
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.LookupInstructionExec;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.HistoricalStreamIndexList;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.util.IndentWriter;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Plan for lookup using a from-stream event looking up one or more to-streams using a specified lookup plan for each
 * to-stream.
 */
public class LookupInstructionPlan {
    private final int fromStream;
    private final String fromStreamName;
    private final int[] toStreams;
    private final TableLookupPlan[] lookupPlans;
    private final boolean[] requiredPerStream;
    private final HistoricalDataPlanNode[] historicalPlans;

    /**
     * Ctor.
     *
     * @param fromStream        - the stream supplying the lookup event
     * @param fromStreamName    - the stream name supplying the lookup event
     * @param toStreams         - the set of streams to look up in
     * @param lookupPlans       - the plan to use for each stream to look up in
     * @param requiredPerStream - indicates which of the lookup streams are required to build a result and which are not
     * @param historicalPlans   - plans for use with historical streams
     */
    public LookupInstructionPlan(int fromStream, String fromStreamName, int[] toStreams, TableLookupPlan[] lookupPlans, HistoricalDataPlanNode[] historicalPlans, boolean[] requiredPerStream) {
        if (toStreams.length != lookupPlans.length) {
            throw new IllegalArgumentException("Invalid number of lookup plans for each stream");
        }
        if (requiredPerStream.length < lookupPlans.length) {
            throw new IllegalArgumentException("Invalid required per stream array");
        }
        if ((fromStream < 0) || (fromStream >= requiredPerStream.length)) {
            throw new IllegalArgumentException("Invalid from stream");
        }

        this.fromStream = fromStream;
        this.fromStreamName = fromStreamName;
        this.toStreams = toStreams;
        this.lookupPlans = lookupPlans;
        this.historicalPlans = historicalPlans;
        this.requiredPerStream = requiredPerStream;
    }

    /**
     * Constructs the executable from the plan.
     *
     * @param statementName              statement name
     * @param statementId                statement id
     * @param annotations                annotations
     * @param indexesPerStream           is the index objects for use in lookups
     * @param streamTypes                is the types of each stream
     * @param streamViews                the viewable representing each stream
     * @param historicalStreamIndexLists index management for historical streams     @return executable instruction
     * @param viewExternal               virtual data window
     * @return instruction exec
     */
    public LookupInstructionExec makeExec(String statementName, int statementId, Annotation[] annotations, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] streamTypes, Viewable[] streamViews, HistoricalStreamIndexList[] historicalStreamIndexLists, VirtualDWView[] viewExternal) {
        JoinExecTableLookupStrategy[] strategies = new JoinExecTableLookupStrategy[lookupPlans.length];
        for (int i = 0; i < lookupPlans.length; i++) {
            if (lookupPlans[i] != null) {
                strategies[i] = lookupPlans[i].makeStrategy(statementName, statementId, annotations, indexesPerStream, streamTypes, viewExternal);
            } else {
                strategies[i] = historicalPlans[i].makeOuterJoinStategy(streamViews, fromStream, historicalStreamIndexLists);
            }
        }
        return new LookupInstructionExec(fromStream, fromStreamName, toStreams, strategies, requiredPerStream);
    }

    /**
     * Output the planned instruction.
     *
     * @param writer to output to
     */
    public void print(IndentWriter writer) {
        writer.println("LookupInstructionPlan" +
                " fromStream=" + fromStream +
                " fromStreamName=" + fromStreamName +
                " toStreams=" + Arrays.toString(toStreams)
        );

        writer.incrIndent();
        for (int i = 0; i < lookupPlans.length; i++) {
            if (lookupPlans[i] != null) {
                writer.println("plan " + i + " :" + lookupPlans[i].toString());
            } else {
                writer.println("plan " + i + " : no lookup plan");
            }
        }
        writer.decrIndent();
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        for (int i = 0; i < lookupPlans.length; i++) {
            if (lookupPlans[i] != null) {
                usedIndexes.addAll(Arrays.asList(lookupPlans[i].getIndexNum()));
            }
        }
    }

    public int getFromStream() {
        return fromStream;
    }

    public String getFromStreamName() {
        return fromStreamName;
    }

    public int[] getToStreams() {
        return toStreams;
    }

    public TableLookupPlan[] getLookupPlans() {
        return lookupPlans;
    }

    public boolean[] getRequiredPerStream() {
        return requiredPerStream;
    }

    public HistoricalDataPlanNode[] getHistoricalPlans() {
        return historicalPlans;
    }
}
