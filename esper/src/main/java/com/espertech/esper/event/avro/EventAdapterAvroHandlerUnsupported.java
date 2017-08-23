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
package com.espertech.esper.event.avro;

import com.espertech.esper.client.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessorRepresentationFactory;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.util.TypeWidenerCustomizer;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

public class EventAdapterAvroHandlerUnsupported implements EventAdapterAvroHandler {
    public final static EventAdapterAvroHandlerUnsupported INSTANCE = new EventAdapterAvroHandlerUnsupported();

    public EventAdapterAvroHandlerUnsupported() {
    }

    public void init(ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings, EngineImportService engineImportService) {
        // no action, init is always done
    }

    public AvroSchemaEventType newEventTypeFromSchema(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterService eventAdapterService, ConfigurationEventTypeAvro requiredConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        throw getUnsupported();
    }

    public EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType) {
        throw getUnsupported();
    }

    public AvroSchemaEventType newEventTypeFromNormalized(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterService eventAdapterService, Map<String, Object> properties, Annotation[] annotations, ConfigurationEventTypeAvro optionalConfig, EventType[] superTypes, Set<EventType> deepSuperTypes, String statementName, String engineURI) {
        throw getUnsupported();
    }

    public EventBeanManufacturer getEventBeanManufacturer(AvroSchemaEventType avroSchemaEventType, EventAdapterService eventAdapterService, WriteablePropertyDescriptor[] properties) {
        throw getUnsupported();
    }

    public EventBeanFactory getEventBeanFactory(EventType type, EventAdapterService eventAdapterService) {
        throw getUnsupported();
    }

    public void validateExistingType(EventType existingType, AvroSchemaEventType proposedType) {
        throw getUnsupported();
    }

    public SelectExprProcessorRepresentationFactory getOutputFactory() {
        throw getUnsupported();
    }

    public void avroCompat(EventType existingType, Map<String, Object> selPropertyTypes) throws ExprValidationException {
        throw getUnsupported();
    }

    public Object convertEvent(EventBean theEvent, AvroSchemaEventType targetType) {
        throw getUnsupported();
    }

    public TypeWidenerCustomizer getTypeWidenerCustomizer(EventType eventType) {
        throw getUnsupported();
    }

    private UnsupportedOperationException getUnsupported() {
        throw new UnsupportedOperationException("Esper-Avro is not part of your classpath");
    }
}
