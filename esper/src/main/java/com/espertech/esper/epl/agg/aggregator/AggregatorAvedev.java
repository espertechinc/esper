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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.Iterator;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AggregatorAvedev implements AggregationMethod {
    private RefCountedSet<Double> valueSet;
    private double sum;

    public AggregatorAvedev() {
        valueSet = new RefCountedSet<Double>();
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, RefCountedSet.class, "valueSet");
        membersColumnized.addMember(column, double.class, "sum");
        ctor.getBlock().assignRef(refCol("valueSet", column), newInstance(RefCountedSet.class));

        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }

        double value = ((Number) object).doubleValue();
        valueSet.add(value);
        sum += value;
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyCodegen(true, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }

        double value = ((Number) object).doubleValue();
        valueSet.remove(value);
        sum -= value;
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyCodegen(false, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void clear() {
        sum = 0;
        valueSet.clear();
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().assignRef(refCol("sum", column), constant(0))
                .exprDotMethod(refCol("valueSet", column), "clear");
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public Object getValue() {
        return computeAvedev(valueSet, sum);
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(staticMethod(AggregatorAvedev.class, "computeAvedev", refCol("valueSet", column), refCol("sum", column)));
    }

    public RefCountedSet<Double> getValueSet() {
        return valueSet;
    }

    public void setValueSet(RefCountedSet<Double> valueSet) {
        this.valueSet = valueSet;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    private static void applyCodegen(boolean enter, boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(enter, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sum = refCol("sum", column);
        CodegenExpressionRef valueSet = refCol("valueSet", column);
        method.getBlock()
                .declareVar(double.class, "val", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value.getExpression(), value.getType()))
                .exprDotMethod(valueSet, enter ? "add" : "remove", ref("val"))
                .assignCompound(sum, enter ? "+" : "-", ref("val"));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param valueSet values
     * @param sum      sum
     * @return value
     */
    public static Object computeAvedev(RefCountedSet<Double> valueSet, double sum) {
        int datapoints = valueSet.size();

        if (datapoints == 0) {
            return null;
        }

        double total = 0;
        double avg = sum / datapoints;

        for (Iterator<Map.Entry<Double, Integer>> it = valueSet.entryIterator(); it.hasNext(); ) {
            Map.Entry<Double, Integer> entry = it.next();
            total += entry.getValue() * Math.abs(entry.getKey() - avg);
        }

        return total / datapoints;
    }
}
