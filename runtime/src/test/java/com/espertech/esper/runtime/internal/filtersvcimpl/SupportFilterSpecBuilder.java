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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlan;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPath;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPathTriplet;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;
import com.espertech.esper.runtime.internal.support.SupportExprEventEvaluator;

import java.util.LinkedList;
import java.util.List;

public class SupportFilterSpecBuilder {
    public static FilterSpecActivatable build(EventType eventType, Object[] objects) {
        FilterSpecPlanPathTriplet[] triplets = buildTriplets(eventType, objects);
        FilterSpecPlanPath[] paths = new FilterSpecPlanPath[] {new FilterSpecPlanPath(triplets)};
        FilterSpecPlan plan = new FilterSpecPlan(paths, null, null);
        plan.initialize();
        return new FilterSpecActivatable(eventType, "SomeAliasNameForType", plan, null, 1);
    }

    public static FilterSpecPlanPathTriplet[] buildTriplets(EventType eventType, Object[] objects) {
        List<FilterSpecPlanPathTriplet> filterParams = new LinkedList<>();

        int index = 0;
        while (objects.length > index) {
            String propertyName = (String) objects[index++];
            FilterOperator filterOperator = (FilterOperator) objects[index++];

            if (!(filterOperator.isRangeOperator())) {
                Object filterForConstant = objects[index++];
                FilterSpecParam param = new SupportFilterSpecParamConstant(makeLookupable(eventType, propertyName), filterOperator, filterForConstant);
                filterParams.add(new FilterSpecPlanPathTriplet(param));
            } else {
                double min = ((Number) objects[index++]).doubleValue();
                double max = ((Number) objects[index++]).doubleValue();
                FilterSpecParam param = new SupportFilterSpecParamRange(makeLookupable(eventType, propertyName), filterOperator,
                    new SupportFilterForEvalConstantDouble(min),
                    new SupportFilterForEvalConstantDouble(max));
                filterParams.add(new FilterSpecPlanPathTriplet(param));
            }
        }

        return filterParams.toArray(new FilterSpecPlanPathTriplet[0]);
    }

    private static ExprFilterSpecLookupable makeLookupable(EventType eventType, String fieldName) {
        SupportExprEventEvaluator eval = new SupportExprEventEvaluator(eventType.getGetter(fieldName));
        return new ExprFilterSpecLookupable(fieldName, eval, null, (EPTypeClass) eventType.getPropertyEPType(fieldName), false, null);
    }
}


