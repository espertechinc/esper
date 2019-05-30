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
package com.espertech.esper.common.internal.epl.agg.method.avg;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOBigDecimalBigIntegerUtil;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.type.MathContextCodegenField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

/**
 * Average that generates double-typed numbers.
 */
public class AggregatorAvgBig extends AggregatorMethodWDistinctWFilterWValueBase {

    private static final Logger log = LoggerFactory.getLogger(AggregatorAvgBig.class);

    private final AggregationForgeFactoryAvg factory;
    private final CodegenExpressionMember sum;
    private final CodegenExpressionMember cnt;

    public AggregatorAvgBig(AggregationForgeFactoryAvg factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.factory = factory;
        sum = membersColumnized.addMember(col, BigDecimal.class, "sum");
        cnt = membersColumnized.addMember(col, long.class, "cnt");
        rowCtor.getBlock().assignRef(sum, newInstance(BigDecimal.class, constant(0d)));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (valueType == BigInteger.class) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", newInstance(BigDecimal.class, value)));
        } else {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", valueType == BigDecimal.class ? value : cast(BigDecimal.class, value)));
        }
        method.getBlock().increment(cnt);
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clearCode())
                .ifElse()
                .decrement(cnt)
                .apply(block -> {
                    if (valueType == BigInteger.class) {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", newInstance(BigDecimal.class, value)));
                    } else {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", valueType == BigDecimal.class ? value : cast(BigDecimal.class, value)));
                    }
                });
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (evaluationTypes[0] == BigInteger.class) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", newInstance(BigDecimal.class, cast(BigInteger.class, value))));
        } else {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", cast(BigDecimal.class, value)));
        }
        method.getBlock().increment(cnt);
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clearCode())
                .ifElse()
                .decrement(cnt)
                .apply(block -> {
                    if (evaluationTypes[0] == BigInteger.class) {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", newInstance(BigDecimal.class, cast(BigInteger.class, value))));
                    } else {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", cast(BigDecimal.class, value)));
                    }
                });
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(clearCode());
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpression math = factory.optionalMathContext == null ? constantNull() : classScope.addOrGetFieldSharable(new MathContextCodegenField(factory.optionalMathContext));
        method.getBlock().methodReturn(staticMethod(this.getClass(), "getValueBigDecimalDivide", cnt, math, sum));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(writeLong(output, row, cnt))
                .staticMethod(DIOBigDecimalBigIntegerUtil.class, "writeBigDec", rowDotMember(row, sum), output);
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(readLong(row, cnt, input))
                .assignRef(rowDotMember(row, sum), staticMethod(DIOBigDecimalBigIntegerUtil.class, "readBigDec", input));
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

    private Consumer<CodegenBlock> clearCode() {
        return block ->
                block.assignRef(sum, newInstance(BigDecimal.class, constant(0d)))
                        .assignRef(cnt, constant(0));
    }
}
