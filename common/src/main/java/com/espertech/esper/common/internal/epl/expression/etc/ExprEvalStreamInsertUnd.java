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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalStreamInsertUnd implements ExprForgeInstrumentable {
    private final ExprStreamUnderlyingNode undNode;
    private final int streamNum;
    private final EPTypeClass returnType;

    public ExprEvalStreamInsertUnd(ExprStreamUnderlyingNode undNode, int streamNum, EPTypeClass returnType) {
        this.undNode = undNode;
        this.streamNum = streamNum;
        this.returnType = returnType;
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprStreamUndSelectClause", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, ExprEvalStreamInsertUnd.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
            .ifCondition(equalsNull(refEPS)).blockReturn(constantNull())
            .declareVar(EventBean.EPTYPE, "bean", arrayAtIndex(refEPS, constant(streamNum)))
            .ifRefNullReturnNull("bean")
            .methodReturn(cast(returnType, exprDotUnderlying(ref("bean"))));
        return localMethod(methodNode);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new IllegalStateException("Evaluator not available");
    }

    public EPTypeClass getEvaluationType() {
        return returnType;
    }

    public EPTypeClass getUnderlyingReturnType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return undNode;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
