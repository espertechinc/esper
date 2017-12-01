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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprNewStructNodeForge implements ExprTypableReturnForge {

    private final ExprNewStructNode parent;
    private final boolean isAllConstants;
    private final LinkedHashMap eventType;

    public ExprNewStructNodeForge(ExprNewStructNode parent, boolean isAllConstants, LinkedHashMap eventType) {
        this.parent = parent;
        this.isAllConstants = isAllConstants;
        this.eventType = eventType;
    }

    public ExprEvaluator getExprEvaluator() {
        ExprEvaluator[] evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        return new ExprNewStructNodeForgeEval(this, evaluators);
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return (ExprTypableReturnEval) getExprEvaluator();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprNewStructNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return Map.class;
    }

    public boolean isAllConstants() {
        return isAllConstants;
    }

    public LinkedHashMap getEventType() {
        return eventType;
    }

    public ExprNewStructNode getForgeRenderable() {
        return parent;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return eventType;
    }

    public Boolean isMultirow() {
        return false;   // New itself can only return a single row
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprNewStructNodeForgeEval.codegenTypeableSingle(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
