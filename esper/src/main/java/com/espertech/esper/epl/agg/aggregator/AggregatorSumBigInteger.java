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
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;

import java.math.BigInteger;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.sumAndCountBigApplyEnterCodegen;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.sumAndCountBigApplyLeaveCodegen;

/**
 * Sum for BigInteger values.
 */
public class AggregatorSumBigInteger implements AggregationMethod {
    protected BigInteger sum;
    protected long cnt;

    /**
     * Ctor.
     */
    public AggregatorSumBigInteger() {
        sum = BigInteger.valueOf(0);
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, BigInteger.class, "sum");
        membersColumnized.addMember(column, long.class, "cnt");
        ctor.getBlock().assignRef(refCol("sum", column), staticMethod(BigInteger.class, "valueOf", constant(0)));
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        sumAndCountBigApplyEnterCodegen(BigInteger.class, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        cnt++;
        sum = sum.add((BigInteger) object);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        if (cnt <= 1) {
            clear();
        } else {
            cnt--;
            sum = sum.subtract((BigInteger) object);
        }
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        sumAndCountBigApplyLeaveCodegen(clearCode(column), BigInteger.class, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void clear() {
        sum = BigInteger.valueOf(0);
        cnt = 0;
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().apply(clearCode(column));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public Object getValue() {
        if (cnt == 0) {
            return null;
        }
        return sum;
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().ifCondition(equalsIdentity(refCol("cnt", column), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(refCol("sum", column));
    }

    private static Consumer<CodegenBlock> clearCode(int stateNumber) {
        return block -> block.assignRef(refCol("sum", stateNumber), staticMethod(BigInteger.class, "valueOf", constant(0)))
                .assignRef(refCol("cnt", stateNumber), constant(0));
    }
}
