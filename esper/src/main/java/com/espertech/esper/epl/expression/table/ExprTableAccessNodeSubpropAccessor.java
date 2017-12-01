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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggregateAccessMultiValueNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Collection;

public class ExprTableAccessNodeSubpropAccessor extends ExprTableAccessNode implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForge {
    private static final long serialVersionUID = 3355957760722481622L;

    private final String subpropName;
    private final ExprNode aggregateAccessMultiValueNode;
    private transient AggregationMethodFactory accessorFactory;

    public ExprTableAccessNodeSubpropAccessor(String tableName, String subpropName, ExprNode aggregateAccessMultiValueNode) {
        super(tableName);
        this.subpropName = subpropName;
        this.aggregateAccessMultiValueNode = aggregateAccessMultiValueNode;
    }

    public ExprAggregateNodeBase getAggregateAccessMultiValueNode() {
        return (ExprAggregateNodeBase) aggregateAccessMultiValueNode;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public ExprForge getForge() {
        return this;
    }

    public Class getEvaluationType() {
        return accessorFactory.getResultType();
    }

    public AggregationAccessor getAccessor(StatementContext statementContext, boolean isFireAndForge) {
        return accessorFactory.getAccessorForge().getAccessor(statementContext.getEngineImportService(), isFireAndForge, statementContext.getStatementName());
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetEventBean(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata) throws ExprValidationException {

        // validate group keys
        validateGroupKeys(tableMetadata, validationContext);
        TableMetadataColumnAggregation column = (TableMetadataColumnAggregation) validateSubpropertyGetCol(tableMetadata, subpropName);

        // validate accessor factory i.e. the parameters types and the match to the required state
        if (column.getAccessAccessorSlotPair() == null) {
            throw new ExprValidationException("Invalid combination of aggregation state and aggregation accessor");
        }
        ExprAggregateAccessMultiValueNode mfNode = (ExprAggregateAccessMultiValueNode) aggregateAccessMultiValueNode;
        mfNode.validatePositionals();
        accessorFactory = mfNode.validateAggregationParamsWBinding(validationContext, column);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTableSubpropAccessor(this, tableName, subpropName, accessorFactory.getAggregationExpression());
            Object result = strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            InstrumentationHelper.get().aExprTableSubpropAccessor(result);
            return result;
        }
        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public String getSubpropName() {
        return subpropName;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return ((ExprAggregateAccessMultiValueNode) aggregateAccessMultiValueNode).getEventTypeCollection(eventAdapterService, statementId);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return ((ExprAggregateAccessMultiValueNode) aggregateAccessMultiValueNode).getComponentTypeCollection();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return ((ExprAggregateAccessMultiValueNode) aggregateAccessMultiValueNode).getEventTypeSingle(eventAdapterService, statementId);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer, subpropName);
        writer.append(".");
        aggregateAccessMultiValueNode.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        ExprTableAccessNodeSubpropAccessor that = (ExprTableAccessNodeSubpropAccessor) other;
        if (!subpropName.equals(that.subpropName)) {
            return false;
        }
        return ExprNodeUtilityCore.deepEquals(aggregateAccessMultiValueNode, that.aggregateAccessMultiValueNode, false);
    }
}
