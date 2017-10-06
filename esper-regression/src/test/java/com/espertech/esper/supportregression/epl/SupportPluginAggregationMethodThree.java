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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.hook.AggregationFunctionFactoryCodegenRowApplyContextManaged;
import com.espertech.esper.client.hook.AggregationFunctionFactoryCodegenRowClearContext;
import com.espertech.esper.client.hook.AggregationFunctionFactoryCodegenRowGetValueContext;
import com.espertech.esper.client.hook.AggregationFunctionFactoryCodegenRowMemberContext;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

import java.io.Serializable;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GE;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class SupportPluginAggregationMethodThree implements Serializable, AggregationMethod {
    private static Object[] lastEnterParameters;
    private int count;

    public static void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), int.class, "count");
    }

    public static Object[] getLastEnterParameters() {
        return lastEnterParameters;
    }

    public static void setLastEnterParameters(Object[] lastEnterParameters) {
        SupportPluginAggregationMethodThree.lastEnterParameters = lastEnterParameters;
    }

    public void enter(Object value) {
        Object[] parameters = (Object[]) value;
        lastEnterParameters = parameters;
        int lower = (Integer) parameters[0];
        int upper = (Integer) parameters[1];
        int val = (Integer) parameters[2];
        if ((val >= lower) && (val <= upper)) {
            count++;
        }
    }

    public static void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        applyCodegen(true, context);
    }

    public void leave(Object value) {
        Object[] parameters = (Object[]) value;
        int lower = (Integer) parameters[0];
        int upper = (Integer) parameters[1];
        int val = (Integer) parameters[2];
        if ((val >= lower) && (val <= upper)) {
            count--;
        }
    }

    public static void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        applyCodegen(false, context);
    }

    public void clear() {
    count = 0;
}

    public static void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
        context.getMethod().getBlock().assignRef(refCol("count", context.getColumn()), constant(0));
    }

    public Object getValue() {
        return count;
    }

    public static void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
        context.getMethod().getBlock().methodReturn(refCol("count", context.getColumn()));
    }

    private static void applyCodegen(boolean enter, AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        context.getMethod().getBlock().declareVar(Object[].class, "parameters", cast(Object[].class, ref("value")))
                .staticMethod(SupportPluginAggregationMethodThree.class, "setLastEnterParameters", ref("parameters"))
                .declareVar(int.class, "lower", cast(Integer.class, arrayAtIndex(ref("parameters"), constant(0))))
                .declareVar(int.class, "upper", cast(Integer.class, arrayAtIndex(ref("parameters"), constant(1))))
                .declareVar(int.class, "val", cast(Integer.class, arrayAtIndex(ref("parameters"), constant(2))))
                .ifCondition(and(relational(ref("val"), GE, ref("lower")), relational(ref("val"), LE, ref("upper"))))
                .assignCompound(refCol("count", context.getColumn()), enter ? "+" : "-", constant(1));
    }
}
