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
package com.espertech.esper.common.internal.epl.agg.method.rate;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.*;

/**
 * Aggregation computing an event arrival rate for data windowed-events.
 */
public class AggregatorRate extends AggregatorMethodWDistinctWFilterBase {

    protected AggregationForgeFactoryRate factory;
    protected CodegenExpressionMember accumulator;
    protected CodegenExpressionMember latest;
    protected CodegenExpressionMember oldest;
    protected CodegenExpressionMember isSet;

    public AggregatorRate(AggregationForgeFactoryRate factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.factory = factory;
        accumulator = membersColumnized.addMember(col, double.class, "accumulator");
        latest = membersColumnized.addMember(col, long.class, "latest");
        oldest = membersColumnized.addMember(col, long.class, "oldest");
        isSet = membersColumnized.addMember(col, boolean.class, "isSet");
    }

    protected void applyEvalEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        Class firstType = forges[0].getEvaluationType();
        CodegenExpression firstExpr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);
        method.getBlock().assignRef(latest, SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(firstExpr, firstType));

        int numFilters = factory.getParent().getOptionalFilter() != null ? 1 : 0;
        if (forges.length == numFilters + 1) {
            method.getBlock().increment(accumulator);
        } else {
            Class secondType = forges[1].getEvaluationType();
            CodegenExpression secondExpr = forges[1].evaluateCodegen(double.class, method, symbols, classScope);
            method.getBlock().assignCompound(accumulator, "+", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(secondExpr, secondType));
        }
    }

    protected void applyEvalLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        int numFilters = factory.getParent().getOptionalFilter() != null ? 1 : 0;

        Class firstType = forges[0].getEvaluationType();
        CodegenExpression firstExpr = forges[0].evaluateCodegen(long.class, method, symbols, classScope);

        method.getBlock().assignRef(oldest, SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(firstExpr, firstType))
                .ifCondition(not(isSet)).assignRef(isSet, constantTrue());
        if (forges.length == numFilters + 1) {
            method.getBlock().decrement(accumulator);
        } else {
            Class secondType = forges[1].getEvaluationType();
            CodegenExpression secondExpr = forges[1].evaluateCodegen(double.class, method, symbols, classScope);
            method.getBlock().assignCompound(accumulator, "-", SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(secondExpr, secondType));
        }
    }

    protected void applyTableEnterFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        throw new UnsupportedOperationException("Not available with tables");
    }

    protected void applyTableLeaveFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        throw new UnsupportedOperationException("Not available with tables");
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(accumulator, constant(0))
                .assignRef(latest, constant(0))
                .assignRef(oldest, constant(0));
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(not(isSet)).blockReturn(constantNull())
                .methodReturn(op(op(accumulator, "*", constant(factory.getTimeAbacus().getOneSecond())), "/", op(latest, "-", oldest)));
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(writeDouble(output, row, accumulator))
                .apply(writeLong(output, row, latest))
                .apply(writeLong(output, row, oldest))
                .apply(writeBoolean(output, row, isSet));
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
                .apply(readDouble(row, accumulator, input))
                .apply(readLong(row, latest, input))
                .apply(readLong(row, oldest, input))
                .apply(readBoolean(row, isSet, input));
    }
}