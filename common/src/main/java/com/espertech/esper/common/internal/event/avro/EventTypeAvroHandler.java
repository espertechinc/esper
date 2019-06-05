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

public interface EventTypeAvroHandler {
    String RUNTIME_NONHA_HANDLER_IMPL = "com.espertech.esper.common.internal.avro.core.EventTypeAvroHandlerImpl";
    String COMPILE_TIME_HANDLER_IMPL = RUNTIME_NONHA_HANDLER_IMPL;

    void init(ConfigurationCommonEventTypeMeta.AvroSettings avroSettings, ClasspathImportService classpathImportService);

    SelectExprProcessorRepresentationFactory getOutputFactory();

    AvroSchemaEventType newEventTypeFromSchema(EventTypeMetadata metadata, EventBeanTypedEventFactory eventBeanTypedEventFactory, ConfigurationCommonEventTypeAvro requiredConfig, EventType[] superTypes, Set<EventType> deepSuperTypes);

    AvroSchemaEventType newEventTypeFromNormalized(EventTypeMetadata metadata, EventTypeNameResolver eventTypeNameResolver, EventBeanTypedEventFactory eventBeanTypedEventFactory, Map<String, Object> properties, Annotation[] annotations, ConfigurationCommonEventTypeAvro optionalConfig, EventType[] superTypes, Set<EventType> deepSuperTypes, String statementName);

    AvroSchemaEventType newEventTypeFromJson(EventTypeMetadata metadata, EventBeanTypedEventFactory eventBeanTypedEventFactory, String schemaJson, EventType[] supertypes, Set<EventType> deepSupertypes);

    EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType);

    EventBeanManufacturerForge getEventBeanManufacturer(AvroSchemaEventType avroSchemaEventType, WriteablePropertyDescriptor[] properties);

    EventBeanFactory getEventBeanFactory(EventType type, EventBeanTypedEventFactory eventBeanTypedEventFactory);

    void validateExistingType(EventType existingType, AvroSchemaEventType proposedType);

    void avroCompat(EventType existingType, Map<String, Object> selPropertyTypes) throws ExprValidationException;

    Object convertEvent(EventBean theEvent, AvroSchemaEventType targetType);

    TypeWidenerCustomizer getTypeWidenerCustomizer(EventType eventType);
}
