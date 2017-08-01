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
import com.espertech.esper.epl.join.plan.CoercionDesc;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeIn;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeRelOp;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.event.EventBeanUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SubordTableLookupStrategyVirtualDW implements SubordTableLookupStrategy {

    private static final Logger log = LoggerFactory.getLogger(SubordTableLookupStrategyVirtualDW.class);

    private final String namedWindowName;
    private final VirtualDataWindowLookup externalIndex;
    private final ExternalEvaluator[] evaluators;
    private final boolean nwOnTrigger;
    private final EventBean[] eventsLocal;

    public SubordTableLookupStrategyVirtualDW(String namedWindowName, VirtualDataWindowLookup externalIndex, List<SubordPropHashKey> hashKeys, CoercionDesc hashKeyCoercionTypes, List<SubordPropRangeKey> rangeKeys, CoercionDesc rangeKeyCoercionTypes, boolean nwOnTrigger, int numOuterStreams) {
        this.namedWindowName = namedWindowName;
        this.externalIndex = externalIndex;
        this.evaluators = new ExternalEvaluator[hashKeys.size() + rangeKeys.size()];
        this.nwOnTrigger = nwOnTrigger;
        this.eventsLocal = new EventBean[numOuterStreams + 1];

        int count = 0;
        for (SubordPropHashKey hashKey : hashKeys) {
            ExprEvaluator evaluator = hashKey.getHashKey().getKeyExpr().getForge().getExprEvaluator();
            evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator, hashKeyCoercionTypes.getCoercionTypes()[count]);
            count++;
        }
        for (int i = 0; i < rangeKeys.size(); i++) {
            SubordPropRangeKey rangeKey = rangeKeys.get(i);
            if (rangeKey.getRangeInfo().getType().isRange()) {
                QueryGraphValueEntryRangeIn range = (QueryGraphValueEntryRangeIn) rangeKey.getRangeInfo();
                ExprEvaluator evaluatorStart = range.getExprStart().getForge().getExprEvaluator();
                ExprEvaluator evaluatorEnd = range.getExprEnd().getForge().getExprEvaluator();
                evaluators[count] = new ExternalEvaluatorBtreeRange(evaluatorStart, evaluatorEnd, rangeKeyCoercionTypes.getCoercionTypes()[i]);
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKey.getRangeInfo();
                ExprEvaluator evaluator = relOp.getExpression().getForge().getExprEvaluator();
                evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator, rangeKeyCoercionTypes.getCoercionTypes()[i]);
            }
            count++;
        }
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        EventBean[] events;
        if (nwOnTrigger) {
            events = eventsPerStream;
        } else {
            System.arraycopy(eventsPerStream, 0, eventsLocal, 1, eventsPerStream.length);
            events = eventsLocal;
        }
        Object[] keys = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            keys[i] = evaluators[i].evaluate(events, context);
        }

        Set<EventBean> data = null;
        try {
            data = externalIndex.lookup(keys, eventsPerStream);
        } catch (RuntimeException ex) {
            log.warn("Exception encountered invoking virtual data window external index for window '" + namedWindowName + "': " + ex.getMessage(), ex);
        }
        return data;
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
        private final Class coercionType;

        private ExternalEvaluatorHashRelOp(ExprEvaluator hashKeysEval, Class coercionType) {
            this.hashKeysEval = hashKeysEval;
            this.coercionType = coercionType;
        }

        public Object evaluate(EventBean[] events, ExprEvaluatorContext context) {
            return EventBeanUtility.coerce(hashKeysEval.evaluate(events, true, context), coercionType);
        }
    }

    private static class ExternalEvaluatorBtreeRange implements ExternalEvaluator {

        private final ExprEvaluator startEval;
        private final ExprEvaluator endEval;
        private final Class coercionType;

        private ExternalEvaluatorBtreeRange(ExprEvaluator startEval, ExprEvaluator endEval, Class coercionType) {
            this.startEval = startEval;
            this.endEval = endEval;
            this.coercionType = coercionType;
        }

        public Object evaluate(EventBean[] events, ExprEvaluatorContext context) {
            Object start = EventBeanUtility.coerce(startEval.evaluate(events, true, context), coercionType);
            Object end = EventBeanUtility.coerce(endEval.evaluate(events, true, context), coercionType);
            return new VirtualDataWindowKeyRange(start, end);
        }
    }
}
