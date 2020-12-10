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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.SortedDoubleVector;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public class AggregatorMedian extends AggregatorMethodWDistinctWFilterWValueBase {
    protected CodegenExpressionMember vector;

    public AggregatorMedian(EPTypeClass optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
    }

    public void initForgeFiltered(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        vector = membersColumnized.addMember(col, SortedDoubleVector.EPTYPE, "vector");
        rowCtor.getBlock().assignRef(vector, newInstance(SortedDoubleVector.EPTYPE));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "add", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, EPType valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "remove", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(value, valueType));
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "add", exprDotMethod(cast(EPTypePremade.NUMBER.getEPType(), value), "doubleValue"));
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "remove", exprDotMethod(cast(EPTypePremade.NUMBER.getEPType(), value), "doubleValue"));
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(vector, "clear");
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(staticMethod(AggregatorMedian.class, "medianCompute", vector));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .staticMethod(SortedDoubleVector.class, "writePoints", output, rowDotMember(row, vector));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .assignRef(rowDotMember(row, vector), staticMethod(SortedDoubleVector.class, "readPoints", input));
    }

    protected void appendFormatWODistinct(FabricTypeCollector collector) {
        collector.sortedDoubleVector();
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
