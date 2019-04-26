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
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;
import static com.espertech.esper.common.internal.epl.agg.method.sum.AggregationForgeFactorySum.getCoercerNonBigIntDec;

public class AggregatorSumNonBig extends AggregatorSumBase {

    public AggregatorSumNonBig(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, Class sumType) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter, sumType);
        if (!Arrays.asList(Double.class, Long.class, Integer.class, Float.class).contains(sumType)) {
            throw new IllegalArgumentException("Invalid sum type " + sumType);
        }
    }

    protected CodegenExpression initOfSum() {
        return constant(0);
    }

    protected void applyAggEnterSum(CodegenExpressionRef value, Class valueType, CodegenMethod method) {
        applyAgg(true, value, valueType, method);
    }

    protected void applyTableEnterSum(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTable(true, value, method, classScope);
    }

    protected void applyAggLeaveSum(CodegenExpressionRef value, Class valueType, CodegenMethod method) {
        applyAgg(false, value, valueType, method);
    }

    protected void applyTableLeaveSum(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTable(false, value, method, classScope);
    }

    protected void writeSum(CodegenExpressionRef row, CodegenExpressionRef output, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType == Double.class) {
            method.getBlock().apply(writeDouble(output, row, sum));
        } else if (sumType == Long.class) {
            method.getBlock().apply(writeLong(output, row, sum));
        } else if (sumType == Integer.class) {
            method.getBlock().apply(writeInt(output, row, sum));
        } else if (sumType == Float.class) {
            method.getBlock().apply(writeFloat(output, row, sum));
        } else {
            throw new IllegalStateException("Unrecognized sum type " + sumType);
        }
    }

    protected void readSum(CodegenExpressionRef row, CodegenExpressionRef input, CodegenMethod method, CodegenClassScope classScope) {
        if (sumType == Double.class) {
            method.getBlock().apply(readDouble(row, sum, input));
        } else if (sumType == Long.class) {
            method.getBlock().apply(readLong(row, sum, input));
        } else if (sumType == Integer.class) {
            method.getBlock().apply(readInt(row, sum, input));
        } else if (sumType == Float.class) {
            method.getBlock().apply(readFloat(row, sum, input));
        } else {
            throw new IllegalStateException("Unrecognized sum type " + sumType);
        }
    }

    private void applyAgg(boolean enter, CodegenExpressionRef value, Class valueType, CodegenMethod method) {
        SimpleNumberCoercer coercer = getCoercerNonBigIntDec(valueType);
        method.getBlock().assignRef(sum, op(sum, enter ? "+" : "-", coercer.coerceCodegen(value, valueType)));
    }

    private void applyTable(boolean enter, CodegenExpressionRef value, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(sum, op(sum, enter ? "+" : "-", cast(sumType, value)));
    }
}