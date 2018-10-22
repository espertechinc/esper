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
package com.espertech.esper.common.internal.avro.selectexprrep;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorAvro implements SelectExprProcessorForge {
    private final EventType resultEventTypeAvro;

    public SelectExprJoinWildcardProcessorAvro(EventType resultEventTypeAvro) {
        this.resultEventTypeAvro = resultEventTypeAvro;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventTypeOuter, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        // NOTE: Maintaining result-event-type as out own field as we may be an "inner" select-expr-processor
        CodegenExpressionField mType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(resultEventTypeAvro, EPStatementInitServices.REF));
        CodegenExpressionField schema = codegenClassScope.getPackageScope().addFieldUnshared(true, Schema.class, staticMethod(AvroSchemaUtil.class, "resolveAvroSchema", EventTypeUtility.resolveTypeCodegen(resultEventTypeAvro, EPStatementInitServices.REF)));
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock().methodReturn(staticMethod(SelectExprJoinWildcardProcessorAvro.class, "processSelectExprJoinWildcardAvro", refEPS, schema, eventBeanFactory, mType));
        return methodNode;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventsPerStream     events
     * @param schema              schema
     * @param eventAdapterService event svc
     * @param resultEventType     result event type
     * @return bean
     */
    public static EventBean processSelectExprJoinWildcardAvro(EventBean[] eventsPerStream, Schema schema, EventBeanTypedEventFactory eventAdapterService, EventType resultEventType) {
        GenericData.Record event = new GenericData.Record(schema);
        for (int i = 0; i < eventsPerStream.length; i++) {
            EventBean streamEvent = eventsPerStream[i];
            if (streamEvent != null) {
                GenericData.Record record = (GenericData.Record) streamEvent.getUnderlying();
                event.put(i, record);
            }
        }
        return eventAdapterService.adapterForTypedAvro(event, resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventTypeAvro;
    }
}
