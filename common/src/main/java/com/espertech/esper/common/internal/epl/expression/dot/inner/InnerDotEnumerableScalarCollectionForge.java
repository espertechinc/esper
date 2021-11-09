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
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalRootChildInnerEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalRootChildInnerForge;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class InnerDotEnumerableScalarCollectionForge implements ExprDotEvalRootChildInnerForge {

    protected final ExprEnumerationForge rootLambdaForge;
    protected final EPTypeClass componentType;

    public InnerDotEnumerableScalarCollectionForge(ExprEnumerationForge rootLambdaForge, EPTypeClass componentType) {
        this.rootLambdaForge = rootLambdaForge;
        this.componentType = componentType;
    }

    public ExprDotEvalRootChildInnerEval getInnerEvaluator() {
        return new InnerDotEnumerableScalarCollectionEval(rootLambdaForge.getExprEvaluatorEnumeration());
    }

    public CodegenExpression codegenEvaluate(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression asCollection = cast(EPTypePremade.COLLECTION.getEPType(), rootLambdaForge.evaluateGetROCollectionScalarCodegen(parentMethod, exprSymbol, codegenClassScope));
        return cast(new EPTypeClassParameterized(Collection.class, componentType), asCollection);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return rootLambdaForge.evaluateGetROCollectionEventsCodegen(parentMethod, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return rootLambdaForge.evaluateGetROCollectionScalarCodegen(parentMethod, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethod parentMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeCollection() {
        return null;
    }

    public EPTypeClass getComponentTypeCollection() {
        return componentType;
    }

    public EventType getEventTypeSingle() {
        return null;
    }

    public EPChainableType getTypeInfo() {
        return EPChainableTypeHelper.collectionOfSingleValue(componentType);
    }
}
