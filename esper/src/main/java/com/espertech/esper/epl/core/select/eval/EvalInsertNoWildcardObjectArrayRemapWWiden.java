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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.util.TypeWidener;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertNoWildcardObjectArrayRemapWWiden implements SelectExprProcessor, SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final int[] remapped;
    protected final TypeWidener[] wideners;

    protected ExprEvaluator[] evaluators;

    public EvalInsertNoWildcardObjectArrayRemapWWiden(SelectExprForgeContext context, EventType resultEventType, int[] remapped, TypeWidener[] wideners) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.remapped = remapped;
        this.wideners = wideners;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {

        Object[] result = new Object[resultEventType.getPropertyNames().length];
        for (int i = 0; i < evaluators.length; i++) {
            Object value = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (wideners[i] != null) {
                value = wideners[i].widen(value);
            }
            result[remapped[i]] = value;
        }

        return context.getEventAdapterService().adapterForTypedObjectArray(result, resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return processCodegen(memberResultEventType, memberEventAdapterService, codegenMethodScope, exprSymbol, codegenClassScope, context.getExprForges(), resultEventType.getPropertyNames(), remapped, wideners);
    }

    public static CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, ExprForge[] forges, String[] propertyNames, int[] remapped, TypeWidener[] optionalWideners) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, EvalInsertNoWildcardObjectArrayRemap.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "result", newArrayByLength(Object.class, constant(propertyNames.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenExpression value;
            if (optionalWideners != null && optionalWideners[i] != null) {
                value = forges[i].evaluateCodegen(forges[i].getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
                value = optionalWideners[i].widenCodegen(value, codegenMethodScope, codegenClassScope);
            } else {
                value = forges[i].evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope);
            }
            block.assignArrayElement(ref("result"), constant(remapped[i]), value);
        }
        block.methodReturn(exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", ref("result"), CodegenExpressionBuilder.member(memberResultEventType.getMemberId())));
        return methodNode;
    }
}
