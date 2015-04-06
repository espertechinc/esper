/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */


package com.espertech.esper.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.IdentityHashMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;

public abstract class FilterParamIndexStringRangeBase extends FilterParamIndexLookupableBase
{
    protected final TreeMap<StringRange, EventEvaluator> ranges;
    private final IdentityHashMap<StringRange, EventEvaluator> rangesNullEndpoints;
    private final ReadWriteLock rangesRWLock;

    protected FilterParamIndexStringRangeBase(FilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(filterOperator, lookupable);

        ranges = new TreeMap<StringRange, EventEvaluator>(new StringRangeComparator());
        rangesNullEndpoints = new IdentityHashMap<StringRange, EventEvaluator>();
        rangesRWLock = readWriteLock;
    }

    public final EventEvaluator get(Object expressionValue)
    {
        if (!(expressionValue instanceof StringRange))
        {
            throw new IllegalArgumentException("Supplied expressionValue must be of type StringRange");
        }

        StringRange range = (StringRange) expressionValue;

        if ((range.getMax() == null) || (range.getMin() == null))
        {
            return rangesNullEndpoints.get(range);
        }

        return ranges.get(range);
    }

    public final void put(Object expressionValue, EventEvaluator matcher)
    {
        if (!(expressionValue instanceof StringRange))
        {
            throw new IllegalArgumentException("Supplied expressionValue must be of type DoubleRange");
        }

        StringRange range = (StringRange) expressionValue;
        if ((range.getMax() == null) || (range.getMin() == null))
        {
            rangesNullEndpoints.put(range, matcher);     // endpoints null - we don't enter
            return;
        }

        ranges.put(range, matcher);
    }

    public final boolean remove(Object filterConstant)
    {
        StringRange range = (StringRange) filterConstant;

        if ((range.getMax() == null) || (range.getMin() == null))
        {
            return rangesNullEndpoints.remove(range) != null;
        }

        return ranges.remove(range) != null;
    }

    public final int size()
    {
        return ranges.size();
    }

    public final ReadWriteLock getReadWriteLock()
    {
        return rangesRWLock;
    }

    private static final Log log = LogFactory.getLog(FilterParamIndexStringRangeBase.class);
}
