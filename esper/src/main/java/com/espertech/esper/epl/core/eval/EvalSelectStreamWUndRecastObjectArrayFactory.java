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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EvalSelectStreamWUndRecastObjectArrayFactory {

    public static SelectExprProcessor make(EventType[] eventTypes, SelectExprContext selectExprContext, int streamNumber, EventType targetType, ExprNode[] exprNodes, EngineImportService engineImportService, String statementName, String engineURI)
            throws ExprValidationException {
        ObjectArrayEventType oaResultType = (ObjectArrayEventType) targetType;
        ObjectArrayEventType oaStreamType = (ObjectArrayEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (oaResultType.isDeepEqualsConsiderOrder(oaStreamType) && selectExprContext.getExpressionNodes().length == 0) {
            return new OAInsertProcessorSimpleRepackage(selectExprContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = selectExprContext.getEventAdapterService().getWriteableProperties(oaResultType, true);
        List<Item> items = new ArrayList<Item>();
        List<WriteablePropertyDescriptor> written = new ArrayList<WriteablePropertyDescriptor>();

        // find the properties coming from the providing source stream
        for (WriteablePropertyDescriptor writeable : writables) {
            String propertyName = writeable.getPropertyName();

            Integer indexSource = oaStreamType.getPropertiesIndexes().get(propertyName);
            Integer indexTarget = oaResultType.getPropertiesIndexes().get(propertyName);

            if (indexSource != null) {
                Object setOneType = oaStreamType.getTypes().get(propertyName);
                Object setTwoType = oaResultType.getTypes().get(propertyName);
                boolean setTwoTypeFound = oaResultType.getTypes().containsKey(propertyName);
                String message = BaseNestableEventUtil.comparePropType(propertyName, setOneType, setTwoType, setTwoTypeFound, oaResultType.getName());
                if (message != null) {
                    throw new ExprValidationException(message);
                }
                items.add(new Item(indexTarget, indexSource, null, null));
                written.add(writeable);
            }
        }

        // find the properties coming from the expressions of the select clause
        int count = written.size();
        for (int i = 0; i < selectExprContext.getExpressionNodes().length; i++) {
            String columnName = selectExprContext.getColumnNames()[i];
            ExprEvaluator evaluator = selectExprContext.getExpressionNodes()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + oaResultType.getName() + "'");
            }

            TypeWidener widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getExprEvaluator().getType(),
                    writable.getType(), columnName, false, null, statementName, engineURI);
            items.add(new Item(count, -1, evaluator, widener));
            written.add(writable);
            count++;
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        EventBeanManufacturer manufacturer;
        try {
            manufacturer = selectExprContext.getEventAdapterService().getManufacturer(oaResultType,
                    written.toArray(new WriteablePropertyDescriptor[written.size()]), engineImportService, true);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException("Failed to write to type: " + e.getMessage(), e);
        }

        return new OAInsertProcessorAllocate(streamNumber, itemsArr, manufacturer, targetType);
    }

    private static WriteablePropertyDescriptor findWritable(String columnName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(columnName)) {
                return writable;
            }
        }
        return null;
    }

    private static class OAInsertProcessorSimpleRepackage implements SelectExprProcessor {
        private final SelectExprContext selectExprContext;
        private final int underlyingStreamNumber;
        private final EventType resultType;

        private OAInsertProcessorSimpleRepackage(SelectExprContext selectExprContext, int underlyingStreamNumber, EventType resultType) {
            this.selectExprContext = selectExprContext;
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            ObjectArrayBackedEventBean theEvent = (ObjectArrayBackedEventBean) eventsPerStream[underlyingStreamNumber];
            return selectExprContext.getEventAdapterService().adapterForTypedObjectArray(theEvent.getProperties(), resultType);
        }
    }

    private static class OAInsertProcessorAllocate implements SelectExprProcessor {
        private final int underlyingStreamNumber;
        private final Item[] items;
        private final EventBeanManufacturer manufacturer;
        private final EventType resultType;

        private OAInsertProcessorAllocate(int underlyingStreamNumber, Item[] items, EventBeanManufacturer manufacturer, EventType resultType) {
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.items = items;
            this.manufacturer = manufacturer;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {

            ObjectArrayBackedEventBean theEvent = (ObjectArrayBackedEventBean) eventsPerStream[underlyingStreamNumber];

            Object[] props = new Object[items.length];
            for (Item item : items) {
                Object value;

                if (item.getOptionalFromIndex() != -1) {
                    value = theEvent.getProperties()[item.getOptionalFromIndex()];
                } else {
                    value = item.getEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (item.getOptionalWidener() != null) {
                        value = item.getOptionalWidener().widen(value);
                    }
                }

                props[item.getToIndex()] = value;
            }

            return manufacturer.make(props);
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
