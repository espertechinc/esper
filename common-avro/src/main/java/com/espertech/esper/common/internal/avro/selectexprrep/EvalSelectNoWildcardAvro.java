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
package com.espertech.esper.common.internal.avro.selectexprrep;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalByGetterFragment;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalStreamInsertNamedWindow;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalStreamInsertBean;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprInsertEventBeanFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableMapForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.nio.ByteBuffer;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EvalSelectNoWildcardAvro implements SelectExprProcessorForge {

    private final SelectExprForgeContext selectExprForgeContext;
    private final AvroEventType resultEventTypeAvro;
    private final ExprForge[] forges;

    public EvalSelectNoWildcardAvro(SelectExprForgeContext selectExprForgeContext, ExprForge[] exprForges, EventType resultEventTypeAvro, String statementName) throws ExprValidationException {
        this.selectExprForgeContext = selectExprForgeContext;
        this.resultEventTypeAvro = (AvroEventType) resultEventTypeAvro;

        this.forges = new ExprForge[selectExprForgeContext.getExprForges().length];
        TypeWidenerCustomizer typeWidenerCustomizer = selectExprForgeContext.getEventTypeAvroHandler().getTypeWidenerCustomizer(resultEventTypeAvro);
        for (int i = 0; i < forges.length; i++) {
            forges[i] = selectExprForgeContext.getExprForges()[i];
            ExprForge forge = exprForges[i];
            Class forgeEvaluationType = forge.getEvaluationType();

            if (forge instanceof ExprEvalByGetterFragment) {
                forges[i] = handleFragment((ExprEvalByGetterFragment) forge);
            } else if (forge instanceof ExprEvalStreamInsertBean) {
                ExprEvalStreamInsertBean und = (ExprEvalStreamInsertBean) forge;
                forges[i] = new SelectExprInsertEventBeanFactory.ExprForgeStreamUnderlying(und.getStreamNum(), Object.class);
            } else if (forge instanceof SelectExprProcessorTypableMapForge) {
                SelectExprProcessorTypableMapForge typableMap = (SelectExprProcessorTypableMapForge) forge;
                forges[i] = new SelectExprProcessorEvalAvroMapToAvro(typableMap.getInnerForge(), ((AvroEventType) resultEventTypeAvro).getSchemaAvro(), selectExprForgeContext.getColumnNames()[i]);
            } else if (forge instanceof ExprEvalStreamInsertNamedWindow) {
                ExprEvalStreamInsertNamedWindow nw = (ExprEvalStreamInsertNamedWindow) forge;
                forges[i] = new SelectExprInsertEventBeanFactory.ExprForgeStreamUnderlying(nw.getStreamNum(), Object.class);
            } else if (forgeEvaluationType != null && forgeEvaluationType.isArray()) {
                TypeWidenerSPI widener = TypeWidenerFactory.getArrayToCollectionCoercer(forgeEvaluationType.getComponentType());
                Class resultType = Collection.class;
                if (forgeEvaluationType == byte[].class) {
                    widener = TypeWidenerFactory.BYTE_ARRAY_TO_BYTE_BUFFER_COERCER;
                    resultType = ByteBuffer.class;
                }
                forges[i] = new SelectExprProcessorEvalAvroArrayCoercer(forge, widener, resultType);
            } else {
                String propertyName = selectExprForgeContext.getColumnNames()[i];
                Class propertyType = resultEventTypeAvro.getPropertyType(propertyName);
                TypeWidenerSPI widener;
                try {
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(propertyName, forgeEvaluationType, propertyType, propertyName, true, typeWidenerCustomizer, statementName);
                } catch (TypeWidenerException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
                if (widener != null) {
                    forges[i] = new SelectExprProcessorEvalAvroArrayCoercer(forge, widener, propertyType);
                }
            }
        }
    }

    public EventType getResultEventType() {
        return resultEventTypeAvro;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField schema = codegenClassScope.getPackageScope().addFieldUnshared(true, Schema.class, staticMethod(AvroSchemaUtil.class, "resolveAvroSchema", EventTypeUtility.resolveTypeCodegen(resultEventTypeAvro, EPStatementInitServices.REF)));
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(GenericData.Record.class, "record", newInstance(GenericData.Record.class, schema));
        for (int i = 0; i < selectExprForgeContext.getColumnNames().length; i++) {
            CodegenExpression expression = forges[i].evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
            block.expression(exprDotMethod(ref("record"), "put", constant(selectExprForgeContext.getColumnNames()[i]), expression));
        }
        block.methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedAvro", ref("record"), resultEventType));
        return methodNode;
    }

    private ExprForge handleFragment(ExprEvalByGetterFragment eval) {
        if (eval.getEvaluationType() == GenericData.Record[].class) {
            return new SelectExprProcessorEvalByGetterFragmentAvroArray(eval.getStreamNum(), eval.getGetter(), Collection.class);
        }
        if (eval.getEvaluationType() == GenericData.Record.class) {
            return new SelectExprProcessorEvalByGetterFragmentAvro(eval.getStreamNum(), eval.getGetter(), GenericData.Record.class);
        }
        throw new EPException("Unrecognized return type " + eval.getEvaluationType() + " for use with Avro");
    }
}