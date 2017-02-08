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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionValidationContext;

public enum SupportAggMFFunc {
    SINGLE_EVENT_1("se1", null),
    SINGLE_EVENT_2("se2", null),
    ENUM_EVENT("ee", getAccessor(SupportAggMFAccessorEnumerableEvents.class)),
    COLL_SCALAR("sc", getAccessor(SupportAggMFAccessorCollScalar.class)),
    ARR_SCALAR("sa", getAccessor(SupportAggMFAccessorArrScalar.class)),
    SCALAR("ss", getAccessor(SupportAggMFAccessorPlainScalar.class));

    private final String name;
    private final AggregationAccessor accessor;

    private SupportAggMFFunc(String name, AggregationAccessor accessor) {
        this.name = name;
        this.accessor = accessor;
    }

    public AggregationAccessor getAccessor() {
        return accessor;
    }

    public String getName() {
        return name;
    }

    public static boolean isSingleEvent(String functionName) {
        return SINGLE_EVENT_1.getName().equals(functionName) ||
               SINGLE_EVENT_2.getName().equals(functionName);
    }

    public static String[] getFunctionNames() {
        String[] names = new String[SupportAggMFFunc.values().length];
        for (int i = 0; i < names.length; i++) {
            names[i] = SupportAggMFFunc.values()[i].getName();
        }
        return names;
    }

    public static SupportAggMFFunc fromFunctionName(String functionName) {
        for (SupportAggMFFunc func : SupportAggMFFunc.values()) {
            if (func.getName().equals(functionName)) {
                return func;
            }
        }
        throw new RuntimeException("Unrecognized function name '" + functionName + "'");
    }

    private static AggregationAccessor getAccessor(final Class clazz) {
        try {
            return (AggregationAccessor) clazz.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + e.getMessage(), e);
        }
    }

    public EPType getReturnType(EventType eventType, ExprNode[] parameters) {
        if (this == SCALAR) {
            return EPTypeHelper.singleValue(parameters[0].getExprEvaluator().getType());
        }
        if (this == ENUM_EVENT) {
            return EPTypeHelper.collectionOfEvents(eventType);
        }
        if (this == COLL_SCALAR) {
            return EPTypeHelper.collectionOfSingleValue(parameters[0].getExprEvaluator().getType());
        }
        if (this == ARR_SCALAR) {
            return EPTypeHelper.array(parameters[0].getExprEvaluator().getType());
        }
        if (this == SINGLE_EVENT_1 || this == SINGLE_EVENT_2) {
            return EPTypeHelper.singleEvent(eventType);
        }
        throw new RuntimeException("Return type not supported for " + this);
    }

    public PlugInAggregationMultiFunctionStateFactory getStateFactory(PlugInAggregationMultiFunctionValidationContext validationContext) {
        if (this == SCALAR) {
            if (validationContext.getParameterExpressions().length != 1) {
                throw new IllegalArgumentException("Function '" + validationContext.getFunctionName() + "' requires 1 parameter");
            }
            ExprEvaluator evaluator = validationContext.getParameterExpressions()[0].getExprEvaluator();
            return new SupportAggMFStatePlainScalarFactory(evaluator);
        }
        if (this == ARR_SCALAR || this == COLL_SCALAR) {
            if (validationContext.getParameterExpressions().length != 1) {
                throw new IllegalArgumentException("Function '" + validationContext.getFunctionName() + "' requires 1 parameter");
            }
            ExprEvaluator evaluator = validationContext.getParameterExpressions()[0].getExprEvaluator();
            return new SupportAggMFStateArrayCollScalarFactory(evaluator);
        }
        if (this == ENUM_EVENT) {
            return new PlugInAggregationMultiFunctionStateFactory() {
                public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
                    return new SupportAggMFStateEnumerableEvents();
                }
            };
        }
        throw new RuntimeException("Return type not supported for " + this);
    }
}
