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
package com.espertech.esper.common.internal.epl.expression.agg.accessagg;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionHandler;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionValidationContext;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.agg.access.plugin.AggregationForgeFactoryAccessPlugin;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprPlugInAggNodeMarker;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

/**
 * Represents a custom aggregation function in an expresson tree.
 */
public class ExprPlugInMultiFunctionAggNode extends ExprAggregateNodeBase implements ExprEnumerationEval, ExprAggMultiFunctionNode, ExprPlugInAggNodeMarker {
    private final AggregationMultiFunctionForge aggregationMultiFunctionForge;
    private final String functionName;
    private final ConfigurationCompilerPlugInAggregationMultiFunction config;
    private AggregationForgeFactoryAccessPlugin factory;

    public ExprPlugInMultiFunctionAggNode(boolean distinct, ConfigurationCompilerPlugInAggregationMultiFunction config, AggregationMultiFunctionForge aggregationMultiFunctionForge, String functionName) {
        super(distinct);
        this.aggregationMultiFunctionForge = aggregationMultiFunctionForge;
        this.functionName = functionName;
        this.config = config;
    }

    protected AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        validatePositionals(validationContext);
        // validate using the context provided by the 'outside' streams to determine parameters
        // at this time 'inside' expressions like 'window(intPrimitive)' are not handled
        ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, this.getChildNodes(), validationContext);
        AggregationMultiFunctionValidationContext ctx = new AggregationMultiFunctionValidationContext(functionName, validationContext.getStreamTypeService().getEventTypes(), positionalParams, validationContext.getStatementName(), validationContext, config, null, getChildNodes(), optionalFilter);
        AggregationMultiFunctionHandler handlerPlugin = aggregationMultiFunctionForge.validateGetHandler(ctx);
        factory = new AggregationForgeFactoryAccessPlugin(this, handlerPlugin);
        return factory;
    }

    public String getAggregationFunctionName() {
        return functionName;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }

    protected boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return false;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return factory.getComponentTypeCollection();
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return factory.getEventTypeCollection();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return factory.getEventTypeSingle();
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getCollectionOfEvents", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getCollectionScalar", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getEventBean", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public AggregationForgeFactory getAggregationForgeFactory() {
        return factory;
    }
}
