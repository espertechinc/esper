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
import com.espertech.esper.codegen.model.blocks.CodegenLegoMayVoid;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertNoWildcardObjectArray extends EvalBase implements SelectExprProcessor, SelectExprProcessorForge {

    private ExprEvaluator[] evaluators;

    public EvalInsertNoWildcardObjectArray(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        super(selectExprForgeContext, resultEventType);
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] result = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            result[i] = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return super.getEventAdapterService().adapterForTypedObjectArray(result, super.getResultEventType());
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtility.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(EventBean.class, this.getClass()).add(params).begin()
                .declareVar(Object[].class, "result", newArray(Object.class, constant(this.context.getExprForges().length)));
        for (int i = 0; i < this.context.getExprForges().length; i++) {
            CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(this.context.getExprForges()[i], CodegenParamSetExprPremade.INSTANCE, context);
            block.assignArrayElement("result", constant(i), expression);
        }
        CodegenMethodId method = block.methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", ref("result"), member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).passAll(params).call();
    }
}
