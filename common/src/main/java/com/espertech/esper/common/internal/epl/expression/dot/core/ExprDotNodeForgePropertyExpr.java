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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterIndexedSPI;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterMappedSPI;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

public class ExprDotNodeForgePropertyExpr extends ExprDotNodeForge {

    private final ExprDotNodeImpl parent;
    private final String statementName;
    private final String propertyName;
    private final int streamNum;
    private final ExprForge exprForge;
    private final Class propertyType;
    private final EventPropertyGetterIndexedSPI indexedGetter;
    private final EventPropertyGetterMappedSPI mappedGetter;

    protected ExprDotNodeForgePropertyExpr(ExprDotNodeImpl parent, String statementName, String propertyName, int streamNum, ExprForge exprForge, Class propertyType, EventPropertyGetterIndexedSPI indexedGetter, EventPropertyGetterMappedSPI mappedGetter) {
        this.parent = parent;
        this.statementName = statementName;
        this.propertyName = propertyName;
        this.streamNum = streamNum;
        this.exprForge = exprForge;
        this.propertyType = propertyType;
        this.indexedGetter = indexedGetter;
        this.mappedGetter = mappedGetter;
    }

    public ExprEvaluator getExprEvaluator() {
        if (indexedGetter != null) {
            return new ExprDotNodeForgePropertyExprEvalIndexed(this, exprForge.getExprEvaluator());
        }
        return new ExprDotNodeForgePropertyExprEvalMapped(this, exprForge.getExprEvaluator());
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (indexedGetter != null) {
            return ExprDotNodeForgePropertyExprEvalIndexed.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        return ExprDotNodeForgePropertyExprEvalMapped.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public Class getEvaluationType() {
        return propertyType;
    }

    public Class getType() {
        return getEvaluationType();
    }

    protected String getWarningText(String expectedType, Object received) {
        return "Statement '" + statementName + "' property " + propertyName + " parameter expression expected a value of " +
                expectedType + " but received " + received == null ? "null" : JavaClassHelper.getClassNameFullyQualPretty(received.getClass());
    }

    public int getStreamNum() {
        return streamNum;
    }

    public EventPropertyGetterIndexedSPI getIndexedGetter() {
        return indexedGetter;
    }

    public EventPropertyGetterMappedSPI getMappedGetter() {
        return mappedGetter;
    }

    public ExprDotNodeImpl getParent() {
        return parent;
    }

    public boolean isReturnsConstantResult() {
        return false;
    }

    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        return null;
    }

    public Integer getStreamNumReferenced() {
        return streamNum;
    }

    public String getRootPropertyName() {
        return null;
    }

    public ExprForge getExprForge() {
        return exprForge;
    }

    public ExprNode getForgeRenderable() {
        return parent;
    }
}
