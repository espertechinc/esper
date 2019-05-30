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
package com.espertech.esper.common.internal.epl.agg.method.leaving;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantFalse;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantTrue;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.readBoolean;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.writeBoolean;

public class AggregatorLeaving implements AggregatorMethod {
    private final AggregationForgeFactoryLeaving factory;
    private final CodegenExpressionMember leaving;

    public AggregatorLeaving(AggregationForgeFactoryLeaving factory, int col, CodegenMemberCol membersColumnized) {
        this.factory = factory;
        this.leaving = membersColumnized.addMember(col, boolean.class, "leaving");
    }

    public void applyEvalEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (factory.getAggregationExpression().getPositionalParams().length > 0) {
            AggregatorCodegenUtil.prefixWithFilterCheck(factory.getAggregationExpression().getPositionalParams()[0].getForge(), method, symbols, classScope);
        }
        method.getBlock().assignRef(leaving, constantTrue());
    }

    public void applyTableEnterCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
    }

    public void applyTableLeaveCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(leaving, constantTrue());
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(leaving, constantFalse());
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(leaving);
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(writeBoolean(output, row, leaving));
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().apply(readBoolean(row, leaving, input));
    }
}