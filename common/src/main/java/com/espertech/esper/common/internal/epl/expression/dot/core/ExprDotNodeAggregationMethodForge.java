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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationMethodCodegenField;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public abstract class ExprDotNodeAggregationMethodForge extends ExprDotNodeForge {

    protected final ExprDotNodeImpl parent;
    protected final String aggregationMethodName;
    protected final ExprNode[] parameters;
    protected final AggregationPortableValidation validation;
    protected AggregationMultiFunctionMethodDesc methodDesc;

    protected abstract CodegenExpression evaluateCodegen(String readerMethodName, Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope);
    protected abstract void toEPL(StringWriter writer);
    protected abstract String getTableName();
    protected abstract String getTableColumnName();

    public ExprDotNodeAggregationMethodForge(ExprDotNodeImpl parent, String aggregationMethodName, ExprNode[] parameters, AggregationPortableValidation validation) {
        this.parent = parent;
        this.aggregationMethodName = aggregationMethodName;
        this.parameters = parameters;
        this.validation = validation;
    }

    public void validate(ExprValidationContext validationContext) throws ExprValidationException  {
        methodDesc = validation.validateAggregationMethod(validationContext, aggregationMethodName, parameters);
    }

    public Class getEvaluationType() {
        return methodDesc.getReader().getResultType();
    }

    public boolean isReturnsConstantResult() {
        return false;
    }

    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        return null;
    }

    public Integer getStreamNumReferenced() {
        return null;
    }

    public String getRootPropertyName() {
        return null;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return evaluateCodegen("getValue", requiredType, parent, symbols, classScope);
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprDotNodeAggregationMethodRootNode.notAvailableCompileTime();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return (writer, parentPrecedence) -> toPrecedenceFreeEPL(writer);
    }

    protected CodegenExpressionField getReader(CodegenClassScope classScope) {
        return classScope.addOrGetFieldSharable(new AggregationMethodCodegenField(methodDesc.getReader(), classScope, this.getClass()));
    }

    public EventType getEventTypeCollection() {
        return methodDesc.getEventTypeCollection();
    }

    public EventType getEventTypeSingle() {
        return methodDesc.getEventTypeSingle();
    }

    public Class getComponentTypeCollection() {
        return methodDesc.getComponentTypeCollection();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprTableSubpropAccessor", requiredType, parent, symbols, classScope)
            .qparam(constant(getTableName())) // table name
            .qparam(constant(getTableColumnName())) // subprop name
            .qparam(constant(aggregationMethodName)) // agg expression
            .build();
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return evaluateCodegen("getValueCollectionEvents", Collection.class, parent, symbols, classScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return evaluateCodegen("getValueCollectionScalar", Collection.class, parent, symbols, classScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return evaluateCodegen("getValueEventBean", EventBean.class, parent, symbols, classScope);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toEPL(writer);
        writer.append(".").append(aggregationMethodName).append("(");
        ExprNodeUtilityPrint.toExpressionStringParameterList(parameters, writer);
        writer.append(")");
    }

    public void accept(ExprNodeVisitor visitor) {
        for (ExprNode parameter : parameters) {
            parameter.accept(visitor);
        }
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        for (ExprNode parameter : parameters) {
            parameter.accept(visitor);
        }
    }

    public void acceptChildnodes(ExprNodeVisitorWithParent visitor) {
        for (ExprNode parameter : parameters) {
            parameter.acceptChildnodes(visitor, parent);
        }
    }
}
