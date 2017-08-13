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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.util.TypeWidener;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

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
            evaluators = ExprNodeUtility.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
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

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext codegenContext) {
        return processCodegen(memberResultEventType, memberEventAdapterService, params, codegenContext, context.getExprForges(), resultEventType.getPropertyNames(), remapped, wideners);
    }

    public static CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context, ExprForge[] forges, String[] propertyNames, int[] remapped, TypeWidener[] optionalWideners) {
        CodegenBlock block = context.addMethod(EventBean.class, EvalInsertNoWildcardObjectArrayRemap.class).add(params).begin()
                .declareVar(Object[].class, "result", newArray(Object.class, constant(propertyNames.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenExpression value = forges[i].evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context);
            if (optionalWideners != null && optionalWideners[i] != null) {
                value = optionalWideners[i].widenCodegen(value, context);
            }
            block.assignArrayElement(ref("result"), constant(remapped[i]), value);
        }
        CodegenMethodId method = block.methodReturn(exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", ref("result"), CodegenExpressionBuilder.member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).passAll(params).call();
    }
}
