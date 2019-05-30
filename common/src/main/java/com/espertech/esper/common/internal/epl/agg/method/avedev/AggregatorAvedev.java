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
package com.espertech.esper.common.internal.epl.agg.method.avedev;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.RefCountedSet;
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
import java.util.Iterator;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

public class AggregatorAvedev extends AggregatorMethodWDistinctWFilterWValueBase {
    private CodegenExpressionMember valueSet;
    private CodegenExpressionMember sum;

    public AggregatorAvedev(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        valueSet = membersColumnized.addMember(col, RefCountedSet.class, "valueSet");
        sum = membersColumnized.addMember(col, double.class, "sum");
        rowCtor.getBlock().assignRef(valueSet, newInstance(RefCountedSet.class));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyCodegen(true, value, valueType, method);
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyCodegen(false, value, valueType, method);
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTableCodegen(true, value, method);
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTableCodegen(false, value, method);
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(sum, constant(0))
                .exprDotMethod(valueSet, "clear");
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(staticMethod(AggregatorAvedev.class, "computeAvedev", valueSet, sum));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(writeDouble(output, row, sum))
                .staticMethod(this.getClass(), "writePoints", output, rowDotMember(row, valueSet));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(readDouble(row, sum, input))
                .assignRef(rowDotMember(row, valueSet), staticMethod(this.getClass(), "readPoints", input));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output   output
     * @param valueSet values
     * @throws IOException io error
     */
    public static void writePoints(DataOutput output, RefCountedSet<Double> valueSet) throws IOException {
        Map<Double, Integer> refSet = valueSet.getRefSet();
        output.writeInt(refSet.size());
        output.writeInt(valueSet.getNumValues());
        for (Map.Entry<Double, Integer> entry : valueSet.getRefSet().entrySet()) {
            output.writeDouble(entry.getKey());
            output.writeInt(entry.getValue());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return values
     * @throws IOException io error
     */
    public static RefCountedSet<Double> readPoints(DataInput input) throws IOException {
        RefCountedSet<Double> valueSet = new RefCountedSet<>();
        Map<Double, Integer> und = valueSet.getRefSet();
        int size = input.readInt();
        valueSet.setNumValues(input.readInt());
        for (int i = 0; i < size; i++) {
            Double key = input.readDouble();
            Integer val = input.readInt();
            und.put(key, val);
        }
        return valueSet;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param valueSet values
     * @param sum      sum
     * @return value
     */
    public static Object computeAvedev(RefCountedSet<Double> valueSet, double sum) {
        int datapoints = valueSet.size();

        if (datapoints == 0) {
            return null;
        }

        double total = 0;
        double avg = sum / datapoints;

        for (Iterator<Map.Entry<Double, Integer>> it = valueSet.entryIterator(); it.hasNext(); ) {
            Map.Entry<Double, Integer> entry = it.next();
            total += entry.getValue() * Math.abs(entry.getKey() - avg);
        }

        return total / datapoints;
    }

    private void applyCodegen(boolean enter, CodegenExpression value, Class valueType, CodegenMethod method) {
        method.getBlock()
                .declareVar(double.class, "d", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType))
                .exprDotMethod(valueSet, enter ? "add" : "remove", ref("d"))
                .assignCompound(sum, enter ? "+" : "-", ref("d"));
    }

    private void applyTableCodegen(boolean enter, CodegenExpression value, CodegenMethod method) {
        method.getBlock()
                .declareVar(double.class, "d", exprDotMethod(cast(Number.class, value), "doubleValue"))
                .exprDotMethod(valueSet, enter ? "add" : "remove", ref("d"))
                .assignCompound(sum, enter ? "+" : "-", ref("d"));
    }
}
