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
package com.espertech.esper.common.internal.epl.join.queryplanouter;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.util.Arrays;
import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Plan for lookup using a from-stream event looking up one or more to-streams using a specified lookup plan for each
 * to-stream.
 */
public class LookupInstructionPlanForge implements CodegenMakeable<SAIFFInitializeSymbol> {
    private final int fromStream;
    private final String fromStreamName;
    private final int[] toStreams;
    private final TableLookupPlanForge[] lookupPlans;
    private final boolean[] requiredPerStream;
    private final HistoricalDataPlanNodeForge[] historicalPlans;

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
    public LookupInstructionPlanForge(int fromStream, String fromStreamName, int[] toStreams, TableLookupPlanForge[] lookupPlans, HistoricalDataPlanNodeForge[] historicalPlans, boolean[] requiredPerStream) {
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

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(LookupInstructionPlan.class,
                constant(fromStream),
                constant(fromStreamName),
                constant(toStreams),
                CodegenMakeableUtil.makeArray("lookupPlans", TableLookupPlan.class, lookupPlans, this.getClass(), parent, symbols, classScope),
                CodegenMakeableUtil.makeArray("historicalPlans", HistoricalDataPlanNode.class, historicalPlans, this.getClass(), parent, symbols, classScope),
                constant(requiredPerStream));
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

    public TableLookupPlanForge[] getLookupPlans() {
        return lookupPlans;
    }

    public boolean[] getRequiredPerStream() {
        return requiredPerStream;
    }

    public HistoricalDataPlanNodeForge[] getHistoricalPlans() {
        return historicalPlans;
    }
}
