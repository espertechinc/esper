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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategy;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategy;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.HistoricalDataExecNode;
import com.espertech.esper.common.internal.epl.join.exec.base.HistoricalTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.strategy.ExecNode;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.util.IndentWriter;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Query plan for performing a historical data lookup.
 * <p>
 * Translates into a particular execution for use in regular and outer joins.
 */
public class HistoricalDataPlanNode extends QueryPlanNode {
    private int numStreams = -1;
    private int streamNum = -1;
    private int rootStreamNum = -1;
    private ExprEvaluator outerJoinExprEval;
    private PollResultIndexingStrategy indexingStrategy;
    private HistoricalIndexLookupStrategy lookupStrategy;

    public ExecNode makeExec(AgentInstanceContext agentInstanceContext, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] streamTypes, Viewable[] streamViews, VirtualDWView[] viewExternal, Lock[] tableSecondaryIndexLocks) {
        HistoricalEventViewable viewable = (HistoricalEventViewable) streamViews[streamNum];
        return new HistoricalDataExecNode(viewable, indexingStrategy, lookupStrategy, numStreams, streamNum);
    }

    /**
     * Returns the table lookup strategy for use in outer joins.
     *
     * @param streamViews all views in join
     * @return strategy
     */
    public HistoricalTableLookupStrategy makeOuterJoinStategy(Viewable[] streamViews) {
        HistoricalEventViewable viewable = (HistoricalEventViewable) streamViews[streamNum];
        return new HistoricalTableLookupStrategy(viewable, indexingStrategy, lookupStrategy, numStreams, streamNum, rootStreamNum, outerJoinExprEval);
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        // none to add
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setNumStreams(int numStreams) {
        this.numStreams = numStreams;
    }

    public void setLookupStrategy(HistoricalIndexLookupStrategy lookupStrategy) {
        this.lookupStrategy = lookupStrategy;
    }

    public void setIndexingStrategy(PollResultIndexingStrategy indexingStrategy) {
        this.indexingStrategy = indexingStrategy;
    }

    public void setRootStreamNum(int rootStreamNum) {
        this.rootStreamNum = rootStreamNum;
    }

    public void setOuterJoinExprEval(ExprEvaluator outerJoinExprEval) {
        this.outerJoinExprEval = outerJoinExprEval;
    }

    protected void print(IndentWriter writer) {
        writer.incrIndent();
        writer.println("HistoricalDataPlanNode streamNum=" + streamNum);
    }
}
