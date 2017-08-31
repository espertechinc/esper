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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.NaturalEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A select expression processor that check what type of result (synthetic and natural) event is expected and
 * produces.
 */
public class SelectExprResultProcessor implements SelectExprProcessor, SelectExprProcessorForge {
    private final StatementResultService statementResultService;
    private final SelectExprProcessorForge syntheticProcessorForge;
    private final BindProcessorForge bindProcessorForge;

    private SelectExprProcessor syntheticProcessor;
    private BindProcessor bindProcessor;

    /**
     * Ctor.
     *
     * @param statementResultService for awareness of listeners and subscribers handles output results
     * @param syntheticProcessor     is the processor generating synthetic events according to the select clause
     * @param bindProcessorForge          for generating natural object column results
     */
    public SelectExprResultProcessor(StatementResultService statementResultService,
                                     SelectExprProcessorForge syntheticProcessor,
                                     BindProcessorForge bindProcessorForge) {
        this.statementResultService = statementResultService;
        this.syntheticProcessorForge = syntheticProcessor;
        this.bindProcessorForge = bindProcessorForge;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (syntheticProcessor == null) {
            syntheticProcessor = syntheticProcessorForge.getSelectExprProcessor(engineImportService, isFireAndForget, statementName);
            bindProcessor = bindProcessorForge.getBindProcessor(engineImportService, isFireAndForget, statementName);
        }
        return this;
    }

    public EventType getResultEventType() {
        return syntheticProcessorForge.getResultEventType();
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qSelectClause(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        }

        boolean makeNatural = statementResultService.isMakeNatural();
        boolean synthesize = statementResultService.isMakeSynthetic() || isSynthesize;

        if (!makeNatural) {
            if (synthesize) {
                EventBean syntheticEvent = syntheticProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aSelectClause(isNewData, syntheticEvent, null);
                }
                return syntheticEvent;
            }
            return null;
        }

        EventBean syntheticEvent = null;
        EventType syntheticEventType = null;

        if (synthesize) {
            syntheticEvent = syntheticProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
            syntheticEventType = syntheticProcessorForge.getResultEventType();
        }

        Object[] parameters = bindProcessor.process(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aSelectClause(isNewData, syntheticEvent, parameters);
        }
        return new NaturalEventBean(syntheticEventType, parameters, syntheticEvent);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode processMethod = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenExpressionRef isSythesize = selectSymbol.getAddSynthesize(processMethod);

        CodegenMethodNode syntheticMethod = syntheticProcessorForge.processCodegen(memberResultEventType, memberEventAdapterService, processMethod, selectSymbol, exprSymbol, codegenClassScope);
        CodegenMethodNode bindMethod = bindProcessorForge.processCodegen(processMethod, exprSymbol, codegenClassScope);

        CodegenMember stmtResultSvc = codegenClassScope.makeAddMember(StatementResultService.class, statementResultService);
        processMethod.getBlock()
                .declareVar(boolean.class, "makeNatural", exprDotMethod(member(stmtResultSvc.getMemberId()), "isMakeNatural"))
                .declareVar(boolean.class, "synthesize", or(exprDotMethod(member(stmtResultSvc.getMemberId()), "isMakeSynthetic"), isSythesize))
                .ifCondition(not(ref("makeNatural")))
                .ifCondition(ref("synthesize"))
                .blockReturn(localMethod(syntheticMethod))
                .blockReturn(constantNull())
                .declareVar(EventBean.class, "syntheticEvent", constantNull())
                .ifCondition(ref("synthesize"))
                .assignRef("syntheticEvent", localMethod(syntheticMethod))
                .blockEnd()
                .declareVar(Object[].class, "parameters", localMethod(bindMethod))
                .methodReturn(newInstance(NaturalEventBean.class, member(memberResultEventType.getMemberId()), ref("parameters"), ref("syntheticEvent")));

        return processMethod;
    }
}
