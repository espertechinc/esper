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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerException;
import com.espertech.esper.util.TypeWidenerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalSelectStreamWUndRecastObjectArrayFactory {

    public static SelectExprProcessorForge make(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, EventType targetType, ExprNode[] exprNodes, EngineImportService engineImportService, String statementName, String engineURI)
            throws ExprValidationException {
        ObjectArrayEventType oaResultType = (ObjectArrayEventType) targetType;
        ObjectArrayEventType oaStreamType = (ObjectArrayEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (oaResultType.isDeepEqualsConsiderOrder(oaStreamType) && selectExprForgeContext.getExprForges().length == 0) {
            return new OAInsertProcessorSimpleRepackage(selectExprForgeContext, streamNumber, targetType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = selectExprForgeContext.getEventAdapterService().getWriteableProperties(oaResultType, true);
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
        for (int i = 0; i < selectExprForgeContext.getExprForges().length; i++) {
            String columnName = selectExprForgeContext.getColumnNames()[i];
            ExprForge forge = selectExprForgeContext.getExprForges()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + oaResultType.getName() + "'");
            }

            TypeWidener widener;
            try {
                widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getForge().getEvaluationType(),
                        writable.getType(), columnName, false, null, statementName, engineURI);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }

            items.add(new Item(count, -1, forge, widener));
            written.add(writable);
            count++;
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[items.size()]);
        EventBeanManufacturer manufacturer;
        try {
            manufacturer = selectExprForgeContext.getEventAdapterService().getManufacturer(oaResultType,
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

    private static class OAInsertProcessorSimpleRepackage implements SelectExprProcessor, SelectExprProcessorForge {
        private final SelectExprForgeContext selectExprForgeContext;
        private final int underlyingStreamNumber;
        private final EventType resultType;

        private OAInsertProcessorSimpleRepackage(SelectExprForgeContext selectExprForgeContext, int underlyingStreamNumber, EventType resultType) {
            this.selectExprForgeContext = selectExprForgeContext;
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
            ObjectArrayBackedEventBean theEvent = (ObjectArrayBackedEventBean) eventsPerStream[underlyingStreamNumber];
            return selectExprForgeContext.getEventAdapterService().adapterForTypedObjectArray(theEvent.getProperties(), resultType);
        }

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            return this;
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenExpression value = exprDotMethod(cast(ObjectArrayBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))), "getProperties");
            methodNode.getBlock().methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", value, member(memberResultEventType.getMemberId())));
            return methodNode;
        }
    }

    private static class OAInsertProcessorAllocate implements SelectExprProcessor, SelectExprProcessorForge {
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

        public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].getForge() != null) {
                    items[i].setEvaluatorAssigned(ExprNodeCompiler.allocateEvaluator(items[i].forge, engineImportService, this.getClass(), isFireAndForget, statementName));
                }
            }
            return this;
        }

        public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {

            ObjectArrayBackedEventBean theEvent = (ObjectArrayBackedEventBean) eventsPerStream[underlyingStreamNumber];

            Object[] props = new Object[items.length];
            for (Item item : items) {
                Object value;

                if (item.getOptionalFromIndex() != -1) {
                    value = theEvent.getProperties()[item.getOptionalFromIndex()];
                } else {
                    value = item.getEvaluatorAssigned().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (item.getOptionalWidener() != null) {
                        value = item.getOptionalWidener().widen(value);
                    }
                }

                props[item.getToIndex()] = value;
            }

            return manufacturer.make(props);
        }

        public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember member = codegenClassScope.makeAddMember(EventBeanManufacturer.class, manufacturer);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenBlock block = methodNode.getBlock()
                    .declareVar(ObjectArrayBackedEventBean.class, "theEvent", cast(ObjectArrayBackedEventBean.class, arrayAtIndex(refEPS, constant(underlyingStreamNumber))))
                    .declareVar(Object[].class, "props", newArrayByLength(Object.class, constant(items.length)));
            for (Item item : items) {
                if (item.getOptionalFromIndex() != -1) {
                    block.assignArrayElement("props", constant(item.getToIndex()), arrayAtIndex(exprDotMethod(ref("theEvent"), "getProperties"), constant(item.getOptionalFromIndex())));
                } else {
                    CodegenExpression value;
                    if (item.getOptionalWidener() != null) {
                        value = item.forge.evaluateCodegen(item.forge.getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                        value = item.getOptionalWidener().widenCodegen(value, methodNode, codegenClassScope);
                    } else {
                        value = item.forge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
                    }
                    block.assignArrayElement("props", constant(item.getToIndex()), value);
                }
            }
            block.methodReturn(exprDotMethod(member(member.getMemberId()), "make", ref("props")));
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
