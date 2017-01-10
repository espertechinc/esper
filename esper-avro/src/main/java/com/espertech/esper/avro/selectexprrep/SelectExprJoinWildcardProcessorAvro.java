/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorAvro implements SelectExprProcessor
{
    private final EventType resultEventType;
    private final Schema schema;
    private final EventAdapterService eventAdapterService;

    public SelectExprJoinWildcardProcessorAvro(EventType resultEventType, EventAdapterService eventAdapterService) {
        this.resultEventType = resultEventType;
        this.schema = ((AvroEventType) resultEventType).getSchemaAvro();
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext)
    {
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

    public EventType getResultEventType()
    {
        return resultEventType;
    }
}
