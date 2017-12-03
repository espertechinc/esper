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
package com.espertech.esper.supportunit.filter;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.*;

import java.util.LinkedList;
import java.util.List;

public class SupportFilterSpecBuilder {
    public static FilterSpecCompiled build(EventType eventType, Object[] objects) {
        return new FilterSpecCompiled(eventType, "SomeAliasNameForType", new List[]{buildList(eventType, objects)}, null);
    }

    public static List<FilterSpecParam> buildList(EventType eventType, Object[] objects) {
        List<FilterSpecParam> filterParams = new LinkedList<FilterSpecParam>();

        int index = 0;
        while (objects.length > index) {
            String propertyName = (String) objects[index++];
            FilterOperator filterOperator = (FilterOperator) objects[index++];

            if (!(filterOperator.isRangeOperator())) {
                Object filterForConstant = objects[index++];
                filterParams.add(new FilterSpecParamConstant(makeLookupable(eventType, propertyName), filterOperator, filterForConstant));
            } else {
                double min = ((Number) objects[index++]).doubleValue();
                double max = ((Number) objects[index++]).doubleValue();
                filterParams.add(new FilterSpecParamRange(makeLookupable(eventType, propertyName), filterOperator,
                        new FilterForEvalConstantDouble(min),
                        new FilterForEvalConstantDouble(max)));
            }
        }

        return filterParams;
    }

    private static ExprFilterSpecLookupable makeLookupable(EventType eventType, String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName), false);
    }
}


