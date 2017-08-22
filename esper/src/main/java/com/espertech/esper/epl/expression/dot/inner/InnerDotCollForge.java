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
package com.espertech.esper.epl.expression.dot.inner;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.dot.ExprDotEvalRootChildInnerEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalRootChildInnerForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class InnerDotCollForge implements ExprDotEvalRootChildInnerForge {

    private final ExprForge rootForge;

    public InnerDotCollForge(ExprForge rootForge) {
        this.rootForge = rootForge;
    }

    public ExprDotEvalRootChildInnerEval getInnerEvaluator() {
        return new InnerDotCollEval(rootForge.getExprEvaluator());
    }

    public CodegenExpression codegenEvaluate(CodegenMethodNode parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return rootForge.evaluateCodegen(rootForge.getEvaluationType(), parentMethod, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodNode parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodNode parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodNode parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeCollection() {
        return null;
    }

    public Class getComponentTypeCollection() {
        return null;
    }

    public EventType getEventTypeSingle() {
        return null;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfSingleValue(Object.class);
    }
}
