/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.event.avro;

import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterServiceImpl;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.core.SelectExprProcessorRepresentationFactory;

import java.lang.annotation.Annotation;
import java.util.Map;

public interface EventAdapterAvroHandler {
    String HANDLER_IMPL = "com.espertech.esper.avro.core.EventAdapterAvroHandlerImpl";

    SelectExprProcessorRepresentationFactory getOutputFactory();
    EventType newEventTypeFromSchema(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, ConfigurationEventTypeAvro avro);
    EventType newEventTypeFromNormalized(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, Map<String, Object> properties, Annotation[] annotations);
    EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType);
}
