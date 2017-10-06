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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryRate;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Aggregation computing an event arrival rate for data windowed-events.
 */
public class AggregatorRate implements AggregationMethod {

    protected final long oneSecondTime;
    protected double accumulator;
    protected long latest;
    protected long oldest;
    protected boolean isSet = false;

    public AggregatorRate(long oneSecondTime) {
        this.oneSecondTime = oneSecondTime;
    }

    public static void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, double.class, "accumulator");
        membersColumnized.addMember(column, long.class, "latest");
        membersColumnized.addMember(column, long.class, "oldest");
        membersColumnized.addMember(column, boolean.class, "isSet");
    }

    public void enter(Object value) {
        if (value.getClass().isArray()) {
            enterValueArr((Object[]) value);
        } else {
            enterValueSingle(value);
        }
    }

    public static void applyEnterCodegen(AggregationMethodFactoryRate forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        int numFilters = forge.getParent().getOptionalFilter() != null ? 1 : 0;
        if (numFilters == 1) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getParent().getOptionalFilter().getForge(), method, symbols, classScope);
        }

        CodegenExpressionRef accumulator = refCol("accumulator", column);
        CodegenExpressionRef latest = refCol("latest", column);

        Class firstType = forges[0].getEvaluationType();
        CodegenExpression firstExpr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);
        method.getBlock().assignRef(latest, SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(firstExpr, firstType));

        if (forges.length == numFilters + 1) {
            method.getBlock().increment(accumulator);
        } else {
            Class secondType = forges[1].getEvaluationType();
            CodegenExpression secondExpr = forges[1].evaluateCodegen(double.class, method, symbols, classScope);
            method.getBlock().assignCompound(accumulator, "+", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(secondExpr, secondType));
        }
    }

    public void leave(Object value) {
        if (value.getClass().isArray()) {
            leaveValueArr((Object[]) value);
        } else {
            leaveValueSingle(value);
        }
    }

    public static void applyLeaveCodegen(AggregationMethodFactoryRate forge, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        int numFilters = forge.getParent().getOptionalFilter() != null ? 1 : 0;
        if (numFilters == 1) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getParent().getOptionalFilter().getForge(), method, symbols, classScope);
        }

        CodegenExpressionRef accumulator = refCol("accumulator", column);
        CodegenExpressionRef oldest = refCol("oldest", column);
        CodegenExpressionRef isSet = refCol("isSet", column);

        Class firstType = forges[0].getEvaluationType();
        CodegenExpression firstExpr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);

        method.getBlock().assignRef(oldest, SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(firstExpr, firstType))
                .ifCondition(not(isSet)).assignRef(isSet, constantTrue());
        if (forges.length == numFilters + 1) {
            method.getBlock().decrement(accumulator);
        } else {
            Class secondType = forges[1].getEvaluationType();
            CodegenExpression secondExpr = forges[1].evaluateCodegen(double.class, method, symbols, classScope);
            method.getBlock().assignCompound(accumulator, "-", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(secondExpr, secondType));
        }
    }

    public void clear() {
        accumulator = 0;
        latest = 0;
        oldest = 0;
    }

    public static void clearCodegen(int column, CodegenMethodNode method) {
        method.getBlock().assignRef(refCol("accumulator", column), constant(0))
                .assignRef(refCol("latest", column), constant(0))
                .assignRef(refCol("oldest", column), constant(0));
    }

    public Object getValue() {
        if (!isSet) return null;
        return (accumulator * oneSecondTime) / (latest - oldest);
    }

    public static void getValueCodegen(AggregationMethodFactoryRate forge, int column, CodegenMethodNode method) {
        CodegenExpressionRef accumulator = refCol("accumulator", column);
        CodegenExpressionRef latest = refCol("latest", column);
        CodegenExpressionRef oldest = refCol("oldest", column);
        CodegenExpressionRef isSet = refCol("isSet", column);

        method.getBlock().ifCondition(not(isSet)).blockReturn(constantNull())
                .methodReturn(op(op(accumulator, "*", constant(forge.getTimeAbacus().getOneSecond())), "/", op(latest, "-", oldest)));
    }

    public long getOneSecondTime() {
        return oneSecondTime;
    }

    public double getAccumulator() {
        return accumulator;
    }

    public void setAccumulator(double accumulator) {
        this.accumulator = accumulator;
    }

    public long getLatest() {
        return latest;
    }

    public void setLatest(long latest) {
        this.latest = latest;
    }

    public long getOldest() {
        return oldest;
    }

    public void setOldest(long oldest) {
        this.oldest = oldest;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    protected void enterValueSingle(Object value) {
        accumulator += 1;
        latest = (Long) value;
    }

    protected void enterValueArr(Object[] parameters) {
        Number val = (Number) parameters[1];
        accumulator += val.doubleValue();
        latest = (Long) parameters[0];
    }

    protected void leaveValueArr(Object[] parameters) {
        Number val = (Number) parameters[1];
        accumulator -= val.doubleValue();
        oldest = (Long) parameters[0];
        if (!isSet) isSet = true;
    }

    protected void leaveValueSingle(Object value) {
        accumulator -= 1;
        oldest = (Long) value;
        if (!isSet) isSet = true;
    }
}