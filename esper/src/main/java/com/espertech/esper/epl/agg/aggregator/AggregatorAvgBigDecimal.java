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

import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionTypePair;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryAvg;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.cntRefCol;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.sumRefCol;

/**
 * Average that generates a BigDecimal numbers.
 */
public class AggregatorAvgBigDecimal implements AggregationMethod {
    private static final Logger log = LoggerFactory.getLogger(AggregatorAvgBigDecimal.class);
    protected BigDecimal sum;
    protected long cnt;
    protected MathContext optionalMathContext;

    /**
     * Ctor.
     *
     * @param optionalMathContext math context
     */
    public AggregatorAvgBigDecimal(MathContext optionalMathContext) {
        sum = new BigDecimal(0.0);
        this.optionalMathContext = optionalMathContext;
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, BigDecimal.class, "sum");
        membersColumnized.addMember(column, long.class, "cnt");
        ctor.getBlock().assignRef(refCol("sum", column), newInstance(BigDecimal.class, constant(0d)));
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        cnt++;
        if (object instanceof BigInteger) {
            sum = sum.add(new BigDecimal((BigInteger) object));
            return;
        }
        sum = sum.add((BigDecimal) object);
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(true, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sum = sumRefCol(column);
        CodegenExpressionRef cnt = cntRefCol(column);

        if (value.getType() == BigInteger.class) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", newInstance(BigDecimal.class, value.getExpression())));
        } else {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", value.getType() == BigDecimal.class ? value.getExpression() : cast(BigDecimal.class, value.getExpression())));
        }
        method.getBlock().increment(cnt);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }

        if (cnt <= 1) {
            clear();
        } else {
            cnt--;
            if (object instanceof BigInteger) {
                sum = sum.subtract(new BigDecimal((BigInteger) object));
            } else {
                sum = sum.subtract((BigDecimal) object);
            }
        }
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpressionTypePair value = AggregatorCodegenUtil.prefixWithFilterNullDistinctChecks(false, distinct, hasFilter, forges, column, method, symbols, classScope);

        CodegenExpressionRef sum = sumRefCol(column);
        CodegenExpressionRef cnt = cntRefCol(column);

        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clearCode(column))
                .ifElse()
                .decrement(cnt)
                .apply(block -> {
                    if (value.getType() == BigInteger.class) {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", newInstance(BigDecimal.class, value.getExpression())));
                    } else {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", value.getType() == BigDecimal.class ? value.getExpression() : cast(BigDecimal.class, value.getExpression())));
                    }
                });
    }

    public void clear() {
        sum = new BigDecimal(0.0);
        cnt = 0;
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().apply(clearCode(column));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public BigDecimal getValue() {
        return getValueBigDecimalDivide(cnt, optionalMathContext, sum);
    }

    public static void getValueCodegen(AggregationMethodFactoryAvg forge, int column, CodegenMethodNode method, CodegenClassScope classScope) {
        CodegenMember mathContext = classScope.makeAddMember(MathContext.class, forge.getOptionalMathContext());
        method.getBlock().methodReturn(staticMethod(AggregatorAvgBigDecimal.class, "getValueBigDecimalDivide", refCol("cnt", column), member(mathContext.getMemberId()), refCol("sum", column)));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param cnt                 count
     * @param optionalMathContext math ctx
     * @param sum                 sum
     * @return result
     */
    public static BigDecimal getValueBigDecimalDivide(long cnt, MathContext optionalMathContext, BigDecimal sum) {
        if (cnt == 0) {
            return null;
        }
        try {
            if (optionalMathContext == null) {
                return sum.divide(new BigDecimal(cnt));
            }
            return sum.divide(new BigDecimal(cnt), optionalMathContext);
        } catch (ArithmeticException ex) {
            log.error("Error computing avg aggregation result: " + ex.getMessage(), ex);
            return new BigDecimal(0);
        }
    }

    private static Consumer<CodegenBlock> clearCode(int stateNumber) {
        return block ->
                block.assignRef(refCol("sum", stateNumber), newInstance(BigDecimal.class, constant(0d)))
                        .assignRef(refCol("cnt", stateNumber), constant(0));
    }
}
