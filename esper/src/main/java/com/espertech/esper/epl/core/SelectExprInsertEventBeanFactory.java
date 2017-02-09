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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.InsertIntoDesc;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.EventBeanManufacturerCtor;
import com.espertech.esper.event.bean.InstanceManufacturerUtil;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.TypeWidenerFactory;
import net.sf.cglib.reflect.FastConstructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectExprInsertEventBeanFactory {
    public static SelectExprProcessor getInsertUnderlyingNonJoin(EventAdapterService eventAdapterService,
                                                                 EventType eventType,
                                                                 boolean isUsingWildcard,
                                                                 StreamTypeService typeService,
                                                                 ExprEvaluator[] expressionNodes,
                                                                 String[] columnNames,
                                                                 Object[] expressionReturnTypes,
                                                                 EngineImportService engineImportService,
                                                                 InsertIntoDesc insertIntoDesc,
                                                                 String[] columnNamesAsProvided,
                                                                 boolean allowNestableTargetFragmentTypes,
                                                                 String statementName)
            throws ExprValidationException {
        // handle single-column coercion to underlying, i.e. "insert into MapDefinedEvent select doSomethingReturnMap() from MyEvent"
        if (expressionReturnTypes.length == 1 &&
                expressionReturnTypes[0] instanceof Class &&
                (eventType instanceof BaseNestableEventType || eventType instanceof AvroSchemaEventType) &&
                JavaClassHelper.isSubclassOrImplementsInterface((Class) expressionReturnTypes[0], eventType.getUnderlyingType()) &&
                insertIntoDesc.getColumnNames().isEmpty() &&
                columnNamesAsProvided[0] == null) {

            if (eventType instanceof MapEventType) {
                return new SelectExprInsertNativeExpressionCoerceMap(eventType, expressionNodes[0], eventAdapterService);
            } else if (eventType instanceof ObjectArrayEventType) {
                return new SelectExprInsertNativeExpressionCoerceObjectArray(eventType, expressionNodes[0], eventAdapterService);
            } else if (eventType instanceof AvroSchemaEventType) {
                return new SelectExprInsertNativeExpressionCoerceAvro(eventType, expressionNodes[0], eventAdapterService);
            } else {
                throw new IllegalStateException("Unrecognied event type " + eventType);
            }
        }

        // handle special case where the target type has no properties and there is a single "null" value selected
        if (eventType.getPropertyDescriptors().length == 0 &&
                columnNames.length == 1 &&
                columnNames[0].equals("null") &&
                expressionReturnTypes[0] == null &&
                !isUsingWildcard) {

            EventBeanManufacturer eventManufacturer;
            try {
                eventManufacturer = eventAdapterService.getManufacturer(eventType, new WriteablePropertyDescriptor[0], engineImportService, true);
            } catch (EventBeanManufactureException e) {
                throw new ExprValidationException(e.getMessage(), e);
            }
            return new SelectExprInsertNativeNoEval(eventType, eventManufacturer);
        }

        // handle writing to defined columns
        Set<WriteablePropertyDescriptor> writableProps = eventAdapterService.getWriteableProperties(eventType, false);
        boolean isEligible = checkEligible(eventType, writableProps, allowNestableTargetFragmentTypes);
        if (!isEligible) {
            return null;
        }

        try {
            return initializeSetterManufactor(eventType, writableProps, isUsingWildcard, typeService, expressionNodes, columnNames, expressionReturnTypes, engineImportService, eventAdapterService, statementName);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                return initializeCtorInjection((BeanEventType) eventType, expressionNodes, expressionReturnTypes, engineImportService, eventAdapterService);
            } catch (ExprValidationException ctorEx) {
                if (writableProps.isEmpty()) {
                    throw ctorEx;
                }
                throw ex;
            }
        }
    }

    public static SelectExprProcessor getInsertUnderlyingJoinWildcard(EventAdapterService eventAdapterService, EventType eventType,
                                                                      String[] streamNames, EventType[] streamTypes, EngineImportService engineImportService, String statementName, String engineURI)
            throws ExprValidationException {
        Set<WriteablePropertyDescriptor> writableProps = eventAdapterService.getWriteableProperties(eventType, false);
        boolean isEligible = checkEligible(eventType, writableProps, false);
        if (!isEligible) {
            return null;
        }

        try {
            return initializeJoinWildcardInternal(eventType, writableProps, streamNames, streamTypes, engineImportService, eventAdapterService, statementName, engineURI);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                ExprEvaluator[] evaluators = new ExprEvaluator[streamTypes.length];
                Object[] resultTypes = new Object[streamTypes.length];
                for (int i = 0; i < streamTypes.length; i++) {
                    evaluators[i] = new ExprEvaluatorJoinWildcard(i, streamTypes[i].getUnderlyingType());
                    resultTypes[i] = evaluators[i].getType();
                }

                return initializeCtorInjection((BeanEventType) eventType, evaluators, resultTypes, engineImportService, eventAdapterService);
            } catch (ExprValidationException ctorEx) {
                if (writableProps.isEmpty()) {
                    throw ctorEx;
                }
                throw ex;
            }
        }
    }

    private static boolean checkEligible(EventType eventType, Set<WriteablePropertyDescriptor> writableProps, boolean allowNestableTargetFragmentTypes) {
        if (writableProps == null) {
            return false;    // no writable properties, not a writable type, proceed
        }

        // For map event types this class does not handle fragment inserts; all fragments are required however and must be explicit
        if (!allowNestableTargetFragmentTypes && (eventType instanceof BaseNestableEventType || eventType instanceof AvroSchemaEventType)) {
            for (EventPropertyDescriptor prop : eventType.getPropertyDescriptors()) {
                if (prop.isFragment()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static SelectExprProcessor initializeSetterManufactor(EventType eventType, Set<WriteablePropertyDescriptor> writables, boolean isUsingWildcard, StreamTypeService typeService, ExprEvaluator[] expressionNodes, String[] columnNames, Object[] expressionReturnTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService, String statementName)
            throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprEvaluator> evaluatorsList = new ArrayList<ExprEvaluator>();
        List<TypeWidener> widenersList = new ArrayList<TypeWidener>();

        // loop over all columns selected, if any
        for (int i = 0; i < columnNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidener widener = null;
            ExprEvaluator evaluator = expressionNodes[i];

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(columnNames[i])) {
                    continue;
                }

                Object columnType = expressionReturnTypes[i];
                if (columnType == null) {
                    TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], null, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                } else if (columnType instanceof EventType) {
                    EventType columnEventType = (EventType) columnType;
                    final Class returnType = columnEventType.getUnderlyingType();
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], columnEventType.getUnderlyingType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());

                    // handle evaluator returning an event
                    if (JavaClassHelper.isSubclassOrImplementsInterface(returnType, desc.getType())) {
                        selectedWritable = desc;
                        widener = new TypeWidener() {
                            public Object widen(Object input) {
                                if (input instanceof EventBean) {
                                    return ((EventBean) input).getUnderlying();
                                }
                                return input;
                            }
                        };
                        continue;
                    }

                    // find stream
                    int streamNum = 0;
                    for (int j = 0; j < typeService.getEventTypes().length; j++) {
                        if (typeService.getEventTypes()[j] == columnEventType) {
                            streamNum = j;
                            break;
                        }
                    }
                    final int streamNumEval = streamNum;
                    evaluator = new ExprEvaluator() {
                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                            EventBean theEvent = eventsPerStream[streamNumEval];
                            if (theEvent != null) {
                                return theEvent.getUnderlying();
                            }
                            return null;
                        }

                        public Class getType() {
                            return returnType;
                        }

                    };
                } else if (columnType instanceof EventType[]) {
                    // handle case where the select-clause contains an fragment array
                    EventType columnEventType = ((EventType[]) columnType)[0];
                    final Class componentReturnType = columnEventType.getUnderlyingType();
                    final Class arrayReturnType = Array.newInstance(componentReturnType, 0).getClass();

                    boolean allowObjectArrayToCollectionConversion = eventType instanceof AvroSchemaEventType;
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], arrayReturnType, desc.getType(), desc.getPropertyName(), allowObjectArrayToCollectionConversion, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    final ExprEvaluator inner = evaluator;
                    evaluator = new ExprEvaluator() {
                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                            Object result = inner.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                            if (!(result instanceof EventBean[])) {
                                return null;
                            }
                            EventBean[] events = (EventBean[]) result;
                            Object values = Array.newInstance(componentReturnType, events.length);
                            for (int i = 0; i < events.length; i++) {
                                Array.set(values, i, events[i].getUnderlying());
                            }
                            return values;
                        }

                        public Class getType() {
                            return componentReturnType;
                        }

                    };
                } else if (!(columnType instanceof Class)) {
                    String message = "Invalid assignment of column '" + columnNames[i] +
                            "' of type '" + columnType +
                            "' to event property '" + desc.getPropertyName() +
                            "' typed as '" + desc.getType().getName() +
                            "', column and parameter types mismatch";
                    throw new ExprValidationException(message);
                } else {
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], (Class) columnType, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                }

                selectedWritable = desc;
                break;
            }

            if (selectedWritable == null) {
                String message = "Column '" + columnNames[i] +
                        "' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?)";
                throw new ExprValidationException(message);
            }

            // add
            writablePropertiesList.add(selectedWritable);
            evaluatorsList.add(evaluator);
            widenersList.add(widener);
        }

        // handle wildcard
        if (isUsingWildcard) {
            EventType sourceType = typeService.getEventTypes()[0];
            for (EventPropertyDescriptor eventPropDescriptor : sourceType.getPropertyDescriptors()) {
                if (eventPropDescriptor.isRequiresIndex() || (eventPropDescriptor.isRequiresMapkey())) {
                    continue;
                }

                WriteablePropertyDescriptor selectedWritable = null;
                TypeWidener widener = null;
                ExprEvaluator evaluator = null;

                for (WriteablePropertyDescriptor writableDesc : writables) {
                    if (!writableDesc.getPropertyName().equals(eventPropDescriptor.getPropertyName())) {
                        continue;
                    }

                    widener = TypeWidenerFactory.getCheckPropertyAssignType(eventPropDescriptor.getPropertyName(), eventPropDescriptor.getPropertyType(), writableDesc.getType(), writableDesc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    selectedWritable = writableDesc;

                    final String propertyName = eventPropDescriptor.getPropertyName();
                    final Class propertyType = eventPropDescriptor.getPropertyType();
                    evaluator = new ExprEvaluator() {

                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                            EventBean theEvent = eventsPerStream[0];
                            if (theEvent != null) {
                                return theEvent.get(propertyName);
                            }
                            return null;
                        }

                        public Class getType() {
                            return propertyType;
                        }
                    };
                    break;
                }

                if (selectedWritable == null) {
                    String message = "Event property '" + eventPropDescriptor.getPropertyName() +
                            "' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?)";
                    throw new ExprValidationException(message);
                }

                writablePropertiesList.add(selectedWritable);
                evaluatorsList.add(evaluator);
                widenersList.add(widener);
            }
        }

        // assign
        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        ExprEvaluator[] exprEvaluators = evaluatorsList.toArray(new ExprEvaluator[evaluatorsList.size()]);
        TypeWidener[] wideners = widenersList.toArray(new TypeWidener[widenersList.size()]);

        EventBeanManufacturer eventManufacturer;
        try {
            eventManufacturer = eventAdapterService.getManufacturer(eventType, writableProperties, engineImportService, false);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprEvaluators, wideners);
    }

    private static SelectExprProcessor initializeCtorInjection(BeanEventType beanEventType, ExprEvaluator[] exprEvaluators, Object[] expressionReturnTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService)
            throws ExprValidationException {

        Pair<FastConstructor, ExprEvaluator[]> pair = InstanceManufacturerUtil.getManufacturer(beanEventType.getUnderlyingType(), engineImportService, exprEvaluators, expressionReturnTypes);
        EventBeanManufacturerCtor eventManufacturer = new EventBeanManufacturerCtor(pair.getFirst(), beanEventType, eventAdapterService);
        return new SelectExprInsertNativeNoWiden(beanEventType, eventManufacturer, pair.getSecond());
    }

    private static SelectExprProcessor initializeJoinWildcardInternal(EventType eventType, Set<WriteablePropertyDescriptor> writables, String[] streamNames, EventType[] streamTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService, String statementName, String engineURI)
            throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprEvaluator> evaluatorsList = new ArrayList<ExprEvaluator>();
        List<TypeWidener> widenersList = new ArrayList<TypeWidener>();

        // loop over all columns selected, if any
        for (int i = 0; i < streamNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidener widener = null;

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(streamNames[i])) {
                    continue;
                }

                widener = TypeWidenerFactory.getCheckPropertyAssignType(streamNames[i], streamTypes[i].getUnderlyingType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, engineURI);
                selectedWritable = desc;
                break;
            }

            if (selectedWritable == null) {
                String message = "Stream underlying object for stream '" + streamNames[i] +
                        "' could not be assigned to any of the properties of the underlying type (missing column names, event property or setter method?)";
                throw new ExprValidationException(message);
            }

            final int streamNum = i;
            final Class returnType = streamTypes[streamNum].getUnderlyingType();
            ExprEvaluator evaluator = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    EventBean theEvent = eventsPerStream[streamNum];
                    if (theEvent != null) {
                        return theEvent.getUnderlying();
                    }
                    return null;
                }

                public Class getType() {
                    return returnType;
                }

            };

            // add
            writablePropertiesList.add(selectedWritable);
            evaluatorsList.add(evaluator);
            widenersList.add(widener);
        }

        // assign
        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        ExprEvaluator[] exprEvaluators = evaluatorsList.toArray(new ExprEvaluator[evaluatorsList.size()]);
        TypeWidener[] wideners = widenersList.toArray(new TypeWidener[widenersList.size()]);

        EventBeanManufacturer eventManufacturer;
        try {
            eventManufacturer = eventAdapterService.getManufacturer(eventType, writableProperties, engineImportService, false);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprEvaluators, wideners);
    }

    public abstract static class SelectExprInsertNativeExpressionCoerceBase implements SelectExprProcessor {

        protected final EventType eventType;
        protected final ExprEvaluator exprEvaluator;
        protected final EventAdapterService eventAdapterService;

        protected SelectExprInsertNativeExpressionCoerceBase(EventType eventType, ExprEvaluator exprEvaluator, EventAdapterService eventAdapterService) {
            this.eventType = eventType;
            this.exprEvaluator = exprEvaluator;
            this.eventAdapterService = eventAdapterService;
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceMap extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceMap(EventType eventType, ExprEvaluator exprEvaluator, EventAdapterService eventAdapterService) {
            super(eventType, exprEvaluator, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = exprEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedMap((Map) result, eventType);
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceAvro extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceAvro(EventType eventType, ExprEvaluator exprEvaluator, EventAdapterService eventAdapterService) {
            super(eventType, exprEvaluator, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = exprEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedAvro(result, eventType);
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceObjectArray extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceObjectArray(EventType eventType, ExprEvaluator exprEvaluator, EventAdapterService eventAdapterService) {
            super(eventType, exprEvaluator, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = exprEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedObjectArray((Object[]) result, eventType);
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceNative extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceNative(EventType eventType, ExprEvaluator exprEvaluator, EventAdapterService eventAdapterService) {
            super(eventType, exprEvaluator, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = exprEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedBean(result, eventType);
        }
    }

    public abstract static class SelectExprInsertNativeBase implements SelectExprProcessor {

        private final EventType eventType;
        protected final EventBeanManufacturer eventManufacturer;
        protected final ExprEvaluator[] exprEvaluators;

        protected SelectExprInsertNativeBase(EventType eventType, EventBeanManufacturer eventManufacturer, ExprEvaluator[] exprEvaluators) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
            this.exprEvaluators = exprEvaluators;
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class SelectExprInsertNativeWidening extends SelectExprInsertNativeBase {

        private final TypeWidener[] wideners;

        public SelectExprInsertNativeWidening(EventType eventType, EventBeanManufacturer eventManufacturer, ExprEvaluator[] exprEvaluators, TypeWidener[] wideners) {
            super(eventType, eventManufacturer, exprEvaluators);
            this.wideners = wideners;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object[] values = new Object[exprEvaluators.length];

            for (int i = 0; i < exprEvaluators.length; i++) {
                Object evalResult = exprEvaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if ((evalResult != null) && (wideners[i] != null)) {
                    evalResult = wideners[i].widen(evalResult);
                }
                values[i] = evalResult;
            }

            return eventManufacturer.make(values);
        }
    }

    public static class SelectExprInsertNativeNoWiden extends SelectExprInsertNativeBase {

        public SelectExprInsertNativeNoWiden(EventType eventType, EventBeanManufacturer eventManufacturer, ExprEvaluator[] exprEvaluators) {
            super(eventType, eventManufacturer, exprEvaluators);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object[] values = new Object[exprEvaluators.length];

            for (int i = 0; i < exprEvaluators.length; i++) {
                Object evalResult = exprEvaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                values[i] = evalResult;
            }

            return eventManufacturer.make(values);
        }
    }

    public static class SelectExprInsertNativeNoEval implements SelectExprProcessor {
        private final static Object[] EMPTY_PROPS = new Object[0];

        private final EventType eventType;
        private final EventBeanManufacturer eventManufacturer;

        public SelectExprInsertNativeNoEval(EventType eventType, EventBeanManufacturer eventManufacturer) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            return eventManufacturer.make(EMPTY_PROPS);
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class ExprEvaluatorJoinWildcard implements ExprEvaluator {
        private final int streamNum;
        private final Class returnType;

        public ExprEvaluatorJoinWildcard(int streamNum, Class returnType) {
            this.streamNum = streamNum;
            this.returnType = returnType;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            EventBean bean = eventsPerStream[streamNum];
            if (bean == null) {
                return null;
            }
            return bean.getUnderlying();
        }

        public Class getType() {
            return returnType;
        }

    }
}
