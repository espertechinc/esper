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
package com.espertech.esper.common.internal.avro.writer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.common.internal.avro.core.AvroGenericDataBackedEventBean;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroRecastFactory {

    public static SelectExprProcessorForge make(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, AvroSchemaEventType targetType, ExprNode[] exprNodes, String statementName)
            throws ExprValidationException {
        AvroEventType resultType = (AvroEventType) targetType;
        AvroEventType streamType = (AvroEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (resultType.getSchema().equals(streamType.getSchema()) && selectExprForgeContext.getExprForges().length == 0) {
            return new AvroInsertProcessorSimpleRepackage(selectExprForgeContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(resultType, true, false);
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
                } else {
                    throw new ExprValidationException("Type by name '" + resultType.getName() + "' " +
                            "in property '" + propertyName +
                            "' expected schema '" + resultTypeField.schema() +
                            "' but received schema '" + streamTypeField.schema() +
                            "'");
                }
            }
        }

        // find the properties coming from the expressions of the select clause
        TypeWidenerCustomizer typeWidenerCustomizer = selectExprForgeContext.getEventTypeAvroHandler().getTypeWidenerCustomizer(targetType);
        for (int i = 0; i < selectExprForgeContext.getExprForges().length; i++) {
            String columnName = selectExprForgeContext.getColumnNames()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + resultType.getName() + "'");
            }
            Schema.Field resultTypeField = resultType.getSchemaAvro().getField(writable.getPropertyName());

            TypeWidenerSPI widener;
            try {
                widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getForge().getEvaluationType(),
                        writable.getType(), columnName, false, typeWidenerCustomizer, statementName);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }

            items.add(new Item(resultTypeField.pos(), -1, exprNode.getForge(), widener));
            written.add(writable);
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        return new AvroInsertProcessorAllocate(streamNumber, itemsArr, resultType, resultType.getSchemaAvro(), selectExprForgeContext.getEventBeanTypedEventFactory());
    }

    private static WriteablePropertyDescriptor findWritable(String columnName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(columnName)) {
                return writable;
            }
        }
        return null;
    }

    private static class AvroInsertProcessorSimpleRepackage implements SelectExprProcessor, SelectExprProcessorForge {
        private final SelectExprForgeContext selectExprForgeContext;
        private final int underlyingStreamNumber;
        private final EventType resultType;

        private AvroInsertProcessorSimpleRepackage(SelectExprForgeContext selectExprForgeContext, int underlyingStreamNumber, EventType resultType) {
            this.selectExprForgeContext = selectExprForgeContext;
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            AvroGenericDataBackedEventBean theEvent = (AvroGenericDataBackedEventBean) eventsPerStream[underlyingStreamNumber];
            return selectExprForgeContext.getEventBeanTypedEventFactory().adapterForTypedAvro(theEvent.getProperties(), resultType);
        }

        public SelectExprProcessor getSelectExprProcessor(ClasspathImportService classpathImportService, boolean isFireAndForget, String statementName) {
            return this;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenExpression theEvent = cast(AvroGenericDataBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber)));
            methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedAvro", exprDotMethod(theEvent, "getProperties"), resultEventType));
            return methodNode;
        }
    }

    private static class AvroInsertProcessorAllocate implements SelectExprProcessor, SelectExprProcessorForge {
        private final int underlyingStreamNumber;
        private final Item[] items;
        private final EventType resultType;
        private final Schema resultSchema;
        private final EventBeanTypedEventFactory eventAdapterService;

        public AvroInsertProcessorAllocate(int underlyingStreamNumber, Item[] items, EventType resultType, Schema resultSchema, EventBeanTypedEventFactory eventAdapterService) {
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

            AvroGenericDataBackedEventBean theEvent = (AvroGenericDataBackedEventBean) eventsPerStream[underlyingStreamNumber];
            GenericData.Record source = theEvent.getProperties();
            GenericData.Record target = new GenericData.Record(resultSchema);
            for (Item item : items) {
                Object value;

                if (item.getOptionalFromIndex() != -1) {
                    value = source.get(item.getOptionalFromIndex());
                } else {
                    value = item.getEvaluatorAssigned().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (item.getOptionalWidener() != null) {
                        value = item.getOptionalWidener().widen(value);
                    }
                }

                target.put(item.getToIndex(), value);
            }

            return eventAdapterService.adapterForTypedAvro(target, resultType);
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenExpressionField schema = codegenClassScope.getPackageScope().addFieldUnshared(true, Schema.class, staticMethod(AvroSchemaUtil.class, "resolveAvroSchema", EventTypeUtility.resolveTypeCodegen(resultType, EPStatementInitServices.REF)));
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(AvroGenericDataBackedEventBean.class, "theEvent", cast(AvroGenericDataBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))))
                    .declareVar(GenericData.Record.class, "source", exprDotMethod(ref("theEvent"), "getProperties"))
                    .declareVar(GenericData.Record.class, "target", newInstance(GenericData.Record.class, schema));
            for (Item item : items) {
                CodegenExpression value;
                if (item.getOptionalFromIndex() != -1) {
                    value = exprDotMethod(ref("source"), "get", constant(item.getOptionalFromIndex()));
                } else {
                    if (item.getOptionalWidener() != null) {
                        value = item.forge.evaluateCodegen(item.getForge().getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                        value = item.getOptionalWidener().widenCodegen(value, methodNode, codegenClassScope);
                    } else {
                        value = item.forge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
                    }
                }
                block.exprDotMethod(ref("target"), "put", constant(item.getToIndex()), value);
            }
            block.methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedAvro", ref("target"), resultEventType));
            return methodNode;
        }
    }

    private static class Item {
        private final int toIndex;
        private final int optionalFromIndex;
        private final ExprForge forge;
        private final TypeWidenerSPI optionalWidener;

        private ExprEvaluator evaluatorAssigned;

        private Item(int toIndex, int optionalFromIndex, ExprForge forge, TypeWidenerSPI optionalWidener) {
            this.toIndex = toIndex;
            this.optionalFromIndex = optionalFromIndex;
            this.forge = forge;
            this.optionalWidener = optionalWidener;
        }

        public int getToIndex() {
            return toIndex;
        }

        public int getOptionalFromIndex() {
            return optionalFromIndex;
        }

        public ExprForge getForge() {
            return forge;
        }

        public TypeWidenerSPI getOptionalWidener() {
            return optionalWidener;
        }

        public ExprEvaluator getEvaluatorAssigned() {
            return evaluatorAssigned;
        }

        public void setEvaluatorAssigned(ExprEvaluator evaluatorAssigned) {
            this.evaluatorAssigned = evaluatorAssigned;
        }
    }
}
