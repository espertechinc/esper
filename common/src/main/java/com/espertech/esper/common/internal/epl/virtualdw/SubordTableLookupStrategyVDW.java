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
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeIn;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeRelOp;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class SubordTableLookupStrategyVDW implements SubordTableLookupStrategy {

    private static final Logger log = LoggerFactory.getLogger(SubordTableLookupStrategyVDW.class);

    private final VirtualDWViewFactory factory;
    private final VirtualDataWindowLookup externalIndex;
    private final ExternalEvaluator[] evaluators;
    private final boolean nwOnTrigger;
    private final EventBean[] eventsLocal;

    public SubordTableLookupStrategyVDW(VirtualDWViewFactory factory, SubordTableLookupStrategyFactoryVDW subordTableFactory, VirtualDataWindowLookup externalIndex) {
        this.factory = factory;
        this.externalIndex = externalIndex;
        this.nwOnTrigger = subordTableFactory.isNwOnTrigger();

        ExprEvaluator[] hashKeys = subordTableFactory.getHashEvals();
        Class[] hashCoercionTypes = subordTableFactory.getHashCoercionTypes();
        QueryGraphValueEntryRange[] rangeKeys = subordTableFactory.getRangeEvals();
        Class[] rangeCoercionTypes = subordTableFactory.getRangeCoercionTypes();

        this.evaluators = new ExternalEvaluator[hashKeys.length + rangeKeys.length];
        this.eventsLocal = new EventBean[subordTableFactory.getNumOuterStreams() + 1];

        int count = 0;
        for (ExprEvaluator hashKey : hashKeys) {
            evaluators[count] = new ExternalEvaluatorHashRelOp(hashKeys[count], hashCoercionTypes[count]);
            count++;
        }
        for (int i = 0; i < rangeKeys.length; i++) {
            QueryGraphValueEntryRange rangeKey = rangeKeys[i];
            if (rangeKey.getType().isRange()) {
                QueryGraphValueEntryRangeIn range = (QueryGraphValueEntryRangeIn) rangeKey;
                ExprEvaluator evaluatorStart = range.getExprStart();
                ExprEvaluator evaluatorEnd = range.getExprEnd();
                evaluators[count] = new ExternalEvaluatorBtreeRange(evaluatorStart, evaluatorEnd, rangeCoercionTypes[i]);
            } else {
                QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKey;
                ExprEvaluator evaluator = relOp.getExpression();
                evaluators[count] = new ExternalEvaluatorHashRelOp(evaluator, rangeCoercionTypes[i]);
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
            log.warn("Exception encountered invoking virtual data window external index for window '" + factory.getNamedWindowName() + "': " + ex.getMessage(), ex);
        }
        return data;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " external index " + externalIndex;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.VDW);
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
