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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.*;

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + e.getMessage(), e);
        }
    }

    public EPType getReturnType(EventType eventType, ExprNode[] parameters) {
        if (this == SCALAR) {
            return EPTypeHelper.singleValue(parameters[0].getForge().getEvaluationType());
        }
        if (this == ENUM_EVENT) {
            return EPTypeHelper.collectionOfEvents(eventType);
        }
        if (this == COLL_SCALAR) {
            return EPTypeHelper.collectionOfSingleValue(parameters[0].getForge().getEvaluationType());
        }
        if (this == ARR_SCALAR) {
            return EPTypeHelper.array(parameters[0].getForge().getEvaluationType());
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
            ExprEvaluator evaluator = validationContext.getParameterExpressions()[0].getForge().getExprEvaluator();
            return new SupportAggMFStatePlainScalarFactory(evaluator);
        }
        if (this == ARR_SCALAR || this == COLL_SCALAR) {
            if (validationContext.getParameterExpressions().length != 1) {
                throw new IllegalArgumentException("Function '" + validationContext.getFunctionName() + "' requires 1 parameter");
            }
            ExprForge forge = validationContext.getParameterExpressions()[0].getForge();
            return new SupportAggMFStateArrayCollScalarFactory(forge);
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

    public void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        switch(this) {
            case SCALAR: SupportAggMFStatePlainScalarFactory.rowMemberCodegen(context); return;
            case ARR_SCALAR:
            case COLL_SCALAR: SupportAggMFStateArrayCollScalarFactory.rowMemberCodegen(context); return;
            case ENUM_EVENT: SupportAggMFStateEnumerableEvents.rowMemberCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void applyEnterCodegen(PlugInAggregationMultiFunctionValidationContext validationContext, PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        switch(this) {
            case SCALAR: {
                ExprForge forge = validationContext.getParameterExpressions()[0].getForge();
                SupportAggMFStatePlainScalarFactory.applyEnterCodegen(forge, context);
                return;
            }
            case ARR_SCALAR:
            case COLL_SCALAR: {
                ExprForge forge = validationContext.getParameterExpressions()[0].getForge();
                SupportAggMFStateArrayCollScalarFactory.applyEnterCodegen(forge, context);
                return;
            }
            case ENUM_EVENT: SupportAggMFStateEnumerableEvents.applyEnterCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        switch(this) {
            case SCALAR: SupportAggMFStatePlainScalarFactory.applyLeaveCodegen(context); return;
            case ARR_SCALAR:
            case COLL_SCALAR: SupportAggMFStateArrayCollScalarFactory.applyLeaveCodegen(context); return;
            case ENUM_EVENT: SupportAggMFStateEnumerableEvents.applyLeaveCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        switch(this) {
            case SCALAR: SupportAggMFStatePlainScalarFactory.clearCodegen(context); return;
            case ARR_SCALAR:
            case COLL_SCALAR: SupportAggMFStateArrayCollScalarFactory.clearCodegen(context); return;
            case ENUM_EVENT: SupportAggMFStateEnumerableEvents.clearCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        switch(this) {
            case SCALAR: SupportAggMFAccessorPlainScalar.getValueCodegen(context); return;
            case ARR_SCALAR: SupportAggMFAccessorArrScalar.getValueCodegen(context); return;
            case COLL_SCALAR: SupportAggMFAccessorCollScalar.getValueCodegen(context); return;
            case ENUM_EVENT: SupportAggMFAccessorEnumerableEvents.getValueCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        switch(this) {
            case SCALAR: SupportAggMFAccessorPlainScalar.getEnumerableEventsCodegen(context); return;
            case ARR_SCALAR: SupportAggMFAccessorArrScalar.getEnumerableEventsCodegen(context); return;
            case COLL_SCALAR: SupportAggMFAccessorCollScalar.getEnumerableEventsCodegen(context); return;
            case ENUM_EVENT: SupportAggMFAccessorEnumerableEvents.getEnumerableEventsCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        switch(this) {
            case SCALAR: SupportAggMFAccessorPlainScalar.getEnumerableEventCodegen(context); return;
            case ARR_SCALAR: SupportAggMFAccessorArrScalar.getEnumerableEventCodegen(context); return;
            case COLL_SCALAR: SupportAggMFAccessorCollScalar.getEnumerableEventCodegen(context); return;
            case ENUM_EVENT: SupportAggMFAccessorEnumerableEvents.getEnumerableEventCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        switch(this) {
            case SCALAR: SupportAggMFAccessorPlainScalar.getEnumerableScalarCodegen(context); return;
            case ARR_SCALAR: SupportAggMFAccessorArrScalar.getEnumerableScalarCodegen(context); return;
            case COLL_SCALAR: SupportAggMFAccessorCollScalar.getEnumerableScalarCodegen(context); return;
            case ENUM_EVENT: SupportAggMFAccessorEnumerableEvents.getEnumerableScalarCodegen(context); return;
        }
        throw new RuntimeException("Not supported for " + this);
    }
}
