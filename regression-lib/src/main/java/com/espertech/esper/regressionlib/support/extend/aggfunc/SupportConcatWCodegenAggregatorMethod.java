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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodFactoryContext;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SupportConcatWCodegenAggregatorMethod implements AggregatorMethod {

    private final CodegenExpressionMember builder;

    public SupportConcatWCodegenAggregatorMethod(AggregatorMethodFactoryContext context) {
        builder = context.getMembersColumnized().addMember(context.getCol(), EPTypePremade.STRINGBUILDER.getEPType(), "buf");
        context.getRowCtor().getBlock().assignRef(builder, newInstance(EPTypePremade.STRINGBUILDER.getEPType()));
    }

    public void applyEvalEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock()
            .declareVar(EPTypePremade.STRING.getEPType(), "value", cast(EPTypePremade.STRING.getEPType(), forges[0].evaluateCodegen(EPTypePremade.STRING.getEPType(), method, symbols, classScope)))
            .exprDotMethod(builder, "append", ref("value"));
    }

    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {

    }

    public void applyTableEnterCodegen(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {

    }

    public void applyTableLeaveCodegen(CodegenExpressionRef value, EPType[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {

    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(builder, newInstance(EPTypePremade.STRINGBUILDER.getEPType()));
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(exprDotMethod(builder, "toString"));
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {

    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {

    }
}