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

import com.espertech.esper.collection.Pair;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterSpecParam;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Utility class for matching filter parameters to indizes. Matches are indicated by the index {@link FilterParamIndexBase}
 * and the filter parameter {@link FilterSpecParam} featuring the same event property name and filter operator.
 */
public class IndexHelper {
    /**
     * Find an index that matches one of the filter parameters passed.
     * The parameter type and index type match up if the property name and
     * filter operator are the same for the index and the filter parameter.
     * For instance, for a filter parameter of "count EQUALS 10", the index against property "count" with
     * operator type EQUALS will be returned, if present.
     * NOTE: The caller is expected to obtain locks, if necessary, on the collections passed in.
     * NOTE: Doesn't match non-property based index - thus boolean expressions don't get found and are always entered as a new index
     *
     * @param parameters is the list of sorted filter parameters
     * @param indizes    is the collection of indexes
     * @return A matching pair of filter parameter and index, if any matches were found. Null if no matches were found.
     */
    public static Pair<FilterValueSetParam, FilterParamIndexBase> findIndex(ArrayDeque<FilterValueSetParam> parameters, List<FilterParamIndexBase> indizes) {
        for (FilterValueSetParam parameter : parameters) {
            ExprFilterSpecLookupable lookupable = parameter.getLookupable();
            FilterOperator operator = parameter.getFilterOperator();

            for (FilterParamIndexBase index : indizes) {
                // if property-based index, we prefer this in matching
                if (index instanceof FilterParamIndexLookupableBase) {
                    FilterParamIndexLookupableBase propBasedIndex = (FilterParamIndexLookupableBase) index;
                    if ((lookupable.equals(propBasedIndex.getLookupable())) &&
                            (operator.equals(propBasedIndex.getFilterOperator()))) {
                        return new Pair<FilterValueSetParam, FilterParamIndexBase>(parameter, index);
                    }
                } else if (index instanceof FilterParamIndexBooleanExpr && parameters.size() == 1) {
                    // if boolean-expression then match only if this is the last parameter,
                    // all others considered are higher order and sort ahead
                    if (operator.equals(FilterOperator.BOOLEAN_EXPRESSION)) {
                        return new Pair<FilterValueSetParam, FilterParamIndexBase>(parameter, index);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Determine among the passed in filter parameters any parameter that matches the given index on property name and
     * filter operator type. Returns null if none of the parameters matches the index.
     *
     * @param parameters is the filter parameter list
     * @param index      is a filter parameter constant value index
     * @return filter parameter, or null if no matching parameter found.
     */
    public static FilterValueSetParam findParameter(ArrayDeque<FilterValueSetParam> parameters,
                                                    FilterParamIndexBase index) {
        if (index instanceof FilterParamIndexLookupableBase) {
            FilterParamIndexLookupableBase propBasedIndex = (FilterParamIndexLookupableBase) index;
            ExprFilterSpecLookupable indexLookupable = propBasedIndex.getLookupable();
            FilterOperator indexOperator = propBasedIndex.getFilterOperator();

            for (FilterValueSetParam parameter : parameters) {
                ExprFilterSpecLookupable lookupable = parameter.getLookupable();
                FilterOperator paramOperator = parameter.getFilterOperator();

                if ((lookupable.equals(indexLookupable)) &&
                        (paramOperator.equals(indexOperator))) {
                    return parameter;
                }
            }
        } else {
            for (FilterValueSetParam parameter : parameters) {
                FilterOperator paramOperator = parameter.getFilterOperator();

                if (paramOperator.equals(index.getFilterOperator())) {
                    return parameter;
                }
            }
        }

        return null;
    }
}
