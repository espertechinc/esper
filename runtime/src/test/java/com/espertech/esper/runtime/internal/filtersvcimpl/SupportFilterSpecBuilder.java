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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;

import java.util.LinkedList;
import java.util.List;

public class SupportFilterSpecBuilder {
    public static FilterSpecActivatable build(EventType eventType, Object[] objects) {
        FilterSpecParam[][] params = new FilterSpecParam[][]{buildList(eventType, objects)};
        return new FilterSpecActivatable(eventType, "SomeAliasNameForType", params, null, 1);
    }

    public static FilterSpecParam[] buildList(EventType eventType, Object[] objects) {
        List<FilterSpecParam> filterParams = new LinkedList<FilterSpecParam>();

        int index = 0;
        while (objects.length > index) {
            String propertyName = (String) objects[index++];
            FilterOperator filterOperator = (FilterOperator) objects[index++];

            if (!(filterOperator.isRangeOperator())) {
                Object filterForConstant = objects[index++];
                filterParams.add(new SupportFilterSpecParamConstant(makeLookupable(eventType, propertyName), filterOperator, filterForConstant));
            } else {
                double min = ((Number) objects[index++]).doubleValue();
                double max = ((Number) objects[index++]).doubleValue();
                filterParams.add(new SupportFilterSpecParamRange(makeLookupable(eventType, propertyName), filterOperator,
                        new SupportFilterForEvalConstantDouble(min),
                        new SupportFilterForEvalConstantDouble(max)));
            }
        }

        return filterParams.toArray(new FilterSpecParam[0]);
    }

    private static ExprFilterSpecLookupable makeLookupable(EventType eventType, String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName), false, null);
    }
}


