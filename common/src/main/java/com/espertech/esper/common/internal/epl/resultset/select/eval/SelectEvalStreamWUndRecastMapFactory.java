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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalStreamWUndRecastMapFactory {

    public static SelectExprProcessorForge make(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, EventType targetType, ExprNode[] exprNodes, ClasspathImportServiceCompileTime classpathImportService, String statementName)
            throws ExprValidationException {
        MapEventType mapResultType = (MapEventType) targetType;
        MapEventType mapStreamType = (MapEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        ExprValidationException typeSameMssage = BaseNestableEventType.isDeepEqualsProperties(mapResultType.getName(), mapResultType.getTypes(), mapStreamType.getTypes());
        if (typeSameMssage == null && selectExprForgeContext.getExprForges().length == 0) {
            return new MapInsertProcessorSimpleRepackage(selectExprForgeContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(mapResultType, true, false);
        List<Item> items = new ArrayList<Item>();
        List<WriteablePropertyDescriptor> written = new ArrayList<WriteablePropertyDescriptor>();

        // find the properties coming from the providing source stream
        int count = 0;
        for (WriteablePropertyDescriptor writeable : writables) {
            String propertyName = writeable.getPropertyName();

            if (mapStreamType.getTypes().containsKey(propertyName)) {
                Object setOneType = mapStreamType.getTypes().get(propertyName);
                Object setTwoType = mapResultType.getTypes().get(propertyName);
                boolean setTwoTypeFound = mapResultType.getTypes().containsKey(propertyName);
                ExprValidationException message = BaseNestableEventUtil.comparePropType(propertyName, setOneType, setTwoType, setTwoTypeFound, mapResultType.getName());
                if (message != null) {
                    throw new ExprValidationException(message.getMessage(), message);
                }
                items.add(new Item(count, propertyName, null, null));
                written.add(writeable);
                count++;
            }
        }

        // find the properties coming from the expressions of the select clause
        for (int i = 0; i < selectExprForgeContext.getExprForges().length; i++) {
            String columnName = selectExprForgeContext.getColumnNames()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + mapResultType.getName() + "'");
            }

            try {
                TypeWidenerSPI widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getForge().getEvaluationType(),
                        writable.getType(), columnName, false, null, statementName);
                items.add(new Item(count, null, exprNode.getForge(), widener));
                written.add(writable);
                count++;
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        EventBeanManufacturerForge manufacturer;
        try {
            manufacturer = EventTypeUtility.getManufacturer(mapResultType,
                    written.toArray(new WriteablePropertyDescriptor[written.size()]), classpathImportService, true, null);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException("Failed to write to type: " + e.getMessage(), e);
        }

        return new MapInsertProcessorAllocate(streamNumber, itemsArr, manufacturer, targetType);
    }

    private static WriteablePropertyDescriptor findWritable(String columnName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(columnName)) {
                return writable;
            }
        }
        return null;
    }

    private static class MapInsertProcessorSimpleRepackage implements SelectExprProcessorForge {
        private final SelectExprForgeContext selectExprForgeContext;
        private final int underlyingStreamNumber;
        private final EventType resultType;

        private MapInsertProcessorSimpleRepackage(SelectExprForgeContext selectExprForgeContext, int underlyingStreamNumber, EventType resultType) {
            this.selectExprForgeContext = selectExprForgeContext;
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenExpression value = exprDotMethod(cast(MappedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))), "getProperties");
            methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedMap", value, resultEventType));
            return methodNode;
        }
    }

    private static class MapInsertProcessorAllocate implements SelectExprProcessorForge {
        private final int underlyingStreamNumber;
        private final Item[] items;
        private final EventBeanManufacturerForge manufacturer;
        private final EventType resultType;

        private MapInsertProcessorAllocate(int underlyingStreamNumber, Item[] items, EventBeanManufacturerForge manufacturer, EventType resultType) {
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.items = items;
            this.manufacturer = manufacturer;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenExpressionField manufacturerField = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, manufacturer.make(codegenMethodScope, codegenClassScope));
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(MappedEventBean.class, "theEvent", cast(MappedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))))
                    .declareVar(Object[].class, "props", newArrayByLength(Object.class, constant(items.length)));
            for (Item item : items) {
                CodegenExpression value;
                if (item.getOptionalPropertyName() != null) {
                    value = exprDotMethodChain(ref("theEvent")).add("getProperties").add("get", constant(item.getOptionalPropertyName()));
                } else {
                    if (item.getOptionalWidener() != null) {
                        value = item.forge.evaluateCodegen(item.forge.getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                        value = item.getOptionalWidener().widenCodegen(value, methodNode, codegenClassScope);
                    } else {
                        value = item.forge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
                    }
                }
                block.assignArrayElement("props", constant(item.getToIndex()), value);
            }
            block.methodReturn(exprDotMethod(manufacturerField, "make", ref("props")));
            return methodNode;
        }
    }

    private static class Item {
        private final int toIndex;
        private final String optionalPropertyName;
        private final ExprForge forge;
        private final TypeWidenerSPI optionalWidener;

        private ExprEvaluator evaluatorAssigned;

        private Item(int toIndex, String optionalPropertyName, ExprForge forge, TypeWidenerSPI optionalWidener) {
            this.toIndex = toIndex;
            this.optionalPropertyName = optionalPropertyName;
            this.forge = forge;
            this.optionalWidener = optionalWidener;
        }

        public int getToIndex() {
            return toIndex;
        }

        public String getOptionalPropertyName() {
            return optionalPropertyName;
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
