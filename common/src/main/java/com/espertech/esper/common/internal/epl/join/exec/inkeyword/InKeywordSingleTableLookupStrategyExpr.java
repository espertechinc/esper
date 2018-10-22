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
package com.espertech.esper.common.internal.epl.join.exec.inkeyword;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.InKeywordTableLookupPlanSingleIdxFactory;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Set;

/**
 * Lookup on an index using a set of expression results as key values.
 */
public class InKeywordSingleTableLookupStrategyExpr implements JoinExecTableLookupStrategy {
    private final InKeywordTableLookupPlanSingleIdxFactory factory;
    private final PropertyHashedEventTable index;
    private final EventBean[] eventsPerStream;

    public InKeywordSingleTableLookupStrategyExpr(InKeywordTableLookupPlanSingleIdxFactory factory, PropertyHashedEventTable index) {
        this.factory = factory;
        this.index = index;
        this.eventsPerStream = new EventBean[factory.getLookupStream() + 1];
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qIndexJoinLookup(this, index);

        eventsPerStream[factory.getLookupStream()] = theEvent;
        Set<EventBean> result = InKeywordTableLookupUtil.singleIndexLookup(factory.getExpressions(), eventsPerStream, exprEvaluatorContext, index);

        instrumentationCommon.aIndexJoinLookup(result, null);
        return result;
    }

    public String toString() {
        return "IndexedTableLookupStrategyExpr expressions" +
                " index=(" + index + ')';
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.INKEYWORDSINGLEIDX;
    }
}
