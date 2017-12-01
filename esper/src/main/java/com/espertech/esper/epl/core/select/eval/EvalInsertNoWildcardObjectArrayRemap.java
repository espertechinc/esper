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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;

public class EvalInsertNoWildcardObjectArrayRemap implements SelectExprProcessor, SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final int[] remapped;
    protected ExprEvaluator[] evaluators;

    public EvalInsertNoWildcardObjectArrayRemap(SelectExprForgeContext context, EventType resultEventType, int[] remapped) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.remapped = remapped;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] result = new Object[resultEventType.getPropertyNames().length];
        for (int i = 0; i < evaluators.length; i++) {
            result[remapped[i]] = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return context.getEventAdapterService().adapterForTypedObjectArray(result, resultEventType);
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return EvalInsertNoWildcardObjectArrayRemapWWiden.processCodegen(memberResultEventType, memberEventAdapterService, codegenMethodScope, exprSymbol, codegenClassScope, context.getExprForges(), resultEventType.getPropertyNames(), remapped, null);
    }
}
