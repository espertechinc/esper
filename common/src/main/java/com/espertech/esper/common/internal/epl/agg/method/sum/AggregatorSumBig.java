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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.DIOSerdeBigDecimalBigInteger;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotRef;

/**
 * Sum for BigInteger values.
 */
public class AggregatorSumBig extends AggregatorSumBase {

    public AggregatorSumBig(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, boolean hasFilter, ExprNode optionalFilter, Class sumType) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, hasFilter, optionalFilter, sumType);
        if (sumType != BigInteger.class && sumType != BigDecimal.class) {
            throw new IllegalArgumentException("Invalid type " + sumType);
        }
    }

    protected CodegenExpression initOfSum() {
        return sumType == BigInteger.class ? staticMethod(BigInteger.class, "valueOf", constant(0)) : newInstance(BigDecimal.class, constant(0d));
    }

    protected void applyAggEnterSum(CodegenExpressionRef value, Class valueType, CodegenMethod method) {
        method.getBlock().assignRef(sum, exprDotMethod(sum, "add", valueType == sumType ? value : cast(sumType, value)));
    }

    protected void applyAggLeaveSum(CodegenExpressionRef value, Class valueType, CodegenMethod method) {
        method.getBlock().assignRef(sum, exprDotMethod(sum, "subtract", valueType == sumType ? value : cast(sumType, value)));
    }

    protected void applyTableEnterSum(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(sum, exprDotMethod(sum, "add", cast(evaluationTypes[0], value)));
    }

    protected void applyTableLeaveSum(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(sum, exprDotMethod(sum, "subtract", cast(evaluationTypes[0], value)));
    }

    protected void writeSum(CodegenExpressionRef row, CodegenExpressionRef output, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType == BigInteger.class) {
            method.getBlock().staticMethod(DIOSerdeBigDecimalBigInteger.class, "writeBigInt", rowDotRef(row, sum), output);
        } else {
            method.getBlock().staticMethod(DIOSerdeBigDecimalBigInteger.class, "writeBigDec", rowDotRef(row, sum), output);
        }
    }

    protected void readSum(CodegenExpressionRef row, CodegenExpressionRef input, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType == BigInteger.class) {
            method.getBlock().assignRef(rowDotRef(row, sum), staticMethod(DIOSerdeBigDecimalBigInteger.class, "readBigInt", input));
        } else {
            method.getBlock().assignRef(rowDotRef(row, sum), staticMethod(DIOSerdeBigDecimalBigInteger.class, "readBigDec", input));
        }
    }
}
