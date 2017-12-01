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
package com.espertech.esper.avro.writer;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.avro.core.AvroGenericDataBackedEventBean;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.select.eval.SelectExprForgeContext;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.TypeWidenerException;
import com.espertech.esper.util.TypeWidenerFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroRecastFactory {

    public static SelectExprProcessorForge make(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, AvroSchemaEventType targetType, ExprNode[] exprNodes, String statementName, String engineURI)
            throws ExprValidationException {
        AvroEventType resultType = (AvroEventType) targetType;
        AvroEventType streamType = (AvroEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (resultType.getSchema().equals(streamType.getSchema()) && selectExprForgeContext.getExprForges().length == 0) {
            return new AvroInsertProcessorSimpleRepackage(selectExprForgeContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = selectExprForgeContext.getEventAdapterService().getWriteableProperties(resultType, true);
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
        TypeWidenerCustomizer typeWidenerCustomizer = selectExprForgeContext.getEventAdapterService().getTypeWidenerCustomizer(targetType);
        for (int i = 0; i < selectExprForgeContext.getExprForges().length; i++) {
            String columnName = selectExprForgeContext.getColumnNames()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + resultType.getName() + "'");
            }
            Schema.Field resultTypeField = resultType.getSchemaAvro().getField(writable.getPropertyName());

            TypeWidener widener;
            try {
                widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getForge().getEvaluationType(),
                        writable.getType(), columnName, false, typeWidenerCustomizer, statementName, engineURI);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }

            items.add(new Item(resultTypeField.pos(), -1, exprNode.getForge(), widener));
            written.add(writable);
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        return new AvroInsertProcessorAllocate(streamNumber, itemsArr, resultType, resultType.getSchemaAvro(), selectExprForgeContext.getEventAdapterService());
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
            return selectExprForgeContext.getEventAdapterService().adapterForTypedAvro(theEvent.getProperties(), resultType);
        }

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            return this;
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenExpression theEvent = cast(AvroGenericDataBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber)));
            methodNode.getBlock().methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedAvro", exprDotMethod(theEvent, "getProperties"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    private static class AvroInsertProcessorAllocate implements SelectExprProcessor, SelectExprProcessorForge {
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

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].getForge() != null) {
                    items[i].setEvaluatorAssigned(ExprNodeCompiler.allocateEvaluator(items[i].forge, engineImportService, this.getClass(), isFireAndForget, statementName));
                }
            }
            return this;
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

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember schemaMember = codegenClassScope.makeAddMember(Schema.class, resultSchema);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(AvroGenericDataBackedEventBean.class, "theEvent", cast(AvroGenericDataBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))))
                    .declareVar(GenericData.Record.class, "source", exprDotMethod(ref("theEvent"), "getProperties"))
                    .declareVar(GenericData.Record.class, "target", newInstance(GenericData.Record.class, member(schemaMember.getMemberId())));
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
            block.methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedAvro", ref("target"), member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    private static class Item {
        private final int toIndex;
        private final int optionalFromIndex;
        private final ExprForge forge;
        private final TypeWidener optionalWidener;

        private ExprEvaluator evaluatorAssigned;

        private Item(int toIndex, int optionalFromIndex, ExprForge forge, TypeWidener optionalWidener) {
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

        public TypeWidener getOptionalWidener() {
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
