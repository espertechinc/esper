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

public class SupportPluginAggregationMethodOne implements AggregationMethod, Serializable {
    private int count;

    public static void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
        context.getMembersColumnized().addMember(context.getColumn(), int.class, "count");
    }

    public void enter(Object value) {
        count--;
    }

    public static void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        context.getMethod().getBlock().decrement(refCol("count", context.getColumn()));
    }

    public void leave(Object value) {
        count++;
    }

    public static void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        context.getMethod().getBlock().increment(refCol("count", context.getColumn()));
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
}
