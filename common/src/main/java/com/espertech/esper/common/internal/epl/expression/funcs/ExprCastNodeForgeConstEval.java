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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprCastNodeForgeConstEval implements ExprEvaluator {
    private final ExprCastNodeForge forge;
    private final Object theConstant;

    public ExprCastNodeForgeConstEval(ExprCastNodeForge forge, Object theConstant) {
        this.forge = forge;
        this.theConstant = theConstant;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return theConstant;
    }

    public static CodegenExpression codegen(ExprCastNodeForge forge, CodegenClassScope codegenClassScope) {
        if (forge.getEvaluationType() == null) {
            return constantNull();
        }

        Class evaluationType = forge.getEvaluationType();
        CodegenMethod initMethod = codegenClassScope.getPackageScope().getInitMethod().makeChildWithScope(evaluationType, ExprCastNodeForgeConstEval.class, CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod compute = initMethod.makeChildWithScope(evaluationType, ExprCastNodeForgeConstEval.class, exprSymbol, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);
        compute.getBlock().methodReturn(ExprCastNodeForgeNonConstEval.codegen(forge, compute, exprSymbol, codegenClassScope));

        initMethod.getBlock().methodReturn(localMethod(compute, constant(null), constantTrue(), constantNull()));

        return codegenClassScope.addFieldUnshared(true, evaluationType, localMethod(initMethod));
    }
}
