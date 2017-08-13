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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNodeCompiler;
import com.espertech.esper.event.EventAdapterService;

public abstract class EvalBaseFirstProp implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprForgeContext selectExprForgeContext;
    private final EventType resultEventType;
    private ExprEvaluator firstEvaluator;

    public EvalBaseFirstProp(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        this.selectExprForgeContext = selectExprForgeContext;
        this.resultEventType = resultEventType;
    }

    protected abstract EventBean processFirstCol(Object result);

    protected abstract CodegenExpression processFirstColCodegen(Class evaluationType, CodegenExpression expression, CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenContext context);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Object first = firstEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        return processFirstCol(first);
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        ExprForge first = selectExprForgeContext.getExprForges()[0];
        return processFirstColCodegen(first.getEvaluationType(), first.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context), memberResultEventType, memberEventAdapterService, context);
    }

    public EventAdapterService getEventAdapterService() {
        return selectExprForgeContext.getEventAdapterService();
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (firstEvaluator == null) {
            firstEvaluator = ExprNodeCompiler.allocateEvaluator(selectExprForgeContext.getExprForges()[0], engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }
}
