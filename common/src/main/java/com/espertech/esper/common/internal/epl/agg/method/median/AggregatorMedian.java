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
package com.espertech.esper.common.internal.epl.agg.method.median;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.SortedDoubleVector;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public class AggregatorMedian extends AggregatorMethodWDistinctWFilterWValueBase {
    protected CodegenExpressionMember vector;

    public AggregatorMedian(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        vector = membersColumnized.addMember(col, SortedDoubleVector.class, "vector");
        rowCtor.getBlock().assignRef(vector, newInstance(SortedDoubleVector.class));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "add", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "remove", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "add", exprDotMethod(cast(Number.class, value), "doubleValue"));
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "remove", exprDotMethod(cast(Number.class, value), "doubleValue"));
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "clear");
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(staticMethod(AggregatorMedian.class, "medianCompute", vector));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .staticMethod(this.getClass(), "writePoints", output, rowDotMember(row, vector));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .assignRef(rowDotMember(row, vector), staticMethod(this.getClass(), "readPoints", input));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output out
     * @param vector points
     * @throws IOException io error
     */
    public static void writePoints(DataOutput output, SortedDoubleVector vector) throws IOException {
        output.writeInt(vector.getValues().size());
        for (double num : vector.getValues()) {
            output.writeDouble(num);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return points
     * @throws IOException io error
     */
    public static SortedDoubleVector readPoints(DataInput input) throws IOException {
        SortedDoubleVector points = new SortedDoubleVector();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            double d = input.readDouble();
            points.add(d);
        }
        return points;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param vector vector
     * @return value
     */
    public static Object medianCompute(SortedDoubleVector vector) {
        if (vector.size() == 0) {
            return null;
        }
        if (vector.size() == 1) {
            return vector.getValue(0);
        }

        int middle = vector.size() >> 1;
        if (vector.size() % 2 == 0) {
            return (vector.getValue(middle - 1) + vector.getValue(middle)) / 2;
        } else {
            return vector.getValue(middle);
        }
    }
}
