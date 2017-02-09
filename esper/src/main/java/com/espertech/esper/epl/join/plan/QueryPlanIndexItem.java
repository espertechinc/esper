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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.util.CollectionUtil;

import java.util.Arrays;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexItem {
    private final String[] indexProps;
    private Class[] optIndexCoercionTypes;
    private final String[] rangeProps;
    private final Class[] optRangeCoercionTypes;
    private final boolean unique;

    /**
     * Ctor.
     *
     * @param indexProps            - array of property names with the first dimension suplying the number of
     *                              distinct indexes. The second dimension can be empty and indicates a full table scan.
     * @param optIndexCoercionTypes - array of coercion types for each index, or null entry for no coercion required
     * @param unique                whether index is unique on index props (not applicable to range-only)
     * @param optRangeCoercionTypes coercion for ranges
     * @param rangeProps            range props
     */
    public QueryPlanIndexItem(String[] indexProps, Class[] optIndexCoercionTypes, String[] rangeProps, Class[] optRangeCoercionTypes, boolean unique) {
        this.indexProps = indexProps;
        this.optIndexCoercionTypes = optIndexCoercionTypes;
        this.rangeProps = (rangeProps == null || rangeProps.length == 0) ? null : rangeProps;
        this.optRangeCoercionTypes = optRangeCoercionTypes;
        this.unique = unique;
        if (unique && indexProps.length == 0) {
            throw new IllegalStateException("Invalid unique index planned without hash index props");
        }
        if (unique && rangeProps.length > 0) {
            throw new IllegalStateException("Invalid unique index planned that includes range props");
        }
    }

    public String[] getIndexProps() {
        return indexProps;
    }

    public Class[] getOptIndexCoercionTypes() {
        return optIndexCoercionTypes;
    }

    public String[] getRangeProps() {
        return rangeProps;
    }

    public Class[] getOptRangeCoercionTypes() {
        return optRangeCoercionTypes;
    }

    public void setOptIndexCoercionTypes(Class[] optIndexCoercionTypes) {
        this.optIndexCoercionTypes = optIndexCoercionTypes;
    }

    public boolean isUnique() {
        return unique;
    }

    @Override
    public String toString() {
        return "QueryPlanIndexItem{" +
                "unique=" + unique +
                ", indexProps=" + (indexProps == null ? null : Arrays.asList(indexProps)) +
                ", rangeProps=" + (rangeProps == null ? null : Arrays.asList(rangeProps)) +
                ", optIndexCoercionTypes=" + (optIndexCoercionTypes == null ? null : Arrays.asList(optIndexCoercionTypes)) +
                ", optRangeCoercionTypes=" + (optRangeCoercionTypes == null ? null : Arrays.asList(optRangeCoercionTypes)) +
                '}';
    }

    public boolean equalsCompareSortedProps(QueryPlanIndexItem other) {
        if (unique != other.unique) {
            return false;
        }
        String[] otherIndexProps = CollectionUtil.copySortArray(other.getIndexProps());
        String[] thisIndexProps = CollectionUtil.copySortArray(this.getIndexProps());
        String[] otherRangeProps = CollectionUtil.copySortArray(other.getRangeProps());
        String[] thisRangeProps = CollectionUtil.copySortArray(this.getRangeProps());
        return CollectionUtil.compare(otherIndexProps, thisIndexProps) && CollectionUtil.compare(otherRangeProps, thisRangeProps);
    }
}
