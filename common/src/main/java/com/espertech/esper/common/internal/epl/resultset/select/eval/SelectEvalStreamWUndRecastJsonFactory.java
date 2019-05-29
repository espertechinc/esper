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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalStreamWUndRecastJsonFactory {

    public static SelectExprProcessorForge make(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, EventType targetType, ExprNode[] exprNodes, ClasspathImportServiceCompileTime classpathImportService, String statementName)
        throws ExprValidationException {
        JsonEventType jsonResultType = (JsonEventType) targetType;
        JsonEventType jsonStreamType = (JsonEventType) eventTypes[streamNumber];

        // (A) fully assignment-compatible: same number, name and type of fields, no additional expressions: Straight repackage
        if (jsonResultType.isDeepEqualsConsiderOrder(jsonStreamType) && selectExprForgeContext.getExprForges().length == 0) {
            return new JsonInsertProcessorStraightFieldAssign(streamNumber, jsonStreamType, jsonResultType);
        }

        // (B) not completely assignable: find matching properties
        Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(jsonResultType, true, false);
        List<Item> items = new ArrayList<Item>();
        List<WriteablePropertyDescriptor> written = new ArrayList<WriteablePropertyDescriptor>();

        // find the properties coming from the providing source stream
        for (WriteablePropertyDescriptor writeable : writables) {
            String propertyName = writeable.getPropertyName();

            JsonUnderlyingField fieldSource = jsonStreamType.getDetail().getFieldDescriptors().get(propertyName);
            JsonUnderlyingField fieldTarget = jsonResultType.getDetail().getFieldDescriptors().get(propertyName);

            if (fieldSource != null) {
                Object setOneType = jsonStreamType.getTypes().get(propertyName);
                Object setTwoType = jsonResultType.getTypes().get(propertyName);
                boolean setTwoTypeFound = jsonResultType.getTypes().containsKey(propertyName);
                ExprValidationException message = BaseNestableEventUtil.comparePropType(propertyName, setOneType, setTwoType, setTwoTypeFound, jsonResultType.getName());
                if (message != null) {
                    throw new ExprValidationException(message.getMessage(), message);
                }
                items.add(new Item(fieldTarget, fieldSource, null, null));
                written.add(writeable);
            }
        }

        // find the properties coming from the expressions of the select clause
        for (int i = 0; i < selectExprForgeContext.getExprForges().length; i++) {
            String columnName = selectExprForgeContext.getColumnNames()[i];
            ExprForge forge = selectExprForgeContext.getExprForges()[i];
            ExprNode exprNode = exprNodes[i];

            WriteablePropertyDescriptor writable = findWritable(columnName, writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find column '" + columnName + "' in target type '" + jsonResultType.getName() + "'");
            }
            JsonUnderlyingField fieldTarget = jsonResultType.getDetail().getFieldDescriptors().get(writable.getPropertyName());

            TypeWidenerSPI widener;
            try {
                widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode), exprNode.getForge().getEvaluationType(),
                    writable.getType(), columnName, false, null, statementName);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }

            items.add(new Item(fieldTarget, null, forge, widener));
            written.add(writable);
        }

        // make manufacturer
        Item[] itemsArr = items.toArray(new Item[0]);
        return new JsonInsertProcessorExpressions(streamNumber, itemsArr, jsonStreamType, jsonResultType);
    }

    private static WriteablePropertyDescriptor findWritable(String columnName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(columnName)) {
                return writable;
            }
        }
        return null;
    }

    private static class JsonInsertProcessorStraightFieldAssign implements SelectExprProcessorForge {
        private final int underlyingStreamNumber;
        private final JsonEventType sourceType;
        private final JsonEventType resultType;

        private JsonInsertProcessorStraightFieldAssign(int underlyingStreamNumber, JsonEventType sourceType, JsonEventType resultType) {
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.sourceType = sourceType;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            methodNode.getBlock()
                .declareVar(resultType.getUnderlyingType(), "und", newInstance(resultType.getUnderlyingType()))
                .declareVar(sourceType.getUnderlyingType(), "src", castUnderlying(sourceType.getUnderlyingType(), arrayAtIndex(refEPS, constant(underlyingStreamNumber))));
            for (Map.Entry<String, JsonUnderlyingField> sourceFieldEntry : sourceType.getDetail().getFieldDescriptors().entrySet()) {
                JsonUnderlyingField targetField = resultType.getDetail().getFieldDescriptors().get(sourceFieldEntry.getKey());
                methodNode.getBlock().assignRef("und." + targetField.getFieldName(), ref("src." + sourceFieldEntry.getValue().getFieldName()));
            }
            methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
            return methodNode;
        }
    }

    private static class JsonInsertProcessorExpressions implements SelectExprProcessorForge {
        private final int underlyingStreamNumber;
        private final Item[] items;
        private final JsonEventType sourceType;
        private final JsonEventType resultType;

        private JsonInsertProcessorExpressions(int underlyingStreamNumber, Item[] items, JsonEventType sourceType, JsonEventType resultType) {
            this.underlyingStreamNumber = underlyingStreamNumber;
            this.items = items;
            this.sourceType = sourceType;
            this.resultType = resultType;
        }

        public EventType getResultEventType() {
            return resultType;
        }

        public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
            CodegenBlock block = methodNode.getBlock()
                .declareVar(sourceType.getUnderlyingType(), "src", castUnderlying(sourceType.getUnderlyingType(), arrayAtIndex(refEPS, constant(underlyingStreamNumber))))
                .declareVar(resultType.getUnderlyingType(), "und", newInstance(resultType.getUnderlyingType()));
            for (Item item : items) {
                if (item.getOptionalFromField() != null) {
                    block.assignRef("und." + item.getToField().getFieldName(), ref("src." + item.getOptionalFromField().getFieldName()));
                } else {
                    CodegenExpression value;
                    if (item.getOptionalWidener() != null) {
                        value = item.forge.evaluateCodegen(item.forge.getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                        value = item.getOptionalWidener().widenCodegen(value, methodNode, codegenClassScope);
                    } else {
                        value = item.forge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
                    }
                    block.assignRef("und." + item.getToField().getFieldName(), value);
                }
            }
            methodNode.getBlock().methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedJson", ref("und"), resultEventType));
            return methodNode;
        }
    }

    private static class Item {
        private final JsonUnderlyingField toField;
        private final JsonUnderlyingField optionalFromField;
        private final ExprForge forge;
        private final TypeWidenerSPI optionalWidener;

        private ExprEvaluator evaluatorAssigned;

        private Item(JsonUnderlyingField toField, JsonUnderlyingField optionalFromField, ExprForge forge, TypeWidenerSPI optionalWidener) {
            if (toField == null) {
                throw new IllegalArgumentException("Null to-field");
            }
            this.toField = toField;
            this.optionalFromField = optionalFromField;
            this.forge = forge;
            this.optionalWidener = optionalWidener;
        }

        public JsonUnderlyingField getToField() {
            return toField;
        }

        public JsonUnderlyingField getOptionalFromField() {
            return optionalFromField;
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
