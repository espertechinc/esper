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
package com.espertech.esper.epl.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindowKeyRange;
import com.espertech.esper.client.hook.VirtualDataWindowLookup;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
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

    public JoinExecTableLookupStrategyVirtualDW(String namedWindowName, VirtualDataWindowLookup externalIndex, TableLookupKeyDesc keyDescriptor, int lookupStream) {
        this.namedWindowName = namedWindowName;
        this.externalIndex = externalIndex;
        this.evaluators = new ExternalEvaluator[keyDescriptor.getHashes().size() + keyDescriptor.getRanges().size()];
        this.eventsPerStream = new EventBean[lookupStream + 1];
        this.lookupStream = lookupStream;

        int count = 0;
        for (QueryGraphValueEntryHashKeyed hashKey : keyDescriptor.getHashes()) {
            ExprEvaluator evaluator = hashKey.getKeyExpr().getForge().getExprEvaluator();
            evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator);
            count++;
        }
        for (QueryGraphValueEntryRange rangeKey : keyDescriptor.getRanges()) {
            if (rangeKey.getType().isRange()) {
                QueryGraphValueEntryRangeIn range = (QueryGraphValueEntryRangeIn) rangeKey;
                ExprEvaluator evaluatorStart = range.getExprStart().getForge().getExprEvaluator();
                ExprEvaluator evaluatorEnd = range.getExprEnd().getForge().getExprEvaluator();
                evaluators[count] = new ExternalEvaluatorBtreeRange(evaluatorStart, evaluatorEnd);
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKey;
                ExprEvaluator evaluator = relOp.getExpression().getForge().getExprEvaluator();
                evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator);
            }
            count++;
        }
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext context) {

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexJoinLookup(this, null);
        }

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

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexJoinLookup(events, null);
        }
        return events;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " external index " + externalIndex;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.VDW, null);
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
