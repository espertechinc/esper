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
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

import java.util.Map;

/**
 * Abstract specification on how to perform a table lookup.
 */
public abstract class TableLookupPlan {
    protected final int lookupStream;
    protected final int indexedStream;
    protected final TableLookupIndexReqKey[] indexNum;

    private ExprEvaluator[] virtualDWHashEvals;
    private Class[] virtualDWHashTypes;
    private QueryGraphValueEntryRange[] virtualDWRangeEvals;
    private Class[] virtualDWRangeTypes;

    protected abstract JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTables, EventType[] eventTypes);

    public TableLookupPlan(int lookupStream, int indexedStream, TableLookupIndexReqKey[] indexNum) {
        this.lookupStream = lookupStream;
        this.indexedStream = indexedStream;
        this.indexNum = indexNum;
    }

    public final JoinExecTableLookupStrategy makeStrategy(AgentInstanceContext agentInstanceContext, Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, EventType[] eventTypes, VirtualDWView[] viewExternals) {
        EventTable[] eventTables = new EventTable[indexNum.length];
        for (int i = 0; i < indexNum.length; i++) {
            eventTables[i] = indexesPerStream[indexedStream].get(getIndexNum()[i]);
        }
        if (viewExternals[indexedStream] != null) {
            return viewExternals[indexedStream].getJoinLookupStrategy(this, agentInstanceContext, eventTables, lookupStream);
        }
        return makeStrategyInternal(eventTables, eventTypes);
    }

    public int getLookupStream() {
        return lookupStream;
    }

    public int getIndexedStream() {
        return indexedStream;
    }

    public TableLookupIndexReqKey[] getIndexNum() {
        return indexNum;
    }

    public ExprEvaluator[] getVirtualDWHashEvals() {
        return virtualDWHashEvals;
    }

    public void setVirtualDWHashEvals(ExprEvaluator[] virtualDWHashEvals) {
        this.virtualDWHashEvals = virtualDWHashEvals;
    }

    public Class[] getVirtualDWHashTypes() {
        return virtualDWHashTypes;
    }

    public void setVirtualDWHashTypes(Class[] virtualDWHashTypes) {
        this.virtualDWHashTypes = virtualDWHashTypes;
    }

    public QueryGraphValueEntryRange[] getVirtualDWRangeEvals() {
        return virtualDWRangeEvals;
    }

    public void setVirtualDWRangeEvals(QueryGraphValueEntryRange[] virtualDWRangeEvals) {
        this.virtualDWRangeEvals = virtualDWRangeEvals;
    }

    public Class[] getVirtualDWRangeTypes() {
        return virtualDWRangeTypes;
    }

    public void setVirtualDWRangeTypes(Class[] virtualDWRangeTypes) {
        this.virtualDWRangeTypes = virtualDWRangeTypes;
    }
}
