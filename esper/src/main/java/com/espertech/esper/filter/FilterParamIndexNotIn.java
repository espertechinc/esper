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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants to match using the 'not in' operator to match against a
 * all other values then the supplied set of values.
 */
public final class FilterParamIndexNotIn extends FilterParamIndexLookupableBase {
    private final Map<Object, Set<EventEvaluator>> constantsMap;
    private final Map<MultiKeyUntyped, EventEvaluator> filterValueEvaluators;
    private final Set<EventEvaluator> evaluatorsSet;
    private final ReadWriteLock constantsMapRWLock;

    public FilterParamIndexNotIn(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock) {
        super(FilterOperator.NOT_IN_LIST_OF_VALUES, lookupable);

        constantsMap = new HashMap<Object, Set<EventEvaluator>>();
        filterValueEvaluators = new HashMap<MultiKeyUntyped, EventEvaluator>();
        evaluatorsSet = new HashSet<EventEvaluator>();
        constantsMapRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object filterConstant) {
        MultiKeyUntyped keyValues = (MultiKeyUntyped) filterConstant;
        return filterValueEvaluators.get(keyValues);
    }

    public final void put(Object filterConstant, EventEvaluator evaluator) {
        // Store evaluator keyed to set of values
        MultiKeyUntyped keys = (MultiKeyUntyped) filterConstant;
        filterValueEvaluators.put(keys, evaluator);
        evaluatorsSet.add(evaluator);

        // Store each value to match against in Map with it's evaluator as a list
        Object[] keyValues = keys.getKeys();
        for (Object keyValue : keyValues) {
            Set<EventEvaluator> evaluators = constantsMap.get(keyValue);
            if (evaluators == null) {
                evaluators = new HashSet<EventEvaluator>();
                constantsMap.put(keyValue, evaluators);
            }
            evaluators.add(evaluator);
        }
    }

    public final void remove(Object filterConstant) {
        MultiKeyUntyped keys = (MultiKeyUntyped) filterConstant;

        // remove the mapping of value set to evaluator
        EventEvaluator eval = filterValueEvaluators.remove(keys);
        evaluatorsSet.remove(eval);

        Object[] keyValues = keys.getKeys();
        for (Object keyValue : keyValues) {
            Set<EventEvaluator> evaluators = constantsMap.get(keyValue);
            if (evaluators != null) {
                // could already be removed as constants may be the same
                evaluators.remove(eval);
                if (evaluators.isEmpty()) {
                    constantsMap.remove(keyValue);
                }
            }
        }
    }

    public final int sizeExpensive() {
        return constantsMap.size();
    }

    public boolean isEmpty() {
        return constantsMap.isEmpty();
    }

    public final ReadWriteLock getReadWriteLock() {
        return constantsMapRWLock;
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        Object attributeValue = lookupable.getGetter().get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, attributeValue);
        }

        if (attributeValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        // Look up in hashtable the set of not-in evaluators
        constantsMapRWLock.readLock().lock();
        Set<EventEvaluator> evalNotMatching = constantsMap.get(attributeValue);

        // if all known evaluators are matching, invoke all
        if (evalNotMatching == null) {
            try {
                for (EventEvaluator eval : evaluatorsSet) {
                    eval.matchEvent(theEvent, matches);
                }
            } finally {
                constantsMapRWLock.readLock().unlock();
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(true);
            }
            return;
        }

        // if none are matching, we are done
        if (evalNotMatching.size() == evaluatorsSet.size()) {
            constantsMapRWLock.readLock().unlock();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        // handle partial matches: loop through all evaluators and see which one should not be matching, match all else
        try {
            for (EventEvaluator eval : evaluatorsSet) {
                if (!(evalNotMatching.contains(eval))) {
                    eval.matchEvent(theEvent, matches);
                }
            }
        } finally {
            constantsMapRWLock.readLock().unlock();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(FilterParamIndexNotIn.class);
}
