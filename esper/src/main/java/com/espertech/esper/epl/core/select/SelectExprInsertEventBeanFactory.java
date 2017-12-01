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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.InsertIntoDesc;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.EventBeanManufacturerCtor;
import com.espertech.esper.event.bean.InstanceManufacturerUtil;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.*;
import net.sf.cglib.reflect.FastConstructor;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprInsertEventBeanFactory {
    public static SelectExprProcessorForge getInsertUnderlyingNonJoin(EventAdapterService eventAdapterService,
                                                                      EventType eventType,
                                                                      boolean isUsingWildcard,
                                                                      StreamTypeService typeService,
                                                                      ExprForge[] forges,
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
                return new SelectExprInsertNativeExpressionCoerceMap(eventType, forges[0], eventAdapterService);
            } else if (eventType instanceof ObjectArrayEventType) {
                return new SelectExprInsertNativeExpressionCoerceObjectArray(eventType, forges[0], eventAdapterService);
            } else if (eventType instanceof AvroSchemaEventType) {
                return new SelectExprInsertNativeExpressionCoerceAvro(eventType, forges[0], eventAdapterService);
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
            return initializeSetterManufactor(eventType, writableProps, isUsingWildcard, typeService, forges, columnNames, expressionReturnTypes, engineImportService, eventAdapterService, statementName);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                return initializeCtorInjection((BeanEventType) eventType, forges, expressionReturnTypes, engineImportService, eventAdapterService, statementName);
            } catch (ExprValidationException ctorEx) {
                if (writableProps.isEmpty()) {
                    throw ctorEx;
                }
                throw ex;
            }
        }
    }

    public static SelectExprProcessorForge getInsertUnderlyingJoinWildcard(EventAdapterService eventAdapterService, EventType eventType,
                                                                           String[] streamNames, EventType[] streamTypes, EngineImportService engineImportService, String statementName, String engineURI, boolean isFireAndForget)
            throws ExprValidationException {
        Set<WriteablePropertyDescriptor> writableProps = eventAdapterService.getWriteableProperties(eventType, false);
        boolean isEligible = checkEligible(eventType, writableProps, false);
        if (!isEligible) {
            return null;
        }

        try {
            return initializeJoinWildcardInternal(eventType, writableProps, streamNames, streamTypes, engineImportService, eventAdapterService, statementName, engineURI, isFireAndForget);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                ExprForge[] forges = new ExprForge[streamTypes.length];
                Object[] resultTypes = new Object[streamTypes.length];
                for (int i = 0; i < streamTypes.length; i++) {
                    forges[i] = new ExprForgeJoinWildcard(i, streamTypes[i].getUnderlyingType());
                    resultTypes[i] = forges[i].getEvaluationType();
                }
                return initializeCtorInjection((BeanEventType) eventType, forges, resultTypes, engineImportService, eventAdapterService, statementName);
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

    private static SelectExprProcessorForge initializeSetterManufactor(EventType eventType, Set<WriteablePropertyDescriptor> writables, boolean isUsingWildcard, StreamTypeService typeService, ExprForge[] expressionForges, String[] columnNames, Object[] expressionReturnTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService, String statementName)
            throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprForge> forgesList = new ArrayList<>();
        List<TypeWidener> widenersList = new ArrayList<TypeWidener>();

        // loop over all columns selected, if any
        for (int i = 0; i < columnNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidener widener = null;
            ExprForge forge = expressionForges[i];

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(columnNames[i])) {
                    continue;
                }

                Object columnType = expressionReturnTypes[i];
                if (columnType == null) {
                    try {
                        TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], null, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }
                } else if (columnType instanceof EventType) {
                    EventType columnEventType = (EventType) columnType;
                    final Class returnType = columnEventType.getUnderlyingType();
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], columnEventType.getUnderlyingType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }

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

                            public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
                                CodegenMethodNode method = codegenMethodScope.makeChild(Object.class, TypeWidener.class, codegenClassScope).addParam(Object.class, "input").getBlock()
                                        .ifCondition(instanceOf(ref("input"), EventBean.class))
                                        .blockReturn(exprDotMethod(cast(EventBean.class, ref("input")), "getUnderlying"))
                                        .methodReturn(ref("input"));
                                return localMethodBuild(method).pass(expression).call();
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
                    forge = new ExprForgeStreamUnderlying(streamNum, typeService.getEventTypes()[streamNum].getUnderlyingType());
                } else if (columnType instanceof EventType[]) {
                    // handle case where the select-clause contains an fragment array
                    EventType columnEventType = ((EventType[]) columnType)[0];
                    final Class componentReturnType = columnEventType.getUnderlyingType();
                    final Class arrayReturnType = Array.newInstance(componentReturnType, 0).getClass();

                    boolean allowObjectArrayToCollectionConversion = eventType instanceof AvroSchemaEventType;
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], arrayReturnType, desc.getType(), desc.getPropertyName(), allowObjectArrayToCollectionConversion, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }

                    final ExprForge inner = forge;
                    forge = new ExprForgeStreamWithInner(inner, componentReturnType);
                } else if (!(columnType instanceof Class)) {
                    String message = "Invalid assignment of column '" + columnNames[i] +
                            "' of type '" + columnType +
                            "' to event property '" + desc.getPropertyName() +
                            "' typed as '" + desc.getType().getName() +
                            "', column and parameter types mismatch";
                    throw new ExprValidationException(message);
                } else {
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], (Class) columnType, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }
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
            forgesList.add(forge);
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
                ExprForge forge = null;

                for (WriteablePropertyDescriptor writableDesc : writables) {
                    if (!writableDesc.getPropertyName().equals(eventPropDescriptor.getPropertyName())) {
                        continue;
                    }

                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(eventPropDescriptor.getPropertyName(), eventPropDescriptor.getPropertyType(), writableDesc.getType(), writableDesc.getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }
                    selectedWritable = writableDesc;

                    final String propertyName = eventPropDescriptor.getPropertyName();
                    EventPropertyGetterSPI getter = ((EventTypeSPI) sourceType).getGetterSPI(propertyName);
                    forge = new ExprForgeStreamWithGetter(getter);
                    break;
                }

                if (selectedWritable == null) {
                    String message = "Event property '" + eventPropDescriptor.getPropertyName() +
                            "' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?)";
                    throw new ExprValidationException(message);
                }

                writablePropertiesList.add(selectedWritable);
                forgesList.add(forge);
                widenersList.add(widener);
            }
        }

        // assign
        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        ExprForge[] exprForges = forgesList.toArray(new ExprForge[forgesList.size()]);
        ExprEvaluator[] exprEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(exprForges, engineImportService, SelectExprInsertEventBeanFactory.class, typeService.isOnDemandStreams(), statementName);
        TypeWidener[] wideners = widenersList.toArray(new TypeWidener[widenersList.size()]);

        EventBeanManufacturer eventManufacturer;
        try {
            eventManufacturer = eventAdapterService.getManufacturer(eventType, writableProperties, engineImportService, false);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprForges, exprEvaluators, wideners);
    }

    private static SelectExprProcessorForge initializeCtorInjection(BeanEventType beanEventType, ExprForge[] forges, Object[] expressionReturnTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService, String statementName)
            throws ExprValidationException {

        Pair<FastConstructor, ExprForge[]> pair = InstanceManufacturerUtil.getManufacturer(beanEventType.getUnderlyingType(), engineImportService, forges, expressionReturnTypes);
        EventBeanManufacturerCtor eventManufacturer = new EventBeanManufacturerCtor(pair.getFirst(), beanEventType, eventAdapterService);
        ExprEvaluator[] evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(pair.getSecond(), engineImportService, SelectExprInsertEventBeanFactory.class, false, statementName);
        return new SelectExprInsertNativeNoWiden(beanEventType, eventManufacturer, pair.getSecond(), evaluators);
    }

    private static SelectExprProcessorForge initializeJoinWildcardInternal(EventType eventType, Set<WriteablePropertyDescriptor> writables, String[] streamNames, EventType[] streamTypes, EngineImportService engineImportService, EventAdapterService eventAdapterService, String statementName, String engineURI, boolean isFireAndForget)
            throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprForge> forgesList = new ArrayList<>();
        List<TypeWidener> widenersList = new ArrayList<TypeWidener>();

        // loop over all columns selected, if any
        for (int i = 0; i < streamNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidener widener = null;

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(streamNames[i])) {
                    continue;
                }

                try {
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(streamNames[i], streamTypes[i].getUnderlyingType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName, engineURI);
                } catch (TypeWidenerException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
                selectedWritable = desc;
                break;
            }

            if (selectedWritable == null) {
                String message = "Stream underlying object for stream '" + streamNames[i] +
                        "' could not be assigned to any of the properties of the underlying type (missing column names, event property or setter method?)";
                throw new ExprValidationException(message);
            }

            ExprForge forge = new ExprForgeStreamUnderlying(i, streamTypes[i].getUnderlyingType());

            // add
            writablePropertiesList.add(selectedWritable);
            forgesList.add(forge);
            widenersList.add(widener);
        }

        // assign
        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        ExprForge[] exprForges = forgesList.toArray(new ExprForge[forgesList.size()]);
        ExprEvaluator[] exprEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(exprForges, engineImportService, SelectExprInsertEventBeanFactory.class, isFireAndForget, statementName);
        TypeWidener[] wideners = widenersList.toArray(new TypeWidener[widenersList.size()]);

        EventBeanManufacturer eventManufacturer;
        try {
            eventManufacturer = eventAdapterService.getManufacturer(eventType, writableProperties, engineImportService, false);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprForges, exprEvaluators, wideners);
    }

    public abstract static class SelectExprInsertNativeExpressionCoerceBase implements SelectExprProcessor, SelectExprProcessorForge {

        protected final EventType eventType;
        protected final ExprForge exprForge;
        protected final EventAdapterService eventAdapterService;
        protected ExprEvaluator evaluator;

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            if (evaluator == null) {
                evaluator = ExprNodeCompiler.allocateEvaluator(exprForge, engineImportService, this.getClass(), isFireAndForget, statementName);
            }
            return this;
        }

        protected SelectExprInsertNativeExpressionCoerceBase(EventType eventType, ExprForge exprForge, EventAdapterService eventAdapterService) {
            this.eventType = eventType;
            this.exprForge = exprForge;
            this.eventAdapterService = eventAdapterService;
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceMap extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceMap(EventType eventType, ExprForge exprForge, EventAdapterService eventAdapterService) {
            super(eventType, exprForge, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedMap((Map) result, eventType);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpression expr = exprForge.evaluateCodegen(Map.class, methodNode, exprSymbol, codegenClassScope);
            if (!JavaClassHelper.isSubclassOrImplementsInterface(exprForge.getEvaluationType(), Map.class)) {
                expr = cast(Map.class, expr);
            }
            methodNode.getBlock().declareVar(Map.class, "result", expr)
                    .ifRefNullReturnNull("result")
                    .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedMap", ref("result"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceAvro extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceAvro(EventType eventType, ExprForge exprForge, EventAdapterService eventAdapterService) {
            super(eventType, exprForge, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedAvro(result, eventType);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                    .declareVar(Object.class, "result", exprForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("result")
                    .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedAvro", ref("result"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceObjectArray extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceObjectArray(EventType eventType, ExprForge exprForge, EventAdapterService eventAdapterService) {
            super(eventType, exprForge, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedObjectArray((Object[]) result, eventType);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                    .declareVar(Object[].class, "result", exprForge.evaluateCodegen(Object[].class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("result")
                    .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", ref("result"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceNative extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceNative(EventType eventType, ExprForge exprForge, EventAdapterService eventAdapterService) {
            super(eventType, exprForge, eventAdapterService);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (result == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedBean(result, eventType);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                    .declareVar(Object.class, "result", exprForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("result")
                    .methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedBean", ref("result"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    public abstract static class SelectExprInsertNativeBase implements SelectExprProcessor, SelectExprProcessorForge {

        private final EventType eventType;
        protected final EventBeanManufacturer eventManufacturer;
        protected final ExprForge[] exprForges;

        protected ExprEvaluator[] exprEvaluators;

        protected SelectExprInsertNativeBase(EventType eventType, EventBeanManufacturer eventManufacturer, ExprForge[] exprForges, ExprEvaluator[] exprEvaluators) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
            this.exprForges = exprForges;
            this.exprEvaluators = exprEvaluators;
        }

        public EventType getResultEventType() {
            return eventType;
        }

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            if (exprEvaluators == null) {
                exprEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(exprForges, engineImportService, this.getClass(), isFireAndForget, statementName);
            }
            return this;
        }
    }

    public static class SelectExprInsertNativeWidening extends SelectExprInsertNativeBase {

        private final TypeWidener[] wideners;

        public SelectExprInsertNativeWidening(EventType eventType, EventBeanManufacturer eventManufacturer, ExprForge[] exprForges, ExprEvaluator[] exprEvaluators, TypeWidener[] wideners) {
            super(eventType, eventManufacturer, exprForges, exprEvaluators);
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

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember manufacturer = codegenClassScope.makeAddMember(EventBeanManufacturer.class, eventManufacturer);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(exprForges.length)));
            for (int i = 0; i < exprForges.length; i++) {
                CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(exprForges[i].getEvaluationType(), exprForges[i], methodNode, exprSymbol, codegenClassScope);
                if (wideners[i] == null) {
                    block.assignArrayElement("values", constant(i), expression);
                } else {
                    String refname = "evalResult" + i;
                    block.declareVar(exprForges[i].getEvaluationType(), refname, expression);
                    if (!exprForges[i].getEvaluationType().isPrimitive()) {
                        block.ifRefNotNull(refname)
                                .assignArrayElement("values", constant(i), wideners[i].widenCodegen(ref(refname), methodNode, codegenClassScope))
                                .blockEnd();
                    } else {
                        block.assignArrayElement("values", constant(i), wideners[i].widenCodegen(ref(refname), methodNode, codegenClassScope));
                    }
                }
            }
            block.methodReturn(exprDotMethod(member(manufacturer.getMemberId()), "make", ref("values")));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeNoWiden extends SelectExprInsertNativeBase {

        public SelectExprInsertNativeNoWiden(EventType eventType, EventBeanManufacturer eventManufacturer, ExprForge[] exprForges, ExprEvaluator[] exprEvaluators) {
            super(eventType, eventManufacturer, exprForges, exprEvaluators);
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            Object[] values = new Object[exprEvaluators.length];

            for (int i = 0; i < exprEvaluators.length; i++) {
                Object evalResult = exprEvaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                values[i] = evalResult;
            }

            return eventManufacturer.make(values);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember manufacturer = codegenClassScope.makeAddMember(EventBeanManufacturer.class, eventManufacturer);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(exprForges.length)));
            for (int i = 0; i < exprForges.length; i++) {
                CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(Object.class, exprForges[i], methodNode, exprSymbol, codegenClassScope);
                block.assignArrayElement("values", constant(i), expression);
            }
            block.methodReturn(exprDotMethod(member(manufacturer.getMemberId()), "make", ref("values")));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeNoEval implements SelectExprProcessor, SelectExprProcessorForge {

        private final EventType eventType;
        private final EventBeanManufacturer eventManufacturer;

        public SelectExprInsertNativeNoEval(EventType eventType, EventBeanManufacturer eventManufacturer) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            return eventManufacturer.make(CollectionUtil.OBJECTARRAY_EMPTY);
        }

        public EventType getResultEventType() {
            return eventType;
        }

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            return this;
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenMember member = codegenClassScope.makeAddMember(EventBeanManufacturer.class, eventManufacturer);
            methodNode.getBlock().methodReturn(exprDotMethod(member(member.getMemberId()), "make", publicConstValue(CollectionUtil.class, "OBJECTARRAY_EMPTY")));
            return methodNode;
        }
    }

    public static class ExprForgeJoinWildcard implements ExprForge, ExprEvaluator, ExprNodeRenderable {
        private final int streamNum;
        private final Class returnType;

        public ExprForgeJoinWildcard(int streamNum, Class returnType) {
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

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.SINGLE;
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(returnType, ExprForgeJoinWildcard.class, codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                    .declareVar(EventBean.class, "bean", arrayAtIndex(refEPS, constant(streamNum)))
                    .ifRefNullReturnNull("bean")
                    .methodReturn(cast(returnType, exprDotUnderlying(ref("bean"))));
            return localMethod(methodNode);
        }

        public Class getEvaluationType() {
            return returnType;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(this.getClass().getSimpleName());
        }
    }

    public static class ExprForgeStreamUnderlying implements ExprForge, ExprEvaluator, ExprNodeRenderable {

        private final int streamNumEval;
        private final Class returnType;

        public ExprForgeStreamUnderlying(int streamNumEval, Class returnType) {
            this.streamNumEval = streamNumEval;
            this.returnType = returnType;
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            EventBean theEvent = eventsPerStream[streamNumEval];
            if (theEvent != null) {
                return theEvent.getUnderlying();
            }
            return null;
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(returnType, this.getClass(), codegenClassScope);

            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                    .declareVar(EventBean.class, "theEvent", arrayAtIndex(refEPS, constant(streamNumEval)))
                    .ifRefNullReturnNull("theEvent")
                    .methodReturn(cast(returnType, exprDotUnderlying(ref("theEvent"))));
            return localMethod(methodNode);
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(ExprForgeStreamUnderlying.class.getSimpleName());
        }

        public Class getEvaluationType() {
            return Object.class;
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.SINGLE;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }
    }

    public static class ExprForgeStreamWithInner implements ExprForge, ExprEvaluator, ExprNodeRenderable {

        private final ExprForge inner;
        private final Class componentReturnType;
        private final ExprEvaluator innerEvaluator; // should be removed

        public ExprForgeStreamWithInner(ExprForge inner, Class componentReturnType) {
            this.inner = inner;
            this.componentReturnType = componentReturnType;
            this.innerEvaluator = inner.getExprEvaluator();
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            Object result = innerEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
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

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(ExprForgeStreamWithInner.class.getSimpleName());
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            Class arrayType = JavaClassHelper.getArrayType(componentReturnType);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(arrayType, this.getClass(), codegenClassScope);


            methodNode.getBlock()
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, inner.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope)))
                    .ifRefNullReturnNull("events")
                    .declareVar(arrayType, "values", newArrayByLength(componentReturnType, arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("values", ref("i"), cast(componentReturnType, exprDotUnderlying(arrayAtIndex(ref("events"), ref("i")))))
                    .blockEnd()
                    .methodReturn(ref("values"));
            return localMethod(methodNode);
        }

        public Class getEvaluationType() {
            return JavaClassHelper.getArrayType(componentReturnType);
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.INTER;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }
    }

    public static class ExprForgeStreamWithGetter implements ExprForge, ExprEvaluator, ExprNodeRenderable {

        private final EventPropertyGetterSPI getter;

        public ExprForgeStreamWithGetter(EventPropertyGetterSPI getter) {
            this.getter = getter;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            EventBean theEvent = eventsPerStream[0];
            if (theEvent != null) {
                return getter.get(theEvent);
            }
            return null;
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Object.class, ExprForgeStreamWithGetter.class, codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                    .declareVar(EventBean.class, "theEvent", arrayAtIndex(refEPS, constant(0)))
                    .ifRefNotNull("theEvent")
                    .blockReturn(getter.eventBeanGetCodegen(ref("theEvent"), methodNode, codegenClassScope))
                    .methodReturn(constantNull());
            return localMethod(methodNode);
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(ExprForgeStreamWithGetter.class.getSimpleName());
        }

        public Class getEvaluationType() {
            return Object.class;
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.SINGLE;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }
    }
}