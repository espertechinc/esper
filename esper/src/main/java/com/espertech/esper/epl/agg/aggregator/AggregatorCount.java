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
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;

import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;

/**
 * Counts all datapoints including null values.
 */
public class AggregatorCount implements AggregationMethod {
    protected long cnt;

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, long.class, "cnt");
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        cnt++;
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForge[] forges, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (hasFilter) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forges[forges.length - 1], method, symbols, classScope);
        }

        CodegenExpressionRef cnt = refCol("cnt", column);
        Consumer<CodegenBlock> increment = block -> block.increment(cnt);

        // handle wildcard
        if (forges.length == 0 || (hasFilter && forges.length == 1)) {
            method.getBlock().apply(increment);
            return;
        }

        Class evalType = forges[0].getEvaluationType();
        method.getBlock().declareVar(evalType, "value", forges[0].evaluateCodegen(evalType, method, symbols, classScope));
        if (!evalType.isPrimitive()) {
            method.getBlock().ifRefNull("value").blockReturnNoValue();
        }
        if (distinct) {
            method.getBlock().ifCondition(not(exprDotMethod(refCol("distinctSet", column), "add", ref("value")))).blockReturnNoValue();
        }
        method.getBlock().apply(increment);
    }

    public void leave(Object object) {
        if (cnt > 0) {
            cnt--;
        }
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForge[] forges, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (hasFilter) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forges[forges.length - 1], method, symbols, classScope);
        }

        CodegenExpressionRef cnt = refCol("cnt", column);
        Consumer<CodegenBlock> decrement = block -> block.ifCondition(relational(cnt, GT, constant(0))).decrement(cnt);

        // handle wildcard
        if (forges.length == 0 || (hasFilter && forges.length == 1)) {
            method.getBlock().apply(decrement);
            return;
        }

        Class evalType = forges[0].getEvaluationType();
        method.getBlock().declareVar(evalType, "value", forges[0].evaluateCodegen(evalType, method, symbols, classScope));
        if (!evalType.isPrimitive()) {
            method.getBlock().ifRefNull("value").blockReturnNoValue();
        }
        if (distinct) {
            method.getBlock().ifCondition(not(exprDotMethod(refCol("distinctSet", column), "remove", ref("value")))).blockReturnNoValue();
        }
        method.getBlock().apply(decrement);
    }

    public void clear() {
        cnt = 0;
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().assignRef(refCol("cnt", column), constant(0));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public Object getValue() {
        return cnt;
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().methodReturn(refCol("cnt", column));
    }
}
