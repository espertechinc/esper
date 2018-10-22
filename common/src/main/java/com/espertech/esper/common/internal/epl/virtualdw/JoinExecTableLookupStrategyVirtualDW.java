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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowKeyRange;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowLookup;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeIn;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeRelOp;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class JoinExecTableLookupStrategyVirtualDW implements JoinExecTableLookupStrategy {
    private static final Logger log = LoggerFactory.getLogger(JoinExecTableLookupStrategyVirtualDW.class);

    private final String namedWindowName;
    private final VirtualDataWindowLookup externalIndex;
    private final ExternalEvaluator[] evaluators;
    private final EventBean[] eventsPerStream;
    private final int lookupStream;

    public JoinExecTableLookupStrategyVirtualDW(String namedWindowName, VirtualDataWindowLookup externalIndex, TableLookupPlan tableLookupPlan) {
        this.namedWindowName = namedWindowName;
        this.externalIndex = externalIndex;
        this.lookupStream = tableLookupPlan.getLookupStream();

        ExprEvaluator[] hashKeys = tableLookupPlan.getVirtualDWHashEvals();
        if (hashKeys == null) {
            hashKeys = new ExprEvaluator[0];
        }
        QueryGraphValueEntryRange[] rangeKeys = tableLookupPlan.getVirtualDWRangeEvals();
        if (rangeKeys == null) {
            rangeKeys = new QueryGraphValueEntryRange[0];
        }

        this.evaluators = new ExternalEvaluator[hashKeys.length + rangeKeys.length];
        this.eventsPerStream = new EventBean[lookupStream + 1];

        int count = 0;
        for (ExprEvaluator hashKey : hashKeys) {
            evaluators[count] = new ExternalEvaluatorHashRelOp(hashKey);
            count++;
        }
        for (QueryGraphValueEntryRange rangeKey : rangeKeys) {
            if (rangeKey.getType().isRange()) {
                QueryGraphValueEntryRangeIn range = (QueryGraphValueEntryRangeIn) rangeKey;
                ExprEvaluator evaluatorStart = range.getExprStart();
                ExprEvaluator evaluatorEnd = range.getExprEnd();
                evaluators[count] = new ExternalEvaluatorBtreeRange(evaluatorStart, evaluatorEnd);
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKey;
                ExprEvaluator evaluator = relOp.getExpression();
                evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator);
            }
            count++;
        }
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext context) {

        eventsPerStream[lookupStream] = theEvent;

        Object[] keys = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            keys[i] = evaluators[i].evaluate(eventsPerStream, context);
        }

        Set<EventBean> events = null;
        try {
            events = externalIndex.lookup(keys, eventsPerStream);
        } catch (RuntimeException ex) {
            log.warn("Exception encountered invoking virtual data window external index for window '" + namedWindowName + "': " + ex.getMessage(), ex);
        }

        return events;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " external index " + externalIndex;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.VDW);
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.VDW;
    }

    private interface ExternalEvaluator {
        public Object evaluate(EventBean[] events, ExprEvaluatorContext context);
    }

    private static class ExternalEvaluatorHashRelOp implements ExternalEvaluator {

        private final ExprEvaluator hashKeysEval;

        private ExternalEvaluatorHashRelOp(ExprEvaluator hashKeysEval) {
            this.hashKeysEval = hashKeysEval;
        }

        public Object evaluate(EventBean[] events, ExprEvaluatorContext context) {
            return hashKeysEval.evaluate(events, true, context);
        }
    }

    private static class ExternalEvaluatorBtreeRange implements ExternalEvaluator {

        private final ExprEvaluator startEval;
        private final ExprEvaluator endEval;

        private ExternalEvaluatorBtreeRange(ExprEvaluator startEval, ExprEvaluator endEval) {
            this.startEval = startEval;
            this.endEval = endEval;
        }

        public Object evaluate(EventBean[] events, ExprEvaluatorContext context) {
            Object start = startEval.evaluate(events, true, context);
            Object end = endEval.evaluate(events, true, context);
            return new VirtualDataWindowKeyRange(start, end);
        }
    }
}
