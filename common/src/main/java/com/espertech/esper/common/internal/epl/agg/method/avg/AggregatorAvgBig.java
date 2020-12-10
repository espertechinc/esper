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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOBigDecimalBigIntegerUtil;
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
    private CodegenExpressionMember sum;
    private CodegenExpressionMember cnt;

    public AggregatorAvgBig(EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, AggregationForgeFactoryAvg factory) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.factory = factory;
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        sum = membersColumnized.addMember(col, EPTypePremade.BIGDECIMAL.getEPType(), "sum");
        cnt = membersColumnized.addMember(col, EPTypePremade.LONGPRIMITIVE.getEPType(), "cnt");
        rowCtor.getBlock().assignRef(sum, newInstance(EPTypePremade.BIGDECIMAL.getEPType(), constant(0d)));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        EPTypeClass valueClass = (EPTypeClass) valueType;
        if (valueClass.getType() == BigInteger.class) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", newInstance(EPTypePremade.BIGDECIMAL.getEPType(), value)));
        } else {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", valueClass.getType() == BigDecimal.class ? value : cast(EPTypePremade.BIGDECIMAL.getEPType(), value)));
        }
        method.getBlock().increment(cnt);
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        EPTypeClass valueClass = (EPTypeClass) valueType;
        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clearCode())
                .ifElse()
                .decrement(cnt)
                .apply(block -> {
                    if (valueClass.getType() == BigInteger.class) {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", newInstance(EPTypePremade.BIGDECIMAL.getEPType(), value)));
                    } else {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", valueClass.getType() == BigDecimal.class ? value : cast(EPTypePremade.BIGDECIMAL.getEPType(), value)));
                    }
                });
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (evaluationTypes[0] == EPTypeNull.INSTANCE) {
            return;
        }
        EPTypeClass valueClass = (EPTypeClass) evaluationTypes[0];
        if (valueClass.getType() == BigInteger.class) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", newInstance(EPTypePremade.BIGDECIMAL.getEPType(), cast(EPTypePremade.BIGINTEGER.getEPType(), value))));
        } else {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", cast(EPTypePremade.BIGDECIMAL.getEPType(), value)));
        }
        method.getBlock().increment(cnt);
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (evaluationTypes[0] == EPTypeNull.INSTANCE) {
            return;
        }
        EPTypeClass valueClass = (EPTypeClass) evaluationTypes[0];
        method.getBlock().ifCondition(relational(cnt, LE, constant(1)))
                .apply(clearCode())
                .ifElse()
                .decrement(cnt)
                .apply(block -> {
                    if (valueClass.getType() == BigInteger.class) {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", newInstance(EPTypePremade.BIGDECIMAL.getEPType(), cast(EPTypePremade.BIGINTEGER.getEPType(), value))));
                    } else {
                        block.assignRef(sum, exprDotMethod(sum, "subtract", cast(EPTypePremade.BIGDECIMAL.getEPType(), value)));
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

    protected void appendFormatWODistinct(FabricTypeCollector collector) {
        collector.builtin(long.class);
        collector.bigDecimal();
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
                block.assignRef(sum, newInstance(EPTypePremade.BIGDECIMAL.getEPType(), constant(0d)))
                        .assignRef(cnt, constant(0));
    }
}
