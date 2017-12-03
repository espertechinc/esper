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
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants for the comparison operators (less, greater, etc).
 * The implementation is based on the SortedMap implementation of TreeMap.
 * The index only accepts numeric constants. It keeps a lower and upper bounds of all constants in the index
 * for fast range checking, since the assumption is that frequently values fall within a range.
 */
public final class FilterParamIndexCompare extends FilterParamIndexLookupableBase {
    private final TreeMap<Object, EventEvaluator> constantsMap;
    private final ReadWriteLock constantsMapRWLock;

    private Double lowerBounds;
    private Double upperBounds;

    public FilterParamIndexCompare(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(filterOperator, lookupable);

        constantsMap = new TreeMap<Object, EventEvaluator>();
        constantsMapRWLock = readWriteLock;

        if ((filterOperator != FilterOperator.GREATER) &&
                (filterOperator != FilterOperator.GREATER_OR_EQUAL) &&
                (filterOperator != FilterOperator.LESS) &&
                (filterOperator != FilterOperator.LESS_OR_EQUAL)) {
            throw new IllegalArgumentException("Invalid filter operator for index of " + filterOperator);
        }
    }

    public final EventEvaluator get(Object filterConstant) {
        return constantsMap.get(filterConstant);
    }

    public final void put(Object filterConstant, EventEvaluator matcher) {
        constantsMap.put(filterConstant, matcher);

        // Update bounds
        Double constant = ((Number) filterConstant).doubleValue();
        if ((lowerBounds == null) || (constant < lowerBounds)) {
            lowerBounds = constant;
        }
        if ((upperBounds == null) || (constant > upperBounds)) {
            upperBounds = constant;
        }
    }

    public final void remove(Object filterConstant) {
        if (constantsMap.remove(filterConstant) == null) {
            return;
        }
        updateBounds();
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
        Object propertyValue = lookupable.getGetter().get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, propertyValue);
        }

        if (propertyValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        // A undefine lower bound indicates an empty index
        if (lowerBounds == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        FilterOperator filterOperator = this.getFilterOperator();
        Double propertyValueDouble = ((Number) propertyValue).doubleValue();

        // Based on current lower and upper bounds check if the property value falls outside - shortcut submap generation
        if ((filterOperator == FilterOperator.GREATER) && (propertyValueDouble <= lowerBounds)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        } else if ((filterOperator == FilterOperator.GREATER_OR_EQUAL) && (propertyValueDouble < lowerBounds)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        } else if ((filterOperator == FilterOperator.LESS) && (propertyValueDouble >= upperBounds)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        } else if ((filterOperator == FilterOperator.LESS_OR_EQUAL) && (propertyValueDouble > upperBounds)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        // Look up in table
        constantsMapRWLock.readLock().lock();
        try {

            // Get the head or tail end of the map depending on comparison type
            Map<Object, EventEvaluator> subMap;

            if ((filterOperator == FilterOperator.GREATER) ||
                    (filterOperator == FilterOperator.GREATER_OR_EQUAL)) {
                // At the head of the map are those with a lower numeric constants
                subMap = constantsMap.headMap(propertyValue);
            } else {
                subMap = constantsMap.tailMap(propertyValue);
            }

            // All entries in the subMap are elgibile, with an exception
            EventEvaluator exactEquals = null;
            if (filterOperator == FilterOperator.LESS) {
                exactEquals = constantsMap.get(propertyValue);
            }

            for (EventEvaluator matcher : subMap.values()) {
                // For the LESS comparison type we ignore the exactly equal case
                // The subMap is sorted ascending, thus the exactly equals case is the first
                if (exactEquals != null) {
                    exactEquals = null;
                    continue;
                }

                matcher.matchEvent(theEvent, matches);
            }

            if (filterOperator == FilterOperator.GREATER_OR_EQUAL) {
                EventEvaluator matcher = constantsMap.get(propertyValue);
                if (matcher != null) {
                    matcher.matchEvent(theEvent, matches);
                }
            }
        } finally {
            constantsMapRWLock.readLock().unlock();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }

    private void updateBounds() {
        if (constantsMap.isEmpty()) {
            lowerBounds = null;
            upperBounds = null;
            return;
        }
        lowerBounds = ((Number) constantsMap.firstKey()).doubleValue();
        upperBounds = ((Number) constantsMap.lastKey()).doubleValue();
    }

    private static final Logger log = LoggerFactory.getLogger(FilterParamIndexCompare.class);
}
