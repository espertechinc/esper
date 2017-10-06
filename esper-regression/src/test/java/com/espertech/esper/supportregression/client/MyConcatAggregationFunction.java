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

import com.espertech.esper.client.hook.*;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.JavaClassHelper;

import java.io.Serializable;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class MyConcatAggregationFunction implements AggregationMethod, Serializable {
    private final static char DELIMITER = ' ';
    private StringBuilder builder;
    private String delimiter;

    public MyConcatAggregationFunction() {
        super();
        builder = new StringBuilder();
        delimiter = "";
    }

    public static void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), StringBuilder.class, "builder")
                .addMember(context.getColumn(), String.class, "delimiter");
        context.getCtor().getBlock().assignRef(refCol("builder", context.getColumn()), newInstance(StringBuilder.class))
                .assignRef(refCol("delimiter", context.getColumn()), constant(""));
    }

    public void enter(Object value) {
        if (value != null) {
            builder.append(delimiter);
            builder.append(value.toString());
            delimiter = String.valueOf(DELIMITER);
        }
    }

    public static void applyEnterCodegen(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        CodegenExpressionRef builder = refCol("builder", context.getColumn());
        CodegenExpressionRef delimiter = refCol("delimiter", context.getColumn());
        context.getMethod().getBlock().ifCondition(not(equalsNull(ref("value"))))
                    .exprDotMethod(builder, "append", delimiter)
                    .exprDotMethod(builder, "append", exprDotMethod(ref("value"), "toString"))
                    .assignRef(delimiter, constant(String.valueOf(DELIMITER)));
    }

    public void leave(Object value) {
        if (value != null) {
            builder.delete(0, value.toString().length() + 1);
        }
    }

    public static void applyLeaveCodegen(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        context.getMethod().getBlock().ifCondition(not(equalsNull(ref("value"))))
                    .exprDotMethod(refCol("builder", context.getColumn()), "delete", constant(0), op(exprDotMethodChain(ref("value")).add("toString").add("length"), "+", constant(1)));
    }

    public void clear() {
        builder = new StringBuilder();
    }

    public static void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
        context.getMethod().getBlock().assignRef(refCol("builder", context.getColumn()), newInstance(StringBuilder.class));
    }

    public Object getValue() {
        return builder.toString();
    }

    public static void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
        context.getMethod().getBlock().methodReturn(exprDotMethod(refCol("builder", context.getColumn()), "toString"));
    }
}
