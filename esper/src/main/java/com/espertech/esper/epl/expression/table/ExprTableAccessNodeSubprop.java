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
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotEnumerationSourceForgeForProps;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnPlain;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Collection;

public class ExprTableAccessNodeSubprop extends ExprTableAccessNode implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForge {
    private static final long serialVersionUID = 1779238498208599159L;

    private final String subpropName;

    private Class bindingReturnType;
    private transient EPType optionalEnumerationType;
    private transient ExprEnumerationGivenEvent optionalPropertyEnumEvaluator;

    public ExprTableAccessNodeSubprop(String tableName, String subpropName) {
        super(tableName);
        this.subpropName = subpropName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return bindingReturnType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetEventBean(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata)
            throws ExprValidationException {
        validateGroupKeys(tableMetadata, validationContext);
        TableMetadataColumn column = validateSubpropertyGetCol(tableMetadata, subpropName);
        if (column instanceof TableMetadataColumnPlain) {
            bindingReturnType = tableMetadata.getInternalEventType().getPropertyType(subpropName);
            ExprDotEnumerationSourceForgeForProps enumerationSource = ExprDotNodeUtility.getPropertyEnumerationSource(subpropName, 0, tableMetadata.getInternalEventType(), true, true);
            optionalEnumerationType = enumerationSource.getReturnType();
            optionalPropertyEnumEvaluator = enumerationSource.getEnumerationGivenEvent();
        } else {
            TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
            optionalEnumerationType = aggcol.getOptionalEnumerationType();
            bindingReturnType = aggcol.getFactory().getResultType();
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTableSubproperty(this, tableName, subpropName);
            Object result = strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            InstrumentationHelper.get().aExprTableSubproperty(result);
            return result;
        }

        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer, subpropName);
    }

    public String getSubpropName() {
        return subpropName;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return EPTypeHelper.optionalIsEventTypeColl(optionalEnumerationType);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return EPTypeHelper.optionalIsComponentTypeColl(optionalEnumerationType);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return EPTypeHelper.optionalIsEventTypeSingle(optionalEnumerationType);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public ExprEnumerationGivenEvent getOptionalPropertyEnumEvaluator() {
        return optionalPropertyEnumEvaluator;
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        ExprTableAccessNodeSubprop that = (ExprTableAccessNodeSubprop) other;
        return subpropName.equals(that.subpropName);
    }
}
