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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
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

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenMember schemaMember = context.makeAddMember(Schema.class, schema);
        return staticMethod(SelectExprJoinWildcardProcessorAvro.class, "processSelectExprJoinWildcardAvro", params.passEPS(), CodegenExpressionBuilder.member(schemaMember.getMemberId()), CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
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
