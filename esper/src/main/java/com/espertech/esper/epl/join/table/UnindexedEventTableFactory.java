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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

/**
 * Factory for simple table of events without an index.
 */
public class UnindexedEventTableFactory implements EventTableFactory {
    protected final int streamNum;

    public UnindexedEventTableFactory(int streamNum) {
        this.streamNum = streamNum;
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        return new EventTable[]{new UnindexedEventTableImpl(streamNum)};
    }

    public Class getEventTableClass() {
        return UnindexedEventTable.class;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " streamNum=" + streamNum;
    }
}
