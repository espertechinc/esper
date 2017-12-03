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
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.*;
import com.espertech.esper.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper to compile (validate and optimize) filter expressions as used in pattern and filter-based streams.
 */
public final class FilterSpecCompilerConsolidateUtil {
    protected static void consolidate(FilterParamExprMap filterParamExprMap, String statementName) {
        // consolidate or place in a boolean expression (by removing filter spec param from the map)
        // any filter parameter that feature the same property name and filter operator,
        // i.e. we are looking for "a!=5 and a!=6"  to transform to "a not in (5,6)" which can match faster
        // considering that "a not in (5,6) and a not in (7,8)" is "a not in (5, 6, 7, 8)" therefore
        // we need to consolidate until there is no more work to do
        Map<Pair<ExprFilterSpecLookupable, FilterOperator>, List<FilterSpecParam>> mapOfParams = new HashMap<Pair<ExprFilterSpecLookupable, FilterOperator>, List<FilterSpecParam>>();

        boolean haveConsolidated;
        do {
            haveConsolidated = false;
            mapOfParams.clear();

            // sort into buckets of propertyName + filterOperator combination
            for (FilterSpecParam currentParam : filterParamExprMap.getFilterParams()) {
                ExprFilterSpecLookupable lookupable = currentParam.getLookupable();
                FilterOperator op = currentParam.getFilterOperator();
                Pair<ExprFilterSpecLookupable, FilterOperator> key = new Pair<ExprFilterSpecLookupable, FilterOperator>(lookupable, op);

                List<FilterSpecParam> existingParam = mapOfParams.get(key);
                if (existingParam == null) {
                    existingParam = new ArrayList<FilterSpecParam>();
                    mapOfParams.put(key, existingParam);
                }
                existingParam.add(currentParam);
            }

            for (List<FilterSpecParam> entry : mapOfParams.values()) {
                if (entry.size() > 1) {
                    haveConsolidated = true;
                    consolidate(entry, filterParamExprMap, statementName);
                }
            }
        }
        while (haveConsolidated);
    }

    // remove duplicate propertyName + filterOperator items making a judgement to optimize or simply remove the optimized form
    private static void consolidate(List<FilterSpecParam> items, FilterParamExprMap filterParamExprMap, String statementName) {
        FilterOperator op = items.get(0).getFilterOperator();
        if (op == FilterOperator.NOT_EQUAL) {
            handleConsolidateNotEqual(items, filterParamExprMap, statementName);
        } else {
            // for all others we simple remove the second optimized form (filter param with same prop name and filter op)
            // and thus the boolean expression that started this is included
            for (int i = 1; i < items.size(); i++) {
                filterParamExprMap.removeValue(items.get(i));
            }
        }
    }

    // consolidate "val != 3 and val != 4 and val != 5"
    // to "val not in (3, 4, 5)"
    private static void handleConsolidateNotEqual(List<FilterSpecParam> parameters, FilterParamExprMap filterParamExprMap, String statementName) {
        List<FilterSpecParamInValue> values = new ArrayList<FilterSpecParamInValue>();

        ExprNode lastNotEqualsExprNode = null;
        for (FilterSpecParam param : parameters) {
            if (param instanceof FilterSpecParamConstant) {
                FilterSpecParamConstant constantParam = (FilterSpecParamConstant) param;
                Object constant = constantParam.getFilterConstant();
                values.add(new FilterForEvalConstantAnyType(constant));
            } else if (param instanceof FilterSpecParamEventProp) {
                FilterSpecParamEventProp eventProp = (FilterSpecParamEventProp) param;
                values.add(new FilterForEvalEventPropMayCoerce(eventProp.getResultEventAsName(), eventProp.getResultEventProperty(),
                        eventProp.isMustCoerce(), JavaClassHelper.getBoxedType(eventProp.getCoercionType())));
            } else if (param instanceof FilterSpecParamEventPropIndexed) {
                FilterSpecParamEventPropIndexed eventProp = (FilterSpecParamEventPropIndexed) param;
                values.add(new FilterForEvalEventPropIndexedMayCoerce(eventProp.getResultEventAsName(), eventProp.getResultEventIndex(), eventProp.getResultEventProperty(),
                        eventProp.isMustCoerce(), JavaClassHelper.getBoxedType(eventProp.getCoercionType()), statementName));
            } else {
                throw new IllegalArgumentException("Unknown filter parameter:" + param.toString());
            }

            lastNotEqualsExprNode = filterParamExprMap.removeEntry(param);
        }

        FilterSpecParamIn param = new FilterSpecParamIn(parameters.get(0).getLookupable(), FilterOperator.NOT_IN_LIST_OF_VALUES, values);
        filterParamExprMap.put(lastNotEqualsExprNode, param);
    }

}
