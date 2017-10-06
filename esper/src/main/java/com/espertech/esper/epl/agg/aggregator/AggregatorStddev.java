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
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LT;

/**
 * Standard deviation always generates double-typed numbers.
 */
public class AggregatorStddev implements AggregationMethod {
    protected double mean;
    protected double qn;
    protected long cnt;

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, double.class, "mean");
        membersColumnized.addMember(column, double.class, "qn");
        membersColumnized.addMember(column, long.class, "cnt");
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }

        double p = ((Number) object).doubleValue();

        // compute running variance per Knuth's method
        if (cnt == 0) {
            mean = p;
            qn = 0;
            cnt = 1;
        } else {
            cnt++;
            double oldmean = mean;
            mean += (p - mean) / cnt;
            qn += (p - oldmean) * (p - mean);
        }
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef mean = refCol("mean", column);
        CodegenExpressionRef qn = refCol("qn", column);
        CodegenExpressionRef cnt = refCol("cnt", column);

        method.getBlock().declareVar(double.class, "p", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value.getExpression(), value.getType()))
                .ifCondition(equalsIdentity(cnt, constant(0)))
                .assignRef(mean, ref("p"))
                .assignRef(qn, constant(0))
                .assignRef(cnt, constant(1))
                .ifElse()
                .increment(cnt)
                .declareVar(double.class, "oldmean", mean)
                .assignCompound(mean, "+", op(op(ref("p"), "-", mean), "/", cnt))
                .assignCompound(qn, "+", op(op(ref("p"), "-", ref("oldmean")), "*", op(ref("p"), "-", mean)));
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }

        double p = ((Number) object).doubleValue();

        // compute running variance per Knuth's method
        if (cnt <= 1) {
            clear();
        } else {
            cnt--;
            double oldmean = mean;
            mean -= (p - mean) / cnt;
            qn -= (p - oldmean) * (p - mean);
        }
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef mean = refCol("mean", column);
        CodegenExpressionRef qn = refCol("qn", column);
        CodegenExpressionRef cnt = refCol("cnt", column);

        Consumer<CodegenBlock> clear = block -> {
            block.assignRef(mean, constant(0))
                    .assignRef(qn, constant(0))
                    .assignRef(cnt, constant(0));
        };

        method.getBlock().declareVar(double.class, "p", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value.getExpression(), value.getType()))
                .ifCondition(relational(cnt, LE, constant(1)))
                .apply(clear)
                .ifElse()
                .decrement(cnt)
                .declareVar(double.class, "oldmean", mean)
                .assignCompound(mean, "-", op(op(ref("p"), "-", mean), "/", cnt))
                .assignCompound(qn, "-", op(op(ref("p"), "-", ref("oldmean")), "*", op(ref("p"), "-", mean)));
    }

    public void clear() {
        mean = 0;
        cnt = 0;
        qn = 0;
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().assignRef(refCol("mean", column), constant(0))
            .assignRef(refCol("qn", column), constant(0))
            .assignRef(refCol("cnt", column), constant(0));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public Object getValue() {
        if (cnt < 2) {
            return null;
        }
        return Math.sqrt(qn / (cnt - 1));
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        CodegenExpressionRef qn = refCol("qn", column);
        CodegenExpressionRef cnt = refCol("cnt", column);
        method.getBlock().ifCondition(relational(cnt, LT, constant(2)))
                .blockReturn(constantNull())
                .methodReturn(staticMethod(Math.class, "sqrt", op(qn, "/", op(cnt, "-", constant(1)))));
    }
}
