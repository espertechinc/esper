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
package com.espertech.esper.common.internal.event.avro;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventBeanFactory;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMeta;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorRepresentationFactory;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

public class EventTypeAvroHandlerUnsupported implements EventTypeAvroHandler {
    public final static EventTypeAvroHandlerUnsupported INSTANCE = new EventTypeAvroHandlerUnsupported();

    public EventTypeAvroHandlerUnsupported() {
    }

    public void init(ConfigurationCommonEventTypeMeta.AvroSettings avroSettings, ClasspathImportService classpathImportService) {
        // no action, init is always done
    }

    public AvroSchemaEventType newEventTypeFromSchema(EventTypeMetadata metadata, EventBeanTypedEventFactory eventBeanTypedEventFactory, ConfigurationCommonEventTypeAvro requiredConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        throw getUnsupported();
    }

    public EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType) {
        throw getUnsupported();
    }

    public AvroSchemaEventType newEventTypeFromNormalized(EventTypeMetadata metadata, EventTypeNameResolver eventTypeNameResolver, EventBeanTypedEventFactory eventBeanTypedEventFactory, Map<String, Object> properties, Annotation[] annotations, ConfigurationCommonEventTypeAvro optionalConfig, EventType[] superTypes, Set<EventType> deepSuperTypes, String statementName) {
        throw getUnsupported();
    }

    public EventBeanManufacturerForge getEventBeanManufacturer(AvroSchemaEventType avroSchemaEventType, WriteablePropertyDescriptor[] properties) {
        throw getUnsupported();
    }

    public EventBeanFactory getEventBeanFactory(EventType type, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
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

    public AvroSchemaEventType newEventTypeFromJson(EventTypeMetadata metadata, EventBeanTypedEventFactory eventBeanTypedEventFactory, String schemaJson, EventType[] supertypes, Set<EventType> deepSupertypes) {
        throw getUnsupported();
    }

    public Object convertEvent(EventBean theEvent, AvroSchemaEventType targetType) {
        throw getUnsupported();
    }

    public TypeWidenerCustomizer getTypeWidenerCustomizer(EventType eventType) {
        return null;
    }

    private UnsupportedOperationException getUnsupported() {
        throw new UnsupportedOperationException("Esper-Avro is not enabled in the configuration or is not part of your classpath");
    }
}
