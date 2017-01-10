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

package com.espertech.esper.avro.core;

import com.espertech.esper.avro.selectexprrep.SelectExprProcessorRepresentationFactoryAvro;
import com.espertech.esper.client.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.*;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.core.SelectExprProcessorRepresentationFactory;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.apache.avro.SchemaBuilder.record;

public class EventAdapterAvroHandlerImpl implements EventAdapterAvroHandler {
    private final static SelectExprProcessorRepresentationFactoryAvro factory = new SelectExprProcessorRepresentationFactoryAvro();

    public AvroSchemaEventType newEventTypeFromSchema(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, ConfigurationEventTypeAvro requiredConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        Object avroSchemaObj = requiredConfig.getAvroSchema();
        String avroSchemaText = requiredConfig.getAvroSchemaText();

        if (avroSchemaObj == null && avroSchemaText == null) {
            throw new IllegalArgumentException("Null value for schema and schema text");
        }
        if (avroSchemaObj != null && avroSchemaText != null) {
            throw new IllegalArgumentException("Both avro schema and avro schema text are supplied and one can be provided");
        }
        if (avroSchemaObj != null && !(avroSchemaObj instanceof Schema)) {
            throw new IllegalArgumentException("Schema expected of type " + Schema.class.getName() + " but received " + avroSchemaObj.getClass().getName());
        }

        Schema schema;
        if (avroSchemaObj != null) {
            schema = (Schema) avroSchemaObj;
        }
        else {
            try {
                schema = new Schema.Parser().parse(avroSchemaText);
            }
            catch (Throwable t) {
                throw new EPException("Failed for parse avro schema: " + t.getMessage(), t);
            }
        }

        return makeType(metadata, eventTypeName, typeId, eventAdapterService, schema, requiredConfig, supertypes, deepSupertypes);
    }

    public AvroSchemaEventType newEventTypeFromNormalized(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, Map<String, Object> properties, Annotation[] annotations, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings, ConfigurationEventTypeAvro optionalConfig, EventType[] optionalSuperTypes, Set<EventType> deepSuperTypes) {
        SchemaBuilder.FieldAssembler<Schema> assembler = record(eventTypeName).fields();

        // add supertypes first so the positions are comparable
        Set<String> added = new HashSet<>();
        if (optionalSuperTypes != null) {
            for (int i = 0; i < optionalSuperTypes.length; i++) {
                AvroEventType superType = (AvroEventType) optionalSuperTypes[i];
                for (Schema.Field field : superType.getSchemaAvro().getFields()) {
                    if (properties.containsKey(field.name()) || added.contains(field.name())) {
                        continue;
                    }
                    added.add(field.name());
                    assembler.name(field.name()).type(field.schema()).noDefault();
                }
            }
        }

        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (!added.contains(prop.getKey())) {
                AvroSchemaUtil.assembleField(prop.getKey(), prop.getValue(), assembler, annotations, avroSettings, eventAdapterService);
                added.add(prop.getKey());
            }
        }

        Schema schema = assembler.endRecord();
        return makeType(metadata, eventTypeName, typeId, eventAdapterService, schema, optionalConfig, optionalSuperTypes, deepSuperTypes);
    }

    public EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType) {
        if (!(avroGenericDataDotRecord instanceof GenericData.Record)) {
            throw new EPException("Unexpected event object type '" + (avroGenericDataDotRecord == null ? "null" : avroGenericDataDotRecord.getClass().getName()) + "' encountered, please supply a GenericData.Record");
        }

        GenericData.Record record = (GenericData.Record) avroGenericDataDotRecord;
        return new AvroEventBean(record, existingType);
    }

    public SelectExprProcessorRepresentationFactory getOutputFactory() {
        return factory;
    }

    public EventBeanManufacturer getEventBeanManufacturer(AvroSchemaEventType avroSchemaEventType, EventAdapterService eventAdapterService, WriteablePropertyDescriptor[] properties) {
        return new EventBeanManufacturerAvro(avroSchemaEventType, eventAdapterService, properties);
    }

    public EventBeanFactory getEventBeanFactory(EventType type, EventAdapterService eventAdapterService) {
        return new EventBeanFactoryAvro(type, eventAdapterService);
    }

    public void validateExistingType(EventType existingType, AvroSchemaEventType proposedType) {
        if (!(existingType instanceof AvroSchemaEventType)) {
            throw new EventAdapterException("Type by name '" + proposedType.getName() + "' is not a compatible type " +
                "(target type underlying is '" + existingType.getUnderlyingType().getName() + "', " +
                "source type underlying is '" + proposedType.getUnderlyingType().getName() + "')");
        }

        Schema proposed = (Schema) proposedType.getSchema();
        Schema existing = (Schema) ((AvroSchemaEventType) existingType).getSchema();
        if (!proposed.equals(existing)) {
            throw new EventAdapterException("Event type named '" + existingType.getName() +
                    "' has already been declared with differing column name or type information\n"
                    + "schemaExisting: " + AvroSchemaUtil.toSchemaStringSafe(existing) + "\n"
                    + "schemaProposed: " + AvroSchemaUtil.toSchemaStringSafe(proposed));
        }
    }

    public ExprEvaluator[] avroCompat(EventType existingType, Map<String, Object> selPropertyTypes, ExprEvaluator[] exprEvaluators) {
        ExprEvaluator[] evals = new ExprEvaluator[exprEvaluators.length];
        int index = -1;
        for (Map.Entry<String, Object> selected : selPropertyTypes.entrySet()) {
            index++;
            evals[index] = exprEvaluators[index];

            if (selected.getValue() instanceof EventType) {
                final ExprEvaluator inner = exprEvaluators[index];
                evals[index] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                        EventBean bean = (EventBean) inner.evaluate(eventsPerStream, isNewData, context);
                        return bean.getUnderlying();
                    }

                    public Class getType() {
                        return GenericData.Record.class;
                    }
                };
            }
        }
        return evals;
    }

    public Object convertEvent(EventBean theEvent, AvroSchemaEventType targetType) {
        GenericData.Record original = ((AvroEventBean) theEvent).getProperties();
        Schema targetSchema = (Schema) targetType.getSchema();
        GenericData.Record target = new GenericData.Record(targetSchema);

        List<Schema.Field> fields = original.getSchema().getFields();
        for (Schema.Field field : fields) {

            Schema.Field targetField = targetSchema.getField(field.name());
            if (targetField == null) {
                continue;
            }

            if (field.schema().getType() == Schema.Type.ARRAY) {
                Collection originalColl = (Collection) original.get(field.pos());
                if (originalColl != null) {
                    target.put(targetField.pos(), new ArrayList<>(originalColl));
                }
            }
            else if (field.schema().getType() == Schema.Type.MAP) {
                Map originalMap = (Map) original.get(field.pos());
                if (originalMap != null) {
                    target.put(targetField.pos(), new HashMap<>(originalMap));
                }
            }
            else {
                target.put(targetField.pos(), original.get(field.pos()));
            }
        }
        return target;
    }

    private AvroSchemaEventType makeType(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, Schema schema, ConfigurationEventTypeAvro optionalConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        return new AvroEventType(metadata, eventTypeName, typeId, eventAdapterService, schema, optionalConfig == null ? null : optionalConfig.getStartTimestampPropertyName(), optionalConfig == null ? null : optionalConfig.getEndTimestampPropertyName(), supertypes, deepSupertypes);
    }
}
