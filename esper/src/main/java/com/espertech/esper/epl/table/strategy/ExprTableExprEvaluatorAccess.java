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
package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprTableExprEvaluatorAccess extends ExprTableExprEvaluatorBase implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval {

    private final AggregationAccessorSlotPair accessAccessorSlotPair;
    private final EventType eventTypeColl;

    public ExprTableExprEvaluatorAccess(ExprNode exprNode, String tableName, String subpropName, int streamNum, Class returnType, AggregationAccessorSlotPair accessAccessorSlotPair, EventType eventTypeColl) {
        super(exprNode, tableName, subpropName, streamNum, returnType);
        this.accessAccessorSlotPair = accessAccessorSlotPair;
        this.eventTypeColl = eventTypeColl;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTableSubproperty(exprNode, tableName, subpropName);
        }

        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) eventsPerStream[streamNum];
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(oa);
        Object result = accessAccessorSlotPair.getAccessor().getValue(row.getStates()[accessAccessorSlotPair.getSlot()], eventsPerStream, isNewData, context);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprTableSubproperty(result);
        }
        return result;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return eventTypeColl;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) eventsPerStream[streamNum];
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(oa);
        return accessAccessorSlotPair.getAccessor().getEnumerableEvents(row.getStates()[accessAccessorSlotPair.getSlot()], eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return exprNode;
    }
}
