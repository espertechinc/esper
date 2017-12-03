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

import com.espertech.esper.filterspec.FilterOperator;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * Each implementation of this abstract class represents an index of filter parameter constants supplied in filter
 * parameters in filter specifications that feature the same event property and operator.
 * <p>
 * For example, a filter with a parameter of "count EQUALS 10" would be represented as index
 * for a property named "count" and for a filter operator typed "EQUALS". The index
 * would store a value of "10" in its internal structure.
 * <p>
 * Implementations make sure that the type of the Object constant in get and put calls matches the event property type.
 */
public abstract class FilterParamIndexBase implements EventEvaluator {
    private final FilterOperator filterOperator;

    /**
     * Constructor.
     *
     * @param filterOperator is the type of comparison performed.
     */
    public FilterParamIndexBase(FilterOperator filterOperator) {
        this.filterOperator = filterOperator;
    }

    /**
     * Get the event evaluation instance associated with the constant. Returns null if no entry found for the constant.
     * The calling class must make sure that access to the underlying resource is protected
     * for multi-threaded access, the getReadWriteLock() method must supply a lock for this purpose.
     *
     * @param filterConstant is the constant supplied in the event filter parameter
     * @return event evaluator stored for the filter constant, or null if not found
     */
    public abstract EventEvaluator get(Object filterConstant);

    /**
     * Store the event evaluation instance for the given constant. Can override an existing value
     * for the same constant.
     * The calling class must make sure that access to the underlying resource is protected
     * for multi-threaded access, the getReadWriteLock() method must supply a lock for this purpose.
     *
     * @param filterConstant is the constant supplied in the filter parameter
     * @param evaluator      to be stored for the constant
     */
    public abstract void put(Object filterConstant, EventEvaluator evaluator);

    /**
     * Remove the event evaluation instance for the given constant. Returns true if
     * the constant was found, or false if not.
     * The calling class must make sure that access to the underlying resource is protected
     * for multi-threaded writes, the getReadWriteLock() method must supply a lock for this purpose.
     *
     * @param filterConstant is the value supplied in the filter paremeter
     */
    public abstract void remove(Object filterConstant);

    /**
     * Return the number of distinct filter parameter constants stored, which can be an expensive call.
     * The calling class must make sure that access to the underlying resource is protected
     * for multi-threaded writes, the getReadWriteLock() method must supply a lock for this purpose.
     *
     * @return Number of entries in index
     */
    public abstract int sizeExpensive();

    /**
     * Return empty indicator.
     * The calling class must make sure that access to the underlying resource is protected
     * for multi-threaded writes, the getReadWriteLock() method must supply a lock for this purpose.
     *
     * @return empty flag
     */
    public abstract boolean isEmpty();

    /**
     * Supplies the lock for protected access.
     *
     * @return lock
     */
    public abstract ReadWriteLock getReadWriteLock();

    /**
     * Returns the filter operator that the index matches for.
     *
     * @return filter operator
     */
    public final FilterOperator getFilterOperator() {
        return filterOperator;
    }

    public String toString() {
        return "filterOperator=" + filterOperator;
    }
}