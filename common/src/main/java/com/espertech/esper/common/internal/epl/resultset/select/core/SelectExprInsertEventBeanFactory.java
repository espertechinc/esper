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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.InsertIntoDesc;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.manufacturer.EventBeanManufacturerCtorForge;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturerUtil;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.*;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprInsertEventBeanFactory {
    public static SelectExprProcessorForge getInsertUnderlyingNonJoin(EventType eventType,
                                                                      boolean isUsingWildcard,
                                                                      StreamTypeService typeService,
                                                                      ExprForge[] forges,
                                                                      String[] columnNames,
                                                                      Object[] expressionReturnTypes,
                                                                      InsertIntoDesc insertIntoDesc,
                                                                      String[] columnNamesAsProvided,
                                                                      boolean allowNestableTargetFragmentTypes,
                                                                      String statementName,
                                                                      ClasspathImportServiceCompileTime classpathImportService,
                                                                      EventTypeAvroHandler eventTypeAvroHandler)
        throws ExprValidationException {

        // handle single-column coercion to underlying, i.e. "insert into MapDefinedEvent select doSomethingReturnMap() from MyEvent"
        if (expressionReturnTypes.length == 1 &&
            expressionReturnTypes[0] instanceof EPTypeClass &&
            insertIntoDesc.getColumnNames().isEmpty() &&
            columnNamesAsProvided[0] == null) {

            EPTypeClass resultType = (EPTypeClass) expressionReturnTypes[0];
            boolean compatible = (eventType instanceof BaseNestableEventType || eventType instanceof AvroSchemaEventType) &&
                JavaClassHelper.isSubclassOrImplementsInterface(resultType, eventType.getUnderlyingEPType().getType());
            compatible = compatible | (eventType instanceof JsonEventType && resultType.getType() == String.class);

            if (compatible) {
                if (eventType instanceof MapEventType) {
                    return new SelectExprInsertNativeExpressionCoerceMap(eventType, forges[0]);
                } else if (eventType instanceof ObjectArrayEventType) {
                    return new SelectExprInsertNativeExpressionCoerceObjectArray(eventType, forges[0]);
                } else if (eventType instanceof AvroSchemaEventType) {
                    return new SelectExprInsertNativeExpressionCoerceAvro(eventType, forges[0]);
                } else if (eventType instanceof JsonEventType) {
                    return new SelectExprInsertNativeExpressionCoerceJson(eventType, forges[0]);
                } else {
                    throw new IllegalStateException("Unrecognied event type " + eventType);
                }
            }
        }

        // handle special case where the target type has no properties and there is a single "null" value selected
        if (eventType.getPropertyDescriptors().length == 0 &&
            columnNames.length == 1 &&
            columnNames[0].equals("null") &&
            (expressionReturnTypes[0] == null || expressionReturnTypes[0] == EPTypeNull.INSTANCE) &&
            !isUsingWildcard) {

            EventBeanManufacturerForge eventManufacturer;
            try {
                eventManufacturer = EventTypeUtility.getManufacturer(eventType, new WriteablePropertyDescriptor[0], classpathImportService, true, eventTypeAvroHandler);
            } catch (EventBeanManufactureException e) {
                throw new ExprValidationException(e.getMessage(), e);
            }
            return new SelectExprInsertNativeNoEval(eventType, eventManufacturer);
        }

        // handle writing to defined columns
        Set<WriteablePropertyDescriptor> writableProps = EventTypeUtility.getWriteableProperties(eventType, false, false);
        boolean isEligible = checkEligible(eventType, writableProps, allowNestableTargetFragmentTypes);
        if (!isEligible) {
            return null;
        }

        try {
            return initializeSetterManufactor(eventType, writableProps, isUsingWildcard, typeService, forges, columnNames, expressionReturnTypes, statementName, classpathImportService, eventTypeAvroHandler);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                return initializeCtorInjection((BeanEventType) eventType, forges, expressionReturnTypes, classpathImportService);
            } catch (ExprValidationException ctorEx) {
                if (writableProps.isEmpty()) {
                    throw ctorEx;
                }
                throw ex;
            }
        }
    }

    public static SelectExprProcessorForge getInsertUnderlyingJoinWildcard(EventType eventType, String[] streamNames, EventType[] streamTypes,
                                                                           ClasspathImportServiceCompileTime classpathImportService, String statementName,
                                                                           EventTypeAvroHandler eventTypeAvroHandler)
        throws ExprValidationException {
        Set<WriteablePropertyDescriptor> writableProps = EventTypeUtility.getWriteableProperties(eventType, false, false);
        boolean isEligible = checkEligible(eventType, writableProps, false);
        if (!isEligible) {
            return null;
        }

        try {
            return initializeJoinWildcardInternal(eventType, writableProps, streamNames, streamTypes, statementName, classpathImportService, eventTypeAvroHandler);
        } catch (ExprValidationException ex) {
            if (!(eventType instanceof BeanEventType)) {
                throw ex;
            }
            // Try constructor injection
            try {
                ExprForge[] forges = new ExprForge[streamTypes.length];
                Object[] resultTypes = new Object[streamTypes.length];
                for (int i = 0; i < streamTypes.length; i++) {
                    forges[i] = new ExprForgeJoinWildcard(i, streamTypes[i].getUnderlyingEPType());
                    resultTypes[i] = forges[i].getEvaluationType();
                }
                return initializeCtorInjection((BeanEventType) eventType, forges, resultTypes, classpathImportService);
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

    private static SelectExprProcessorForge initializeSetterManufactor(EventType eventType, Set<WriteablePropertyDescriptor> writables, boolean isUsingWildcard, StreamTypeService typeService, ExprForge[] expressionForges, String[] columnNames, Object[] expressionReturnTypes, String statementName, ClasspathImportServiceCompileTime classpathImportService, EventTypeAvroHandler eventTypeAvroHandler)
        throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventTypeAvroHandler.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprForge> forgesList = new ArrayList<>();
        List<TypeWidenerSPI> widenersList = new ArrayList<TypeWidenerSPI>();

        // loop over all columns selected, if any
        for (int i = 0; i < columnNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidenerSPI widener = null;
            ExprForge forge = expressionForges[i];

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(columnNames[i])) {
                    continue;
                }

                Object columnType = expressionReturnTypes[i];
                if (columnType == null) {
                    try {
                        TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], null, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName);
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }
                } else if (columnType instanceof EventType) {
                    EventType columnEventType = (EventType) columnType;
                    final EPTypeClass returnType = columnEventType.getUnderlyingEPType();
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], columnEventType.getUnderlyingEPType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName);
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }

                    // handle evaluator returning an event
                    if (desc.getType() instanceof EPTypeClass && JavaClassHelper.isSubclassOrImplementsInterface(returnType, (EPTypeClass) desc.getType())) {
                        selectedWritable = desc;
                        widener = new TypeWidenerSPI() {
                            public Object widen(Object input) {
                                if (input instanceof EventBean) {
                                    return ((EventBean) input).getUnderlying();
                                }
                                return input;
                            }

                            public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
                                CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), TypeWidenerSPI.class, codegenClassScope).addParam(EPTypePremade.OBJECT.getEPType(), "input").getBlock()
                                    .ifCondition(instanceOf(ref("input"), EventBean.EPTYPE))
                                    .blockReturn(exprDotMethod(cast(EventBean.EPTYPE, ref("input")), "getUnderlying"))
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
                    forge = new ExprForgeStreamUnderlying(streamNum, typeService.getEventTypes()[streamNum].getUnderlyingEPType());
                } else if (columnType instanceof EventType[]) {
                    // handle case where the select-clause contains an fragment array
                    EventType columnEventType = ((EventType[]) columnType)[0];
                    final EPTypeClass componentReturnType = columnEventType.getUnderlyingEPType();
                    final EPTypeClass arrayReturnType = JavaClassHelper.getArrayType(componentReturnType);

                    boolean allowObjectArrayToCollectionConversion = eventType instanceof AvroSchemaEventType;
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], arrayReturnType, desc.getType(), desc.getPropertyName(), allowObjectArrayToCollectionConversion, typeWidenerCustomizer, statementName);
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }

                    final ExprForge inner = forge;
                    forge = new ExprForgeStreamWithInner(inner, componentReturnType);
                } else if (columnType instanceof EPType) {
                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(columnNames[i], (EPType) columnType, desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName);
                    } catch (TypeWidenerException ex) {
                        throw new ExprValidationException(ex.getMessage(), ex);
                    }
                } else {
                    String message = "Invalid assignment of column '" + columnNames[i] +
                        "' of type '" + columnType +
                        "' to event property '" + desc.getPropertyName() +
                        "' typed as '" + desc.getType().getTypeName() +
                        "', column and parameter types mismatch";
                    throw new ExprValidationException(message);
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
                TypeWidenerSPI widener = null;
                ExprForge forge = null;

                for (WriteablePropertyDescriptor writableDesc : writables) {
                    if (!writableDesc.getPropertyName().equals(eventPropDescriptor.getPropertyName())) {
                        continue;
                    }

                    try {
                        widener = TypeWidenerFactory.getCheckPropertyAssignType(eventPropDescriptor.getPropertyName(), eventPropDescriptor.getPropertyEPType(), writableDesc.getType(), writableDesc.getPropertyName(), false, typeWidenerCustomizer, statementName);
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
        TypeWidenerSPI[] wideners = widenersList.toArray(new TypeWidenerSPI[widenersList.size()]);

        EventBeanManufacturerForge eventManufacturer;
        try {
            eventManufacturer = EventTypeUtility.getManufacturer(eventType, writableProperties, classpathImportService, false, eventTypeAvroHandler);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        if (eventManufacturer == null) {
            return null;
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprForges, wideners);
    }

    private static SelectExprProcessorForge initializeCtorInjection(BeanEventType beanEventType, ExprForge[] forges, Object[] expressionReturnTypes, ClasspathImportServiceCompileTime classpathImportService)
        throws ExprValidationException {

        Pair<Constructor, ExprForge[]> pair = InstanceManufacturerUtil.getManufacturer(beanEventType.getUnderlyingEPType(), classpathImportService, forges, expressionReturnTypes);
        EventBeanManufacturerCtorForge eventManufacturer = new EventBeanManufacturerCtorForge(pair.getFirst(), beanEventType);
        return new SelectExprInsertNativeNoWiden(beanEventType, eventManufacturer, pair.getSecond());
    }

    private static SelectExprProcessorForge initializeJoinWildcardInternal(EventType eventType, Set<WriteablePropertyDescriptor> writables, String[] streamNames, EventType[] streamTypes, String statementName, ClasspathImportServiceCompileTime classpathImportService, EventTypeAvroHandler eventTypeAvroHandler)
        throws ExprValidationException {
        TypeWidenerCustomizer typeWidenerCustomizer = eventTypeAvroHandler.getTypeWidenerCustomizer(eventType);
        List<WriteablePropertyDescriptor> writablePropertiesList = new ArrayList<WriteablePropertyDescriptor>();
        List<ExprForge> forgesList = new ArrayList<>();
        List<TypeWidenerSPI> widenersList = new ArrayList<TypeWidenerSPI>();

        // loop over all columns selected, if any
        for (int i = 0; i < streamNames.length; i++) {
            WriteablePropertyDescriptor selectedWritable = null;
            TypeWidenerSPI widener = null;

            for (WriteablePropertyDescriptor desc : writables) {
                if (!desc.getPropertyName().equals(streamNames[i])) {
                    continue;
                }

                try {
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(streamNames[i], streamTypes[i].getUnderlyingEPType(), desc.getType(), desc.getPropertyName(), false, typeWidenerCustomizer, statementName);
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

            ExprForge forge = new ExprForgeStreamUnderlying(i, streamTypes[i].getUnderlyingEPType());

            // add
            writablePropertiesList.add(selectedWritable);
            forgesList.add(forge);
            widenersList.add(widener);
        }

        // assign
        WriteablePropertyDescriptor[] writableProperties = writablePropertiesList.toArray(new WriteablePropertyDescriptor[writablePropertiesList.size()]);
        ExprForge[] exprForges = forgesList.toArray(new ExprForge[forgesList.size()]);
        TypeWidenerSPI[] wideners = widenersList.toArray(new TypeWidenerSPI[widenersList.size()]);

        EventBeanManufacturerForge eventManufacturer;
        try {
            eventManufacturer = EventTypeUtility.getManufacturer(eventType, writableProperties, classpathImportService, false, eventTypeAvroHandler);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException(e.getMessage(), e);
        }

        return new SelectExprInsertNativeWidening(eventType, eventManufacturer, exprForges, wideners);
    }

    public abstract static class SelectExprInsertNativeExpressionCoerceBase implements SelectExprProcessorForge {

        protected final EventType eventType;
        protected final ExprForge exprForge;
        protected ExprEvaluator evaluator;

        protected SelectExprInsertNativeExpressionCoerceBase(EventType eventType, ExprForge exprForge) {
            this.eventType = eventType;
            this.exprForge = exprForge;
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceMap extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceMap(EventType eventType, ExprForge exprForge) {
            super(eventType, exprForge);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            CodegenExpression expr = exprForge.evaluateCodegen(EPTypePremade.MAP.getEPType(), methodNode, exprSymbol, codegenClassScope);
            if (!JavaClassHelper.isSubclassOrImplementsInterface(exprForge.getEvaluationType(), Map.class)) {
                expr = cast(EPTypePremade.MAP.getEPType(), expr);
            }
            methodNode.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "result", expr)
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedMap", ref("result"), resultEventType));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceAvro extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceAvro(EventType eventType, ExprForge exprForge) {
            super(eventType, exprForge);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "result", exprForge.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedAvro", ref("result"), resultEventType));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceObjectArray extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceObjectArray(EventType eventType, ExprForge exprForge) {
            super(eventType, exprForge);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "result", exprForge.evaluateCodegen(EPTypePremade.OBJECTARRAY.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedObjectArray", ref("result"), resultEventType));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceJson extends SelectExprInsertNativeExpressionCoerceBase {

        protected SelectExprInsertNativeExpressionCoerceJson(EventType eventType, ExprForge exprForge) {
            super(eventType, exprForge);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                .declareVar(EPTypePremade.STRING.getEPType(), "result", exprForge.evaluateCodegen(EPTypePremade.STRING.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .declareVar(EPTypePremade.OBJECT.getEPType(), "und", exprDotMethod(cast(JsonEventType.EPTYPE, resultEventType), "parse", ref("result")))
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeExpressionCoerceNative extends SelectExprInsertNativeExpressionCoerceBase {
        protected SelectExprInsertNativeExpressionCoerceNative(EventType eventType, ExprForge exprForge) {
            super(eventType, exprForge);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "result", exprForge.evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedBean", ref("result"), resultEventType));
            return methodNode;
        }
    }

    public abstract static class SelectExprInsertNativeBase implements SelectExprProcessorForge {

        private final EventType eventType;
        protected final EventBeanManufacturerForge eventManufacturer;
        protected final ExprForge[] exprForges;

        protected SelectExprInsertNativeBase(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
            this.exprForges = exprForges;
        }

        public EventType getResultEventType() {
            return eventType;
        }
    }

    public static class SelectExprInsertNativeWidening extends SelectExprInsertNativeBase {

        private final TypeWidenerSPI[] wideners;

        public SelectExprInsertNativeWidening(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges, TypeWidenerSPI[] wideners) {
            super(eventType, eventManufacturer, exprForges);
            this.wideners = wideners;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, eventManufacturer.make(codegenMethodScope, codegenClassScope));
            CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(exprForges.length)));
            for (int i = 0; i < exprForges.length; i++) {
                EPType evalType = exprForges[i].getEvaluationType();
                CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(evalType, exprForges[i], methodNode, exprSymbol, codegenClassScope);
                if (wideners[i] == null) {
                    block.assignArrayElement("values", constant(i), expression);
                } else {
                    String refname = "evalResult" + i;
                    if (evalType == null || evalType == EPTypeNull.INSTANCE) {
                        // no action
                    } else {
                        EPTypeClass evalClass = (EPTypeClass) evalType;
                        block.declareVar(evalClass, refname, expression);
                        if (!evalClass.getType().isPrimitive()) {
                            block.ifRefNotNull(refname)
                                .assignArrayElement("values", constant(i), wideners[i].widenCodegen(ref(refname), methodNode, codegenClassScope))
                                .blockEnd();
                        } else {
                            block.assignArrayElement("values", constant(i), wideners[i].widenCodegen(ref(refname), methodNode, codegenClassScope));
                        }
                    }
                }
            }
            block.methodReturn(exprDotMethod(manufacturer, "make", ref("values")));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeNoWiden extends SelectExprInsertNativeBase {

        public SelectExprInsertNativeNoWiden(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges) {
            super(eventType, eventManufacturer, exprForges);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, eventManufacturer.make(codegenMethodScope, codegenClassScope));
            CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(exprForges.length)));
            for (int i = 0; i < exprForges.length; i++) {
                CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(EPTypePremade.OBJECT.getEPType(), exprForges[i], methodNode, exprSymbol, codegenClassScope);
                block.assignArrayElement("values", constant(i), expression);
            }
            block.methodReturn(exprDotMethod(manufacturer, "make", ref("values")));
            return methodNode;
        }
    }

    public static class SelectExprInsertNativeNoEval implements SelectExprProcessorForge {

        private final EventType eventType;
        private final EventBeanManufacturerForge eventManufacturer;

        public SelectExprInsertNativeNoEval(EventType eventType, EventBeanManufacturerForge eventManufacturer) {
            this.eventType = eventType;
            this.eventManufacturer = eventManufacturer;
        }

        public EventType getResultEventType() {
            return eventType;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
            CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, eventManufacturer.make(codegenMethodScope, codegenClassScope));
            methodNode.getBlock().methodReturn(exprDotMethod(manufacturer, "make", publicConstValue(CollectionUtil.class, "OBJECTARRAY_EMPTY")));
            return methodNode;
        }
    }

    public static class ExprForgeJoinWildcard implements ExprForge, ExprEvaluator, ExprNodeRenderable {
        private final int streamNum;
        private final EPTypeClass returnType;

        public ExprForgeJoinWildcard(int streamNum, EPTypeClass returnType) {
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

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, ExprForgeJoinWildcard.class, codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "bean", arrayAtIndex(refEPS, constant(streamNum)))
                .ifRefNullReturnNull("bean")
                .methodReturn(cast(returnType, exprDotUnderlying(ref("bean"))));
            return localMethod(methodNode);
        }

        public EPTypeClass getEvaluationType() {
            return returnType;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
            writer.append(this.getClass().getSimpleName());
        }
    }

    public static class ExprForgeStreamUnderlying implements ExprForge, ExprEvaluator, ExprNodeRenderable {

        private final int streamNumEval;
        private final EPTypeClass returnType;

        public ExprForgeStreamUnderlying(int streamNumEval, EPTypeClass returnType) {
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

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, this.getClass(), codegenClassScope);

            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "theEvent", arrayAtIndex(refEPS, constant(streamNumEval)))
                .ifRefNullReturnNull("theEvent")
                .methodReturn(cast(returnType, exprDotUnderlying(ref("theEvent"))));
            return localMethod(methodNode);
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
            writer.append(ExprForgeStreamUnderlying.class.getSimpleName());
        }

        public EPTypeClass getEvaluationType() {
            return EPTypePremade.OBJECT.getEPType();
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
        }
    }

    public static class ExprForgeStreamWithInner implements ExprForge, ExprEvaluator, ExprNodeRenderable {

        private final ExprForge inner;
        private final EPTypeClass componentReturnType;

        public ExprForgeStreamWithInner(ExprForge inner, EPTypeClass componentReturnType) {
            this.inner = inner;
            this.componentReturnType = componentReturnType;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
            throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
            writer.append(ExprForgeStreamWithInner.class.getSimpleName());
        }

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            EPTypeClass arrayType = JavaClassHelper.getArrayType(componentReturnType);
            CodegenMethod methodNode = codegenMethodScope.makeChild(arrayType, this.getClass(), codegenClassScope);


            methodNode.getBlock()
                .declareVar(EventBean.EPTYPEARRAY, "events", cast(EventBean.EPTYPEARRAY, inner.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope)))
                .ifRefNullReturnNull("events")
                .declareVar(arrayType, "values", newArrayByLength(componentReturnType, arrayLength(ref("events"))))
                .forLoopIntSimple("i", arrayLength(ref("events")))
                .assignArrayElement("values", ref("i"), cast(componentReturnType, exprDotUnderlying(arrayAtIndex(ref("events"), ref("i")))))
                .blockEnd()
                .methodReturn(ref("values"));
            return localMethod(methodNode);
        }

        public EPTypeClass getEvaluationType() {
            return JavaClassHelper.getArrayType(componentReturnType);
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
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

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), ExprForgeStreamWithGetter.class, codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "theEvent", arrayAtIndex(refEPS, constant(0)))
                .ifRefNotNull("theEvent")
                .blockReturn(getter.eventBeanGetCodegen(ref("theEvent"), methodNode, codegenClassScope))
                .methodReturn(constantNull());
            return localMethod(methodNode);
        }

        public ExprEvaluator getExprEvaluator() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
            writer.append(ExprForgeStreamWithGetter.class.getSimpleName());
        }

        public EPTypeClass getEvaluationType() {
            return EPTypePremade.OBJECT.getEPType();
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
        }
    }
}