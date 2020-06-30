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
package com.espertech.esper.common.internal.epl.expression.dot.inner;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalRootChildInnerEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalRootChildInnerForge;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class InnerDotArrPrimitiveToCollForge implements ExprDotEvalRootChildInnerForge {

    protected final ExprForge rootForge;

    public InnerDotArrPrimitiveToCollForge(ExprForge rootForge) {
        this.rootForge = rootForge;
    }

    public ExprDotEvalRootChildInnerEval getInnerEvaluator() {
        return new InnerDotArrPrimitiveToCollEval(rootForge.getExprEvaluator());
    }

    public CodegenExpression codegenEvaluate(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return InnerDotArrPrimitiveToCollEval.codegen(this, parentMethod, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeCollection() {
        return null;
    }

    public EPTypeClass getComponentTypeCollection() {
        return null;
    }

    public EventType getEventTypeSingle() {
        return null;
    }

    public EPChainableType getTypeInfo() {
        EPTypeClass componentType = JavaClassHelper.getArrayComponentType((EPTypeClass) rootForge.getEvaluationType());
        return EPChainableTypeHelper.collectionOfSingleValue(componentType);
    }
}
