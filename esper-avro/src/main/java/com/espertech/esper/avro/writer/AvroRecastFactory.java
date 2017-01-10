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

package com.espertech.esper.avro.writer;

import com.espertech.esper.avro.core.AvroBackedEventBean;
import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.eval.SelectExprContext;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.*;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AvroRecastFactory {

    public static SelectExprProcessor make(EventType[] eventTypes, SelectExprContext selectExprContext, int streamNumber, AvroSchemaEventType targetType, ExprNode[] exprNodes, EngineImportService engineImportService)
            throws ExprValidationException
    {
        AvroEventType resultType = (AvroEventType) targetType;
        AvroEventType streamType = (AvroEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (resultType.getSchema().equals(streamType.getSchema()) && selectExprContext.getExpressionNodes().length == 0) {
            return new AvroInsertProcessorSimpleRepackage(selectExprContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = selectExprContext.getEventAdapterService().getWriteableProperties(resultType, true);
        List<Item> items = new ArrayList<Item>();
        List<WriteablePropertyDescriptor> written = new ArrayList<WriteablePropertyDescriptor>();

        // find the properties coming from the providing source stream
        for (WriteablePropertyDescriptor writeable : writables) {
            String propertyName = writeable.getPropertyName();

            Schema.Field streamTypeField = streamType.getSchemaAvro().getField(propertyName);
            Integer indexSource = streamTypeField == null ? null : streamTypeField.pos();
            Schema.Field resultTypeField = resultType.getSchemaAvro().getField(propertyName);
            Integer indexTarget = resultTypeField == null ? null : resultTypeField.pos();

            if (indexSource != null && indexTarget != null) {
                if (streamTypeField.schema().equals(resultTypeField.schema())) {
                    items.add(new Item(indexTarget, indexSource, null, null));
                }
                else {
                    throw new ExprValidationException("Type by name '" + resultType.getName() + "' " +
                            "in property '" + propertyName +
                            "' expected schema '" + resultTypeField.schema() +
                            "' but received schema '" + streamTypeField.schema() +
                            "'");
                }
            }
        }

        // find the properties coming from the expressions of the select clause
        for (int i = 0; i < selectExprContext.getExpressionNodes().length; i++) {
            String columnName = selectExprContext.getColumnNames()[i];
            ExprEvaluator evaluator = selectExprContext.getExpressionNodes()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + resultType.getName() + "'");
            }
            Schema.Field resultTypeField = resultType.getSchemaAvro().getField(writable.getPropertyName());

            TypeWidener widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getExprEvaluator().getType(),
                    writable.getType(), columnName, false);
            items.add(new Item(resultTypeField.pos(), -1, evaluator, widener));
            written.add(writable);
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        return new AvroInsertProcessorAllocate(streamNumber, itemsArr, resultType, resultType.getSchemaAvro(), selectExprContext.getEventAdapterService());
    }

    private static WriteablePropertyDescriptor findWritable(String columnName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(columnName)) {
                return writable;
            }
        }
        return null;
    }

    private static class AvroInsertProcessorSimpleRepackage implements SelectExprProcessor {
        private final SelectExprContext selectExprContext;
        private final int underlyingStreamNumber;
        private final EventType resultType;

        private AvroInsertProcessorSimpleRepackage(SelectExprContext selectExprContext, int underlyingStreamNumber, EventType resultType) {
            this.selectExprContext = selectExprContext;
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            AvroBackedEventBean theEvent = (AvroBackedEventBean) eventsPerStream[underlyingStreamNumber];
            return selectExprContext.getEventAdapterService().adapterForTypedAvro(theEvent.getProperties(), resultType);
        }
    }

    private static class AvroInsertProcessorAllocate implements SelectExprProcessor {
        private final int underlyingStreamNumber;
        private final Item[] items;
        private final EventType resultType;
        private final Schema resultSchema;
        private final EventAdapterService eventAdapterService;

        public AvroInsertProcessorAllocate(int underlyingStreamNumber, Item[] items, EventType resultType, Schema resultSchema, EventAdapterService eventAdapterService) {
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.items = items;
            this.resultType = resultType;
            this.resultSchema = resultSchema;
            this.eventAdapterService = eventAdapterService;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {

            GenericData.Record source = ((AvroBackedEventBean) eventsPerStream[underlyingStreamNumber]).getProperties();
            GenericData.Record target = new GenericData.Record(resultSchema);
            for (Item item : items) {
                Object value;

                if (item.getOptionalFromIndex() != -1) {
                    value = source.get(item.getOptionalFromIndex());
                }
                else {
                    value = item.getEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (item.getOptionalWidener() != null) {
                        value = item.getOptionalWidener().widen(value);
                    }
                }

                target.put(item.getToIndex(), value);
            }

            return eventAdapterService.adapterForTypedAvro(target, resultType);
        }
    }

    private static class Item {
        private final int toIndex;
        private final int optionalFromIndex;
        private final ExprEvaluator evaluator;
        private final TypeWidener optionalWidener;

        private Item(int toIndex, int optionalFromIndex, ExprEvaluator evaluator, TypeWidener optionalWidener) {
            this.toIndex = toIndex;
            this.optionalFromIndex = optionalFromIndex;
            this.evaluator = evaluator;
            this.optionalWidener = optionalWidener;
        }

        public int getToIndex() {
            return toIndex;
        }

        public int getOptionalFromIndex() {
            return optionalFromIndex;
        }

        public ExprEvaluator getEvaluator() {
            return evaluator;
        }

        public TypeWidener getOptionalWidener() {
            return optionalWidener;
        }
    }
}
