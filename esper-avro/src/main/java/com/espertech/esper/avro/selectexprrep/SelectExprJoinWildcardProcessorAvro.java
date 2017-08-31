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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorAvro implements SelectExprProcessor, SelectExprProcessorForge {
    private final EventType resultEventType;
    private final Schema schema;
    private final EventAdapterService eventAdapterService;

    public SelectExprJoinWildcardProcessorAvro(EventType resultEventType, EventAdapterService eventAdapterService) {
        this.resultEventType = resultEventType;
        this.schema = ((AvroEventType) resultEventType).getSchemaAvro();
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        return processSelectExprJoinWildcardAvro(eventsPerStream, schema, eventAdapterService, resultEventType);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember schemaMember = codegenClassScope.makeAddMember(Schema.class, schema);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock().methodReturn(staticMethod(SelectExprJoinWildcardProcessorAvro.class, "processSelectExprJoinWildcardAvro", refEPS, CodegenExpressionBuilder.member(schemaMember.getMemberId()), CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), CodegenExpressionBuilder.member(memberResultEventType.getMemberId())));
        return methodNode;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param eventsPerStream events
     * @param schema schema
     * @param eventAdapterService event svc
     * @param resultEventType
     * @return bean
     */
    public static EventBean processSelectExprJoinWildcardAvro(EventBean[] eventsPerStream, Schema schema, EventAdapterService eventAdapterService, EventType resultEventType) {
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
        return resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return this;
    }
}
