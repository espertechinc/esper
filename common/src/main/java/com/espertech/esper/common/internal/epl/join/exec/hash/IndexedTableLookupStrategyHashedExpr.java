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
package com.espertech.esper.common.internal.epl.join.exec.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.IndexedTableLookupPlanHashedOnlyFactory;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Set;

public class IndexedTableLookupStrategyHashedExpr implements JoinExecTableLookupStrategy {
    private final IndexedTableLookupPlanHashedOnlyFactory factory;
    private final PropertyHashedEventTable index;
    private final EventBean[] eventsPerStream;

    public IndexedTableLookupStrategyHashedExpr(IndexedTableLookupPlanHashedOnlyFactory factory, PropertyHashedEventTable index, int numStreams) {
        this.factory = factory;
        this.index = index;
        this.eventsPerStream = new EventBean[numStreams + 1];
    }

    public PropertyHashedEventTable getIndex() {
        return index;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qIndexJoinLookup(this, index);

        eventsPerStream[factory.getLookupStream()] = theEvent;
        Object key = factory.getExprEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
        Set<EventBean> result = index.lookup(key);

        instrumentationCommon.aIndexJoinLookup(result, key);
        return result;
    }

    public String toString() {
        return "IndexedTableLookupStrategySingleExpr evaluation" +
                " index=(" + index + ')';
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.MULTIEXPR;
    }
}
