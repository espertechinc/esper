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

import com.espertech.esper.epl.index.quadtree.EngineImportApplicationDotMethodPointInsideRectange;
import com.espertech.esper.epl.index.quadtree.EngineImportApplicationDotMethodRectangeIntersectsRectangle;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

/**
 * Factory for {@link FilterParamIndexBase} instances based on event property name and filter operator type.
 */
public class IndexFactory {
    /**
     * Factory for indexes that store filter parameter constants for a given event property and filter
     * operator.
     * <p>Does not perform any check of validity of property name.
     *
     * @param filterOperator is the type of index to use
     * @param lockFactory    lock factory
     * @param lookupable     the lookup item
     * @return the proper index based on the filter operator type
     */
    public static FilterParamIndexBase createIndex(ExprFilterSpecLookupable lookupable, FilterServiceGranularLockFactory lockFactory, FilterOperator filterOperator) {
        FilterParamIndexBase index;
        Class returnValueType = lookupable.getReturnType();

        // Handle all EQUAL comparisons
        if (filterOperator == FilterOperator.EQUAL) {
            index = new FilterParamIndexEquals(lookupable, lockFactory.obtainNew());
            return index;
        }

        // Handle all NOT-EQUAL comparisons
        if (filterOperator == FilterOperator.NOT_EQUAL) {
            index = new FilterParamIndexNotEquals(lookupable, lockFactory.obtainNew());
            return index;
        }

        if (filterOperator == FilterOperator.IS) {
            index = new FilterParamIndexEqualsIs(lookupable, lockFactory.obtainNew());
            return index;
        }

        if (filterOperator == FilterOperator.IS_NOT) {
            index = new FilterParamIndexNotEqualsIs(lookupable, lockFactory.obtainNew());
            return index;
        }

        // Handle all GREATER, LESS etc. comparisons
        if ((filterOperator == FilterOperator.GREATER) ||
                (filterOperator == FilterOperator.GREATER_OR_EQUAL) ||
                (filterOperator == FilterOperator.LESS) ||
                (filterOperator == FilterOperator.LESS_OR_EQUAL)) {
            if (returnValueType != String.class) {
                index = new FilterParamIndexCompare(lookupable, lockFactory.obtainNew(), filterOperator);
            } else {
                index = new FilterParamIndexCompareString(lookupable, lockFactory.obtainNew(), filterOperator);
            }
            return index;
        }

        // Handle all normal and inverted RANGE comparisons
        if (filterOperator.isRangeOperator()) {
            if (returnValueType != String.class) {
                index = new FilterParamIndexDoubleRange(lookupable, lockFactory.obtainNew(), filterOperator);
            } else {
                index = new FilterParamIndexStringRange(lookupable, lockFactory.obtainNew(), filterOperator);
            }
            return index;
        }
        if (filterOperator.isInvertedRangeOperator()) {
            if (returnValueType != String.class) {
                return new FilterParamIndexDoubleRangeInverted(lookupable, lockFactory.obtainNew(), filterOperator);
            } else {
                return new FilterParamIndexStringRangeInverted(lookupable, lockFactory.obtainNew(), filterOperator);
            }
        }

        // Handle all IN and NOT IN comparisons
        if (filterOperator == FilterOperator.IN_LIST_OF_VALUES) {
            return new FilterParamIndexIn(lookupable, lockFactory.obtainNew());
        }
        if (filterOperator == FilterOperator.NOT_IN_LIST_OF_VALUES) {
            return new FilterParamIndexNotIn(lookupable, lockFactory.obtainNew());
        }

        // Handle all boolean expression
        if (filterOperator == FilterOperator.BOOLEAN_EXPRESSION) {
            return new FilterParamIndexBooleanExpr(lockFactory.obtainNew());
        }

        // Handle advanced-index
        if (filterOperator == FilterOperator.ADVANCED_INDEX) {
            FilterSpecLookupableAdvancedIndex advLookable = (FilterSpecLookupableAdvancedIndex) lookupable;
            if (advLookable.getIndexType().equals(EngineImportApplicationDotMethodPointInsideRectange.INDEXTYPE_NAME)) {
                return new FilterParamIndexQuadTreePointRegion(lockFactory.obtainNew(), lookupable);
            } else if (advLookable.getIndexType().equals(EngineImportApplicationDotMethodRectangeIntersectsRectangle.INDEXTYPE_NAME)) {
                return new FilterParamIndexQuadTreeMXCIF(lockFactory.obtainNew(), lookupable);
            } else {
                throw new IllegalStateException("Unrecognized index type " + advLookable.getIndexType());
            }

        }
        throw new IllegalArgumentException("Cannot create filter index instance for filter operator " + filterOperator);
    }
}

