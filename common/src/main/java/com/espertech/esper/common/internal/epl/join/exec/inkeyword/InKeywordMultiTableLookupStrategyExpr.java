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
import com.espertech.esper.common.internal.epl.join.indexlookupplan.InKeywordTableLookupPlanMultiIdxFactory;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Set;

/**
 * Lookup on an index using a set of expression results as key values.
 */
public class InKeywordMultiTableLookupStrategyExpr implements JoinExecTableLookupStrategy {
    private final InKeywordTableLookupPlanMultiIdxFactory factory;
    private final PropertyHashedEventTable[] indexes;
    private final EventBean[] eventsPerStream;

    public InKeywordMultiTableLookupStrategyExpr(InKeywordTableLookupPlanMultiIdxFactory factory, PropertyHashedEventTable[] indexes) {
        this.factory = factory;
        this.indexes = indexes;
        this.eventsPerStream = new EventBean[factory.getLookupStream() + 1];
    }

    public PropertyHashedEventTable[] getIndex() {
        return indexes;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qIndexJoinLookup(this, indexes[0]);

        eventsPerStream[factory.getLookupStream()] = theEvent;
        Set<EventBean> result = InKeywordTableLookupUtil.multiIndexLookup(factory.getKeyExpr(), eventsPerStream, exprEvaluatorContext, indexes);

        instrumentationCommon.aIndexJoinLookup(result, null);

        return result;
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.INKEYWORDMULTIIDX;
    }
}
