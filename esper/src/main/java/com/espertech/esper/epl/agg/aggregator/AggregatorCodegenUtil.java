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

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.SimpleNumberCoercer;

import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class AggregatorCodegenUtil {

    public static CodegenExpressionRef sumRefCol(int column) {
        return refCol("sum", column);
    }

    public static CodegenExpressionRef cntRefCol(int column) {
        return refCol("cnt", column);
    }

    public static void rowMemberSumAndCnt(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, Class sumType) {
        membersColumnized.addMember(column, sumType, "sum");
        membersColumnized.addMember(column, long.class, "cnt");
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public static CodegenExpressionTypePair prefixWithFilterNullDistinctChecks(boolean enter, boolean distinct, boolean hasFilter, ExprForge[] forges, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (hasFilter) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forges[forges.length - 1], method, symbols, classScope);
        }

        Class type = forges[0].getEvaluationType();
        CodegenExpression expr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);
        method.getBlock().declareVar(type, "value", expr);
        if (!type.isPrimitive()) {
            method.getBlock().ifRefNull("value").blockReturnNoValue();
        }
        if (distinct) {
            method.getBlock().ifCondition(not(exprDotMethod(refCol("distinctSet", column), enter ? "add" : "remove", ref("value")))).blockReturnNoValue();
        }

        return new CodegenExpressionTypePair(type, ref("value"));
    }

    public static void prefixWithFilterCheck(ExprForge filterForge, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        Class filterType = filterForge.getEvaluationType();
        method.getBlock().declareVar(filterType, "pass", filterForge.evaluateCodegen(filterType, method, symbols, classScope));
        if (!filterType.isPrimitive()) {
            method.getBlock().ifRefNull("pass").blockReturnNoValue();
        }
        method.getBlock().ifCondition(not(ref("pass"))).blockReturnNoValue();
    }

    public static void sumAndCountApplyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope, SimpleNumberCoercer coercer) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sumRef = sumRefCol(column);
        CodegenExpressionRef cntRef = cntRefCol(column);

        method.getBlock().increment(cntRef)
                .assignRef(sumRef, op(sumRef, "+", coercer.coerceCodegen(value.getExpression(), value.getType())));
    }

    public static void sumAndCountApplyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope, SimpleNumberCoercer coercer) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sumRef = sumRefCol(column);
        CodegenExpressionRef cntRef = cntRefCol(column);
        Consumer<CodegenBlock> clearCode = block -> {
            block.assignRef(sumRef, constant(0)).assignRef(cntRef, constant(0));
        };

        method.getBlock().ifCondition(relational(cntRef, LE, constant(1))).apply(clearCode)
                .ifElse()
                .decrement(cntRef)
                .assignRef(sumRefCol(column), op(sumRefCol(column), "-", coercer.coerceCodegen(value.getExpression(), value.getType())));
    }

    public static void sumAndCountClearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().assignRef(sumRefCol(column), constant(0)).assignRef(cntRefCol(column), constant(0));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public static void getValueSum(int column, CodegenMethodNode method) {
        method.getBlock()
                .ifCondition(equalsIdentity(cntRefCol(column), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(sumRefCol(column));
    }

    protected static void sumAndCountBigApplyEnterCodegen(Class target, boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sum = refCol("sum", column);

        method.getBlock().increment(refCol("cnt", column))
                .assignRef(sum, exprDotMethod(sum, "add", value.getType() == target ? value.getExpression() : cast(target, value.getExpression())));
    }

    protected static void sumAndCountBigApplyLeaveCodegen(Consumer<CodegenBlock> clear, Class target, boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sum = refCol("sum", column);
        CodegenExpressionRef cnt = refCol("cnt", column);

        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clear)
                .ifElse()
                .decrement(refCol("cnt", column))
                .assignRef(sum, exprDotMethod(sum, "subtract", value.getType() == target ? value.getExpression() : cast(target, value.getExpression())));
    }
}
