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
package com.espertech.esper.common.internal.epl.agg.method.sum;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOBigDecimalBigIntegerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

/**
 * Sum for BigInteger values.
 */
public class AggregatorSumBig extends AggregatorSumBase {

    public AggregatorSumBig(EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, EPTypeClass sumType) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter, sumType);
        if (sumType.getType() != BigInteger.class && sumType.getType() != BigDecimal.class) {
            throw new IllegalArgumentException("Invalid type " + sumType);
        }
    }

    protected CodegenExpression initOfSum() {
        return sumType.getType() == BigInteger.class ? staticMethod(BigInteger.class, "valueOf", constant(0)) : newInstance(EPTypePremade.BIGDECIMAL.getEPType(), constant(0d));
    }

    protected void applyAggEnterSum(CodegenExpressionRef value, EPType valueType, CodegenMethod method) {
        EPTypeClass valueClass = (EPTypeClass) valueType;
        method.getBlock().assignRef(sum, exprDotMethod(sum, "add", valueClass.getType().equals(sumType.getType()) ? value : cast(sumType, value)));
    }

    protected void applyAggLeaveSum(CodegenExpressionRef value, EPType valueType, CodegenMethod method) {
        EPTypeClass valueClass = (EPTypeClass) valueType;
        method.getBlock().assignRef(sum, exprDotMethod(sum, "subtract", valueClass.getType().equals(sumType.getType()) ? value : cast(sumType, value)));
    }

    protected void applyTableEnterSum(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (evaluationTypes[0] != EPTypeNull.INSTANCE) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "add", cast((EPTypeClass) evaluationTypes[0], value)));
        }
    }

    protected void applyTableLeaveSum(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        if (evaluationTypes[0] != EPTypeNull.INSTANCE) {
            method.getBlock().assignRef(sum, exprDotMethod(sum, "subtract", cast((EPTypeClass) evaluationTypes[0], value)));
        }
    }

    protected void writeSum(CodegenExpressionRef row, CodegenExpressionRef output, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType.getType() == BigInteger.class) {
            method.getBlock().staticMethod(DIOBigDecimalBigIntegerUtil.class, "writeBigInt", rowDotMember(row, sum), output);
        } else {
            method.getBlock().staticMethod(DIOBigDecimalBigIntegerUtil.class, "writeBigDec", rowDotMember(row, sum), output);
        }
    }

    protected void readSum(CodegenExpressionRef row, CodegenExpressionRef input, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType.getType() == BigInteger.class) {
            method.getBlock().assignRef(rowDotMember(row, sum), staticMethod(DIOBigDecimalBigIntegerUtil.class, "readBigInt", input));
        } else {
            method.getBlock().assignRef(rowDotMember(row, sum), staticMethod(DIOBigDecimalBigIntegerUtil.class, "readBigDec", input));
        }
    }
}
