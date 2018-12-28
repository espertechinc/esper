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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;

import java.io.StringWriter;

public class ExprDotNodeAggregationMethodRootNode extends ExprNodeBase implements ExprEnumerationForge, ExprForge {
    private final ExprDotNodeAggregationMethodForge forge;

    public ExprDotNodeAggregationMethodRootNode(ExprDotNodeAggregationMethodForge forge) {
        this.forge = forge;
    }

    public ExprForge getForge() {
        return this;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return forge.getEventTypeCollection();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return forge.getEventTypeSingle();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return forge.getComponentTypeCollection();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.evaluateGetROCollectionEventsCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.evaluateGetEventBeanCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return forge.evaluateGetROCollectionScalarCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // validation already done
        return null;
    }

    public Class getEvaluationType() {
        return forge.getEvaluationType();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        forge.toPrecedenceFreeEPL(writer);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new UnsupportedOperationException("Evaluator not available at compile-time");
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return (writer, parentPrecedence) -> forge.toPrecedenceFreeEPL(writer);
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        throw notAvailableCompileTime();
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    @Override
    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        forge.accept(visitor);
    }

    @Override
    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        forge.accept(visitor);
    }

    @Override
    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        forge.acceptChildnodes(visitor);
    }

    static UnsupportedOperationException notAvailableCompileTime() {
        return new UnsupportedOperationException("Evaluator not available at compile-time");
    }
}
