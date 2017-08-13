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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.core.service.StatementResultService;
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

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenMember stmtResultSvc = context.makeAddMember(StatementResultService.class, statementResultService);
        CodegenMethodId method = context.addMethod(EventBean.class, this.getClass()).add(params).begin()
                .declareVar(boolean.class, "makeNatural", exprDotMethod(member(stmtResultSvc.getMemberId()), "isMakeNatural"))
                .declareVar(boolean.class, "synthesize", or(exprDotMethod(member(stmtResultSvc.getMemberId()), "isMakeSynthetic"), params.passIsSynthesize()))
                .ifCondition(not(ref("makeNatural")))
                    .ifCondition(ref("synthesize"))
                        .blockReturn(syntheticProcessorForge.processCodegen(memberResultEventType, memberEventAdapterService, params, context))
                    .blockReturn(constantNull())
                .declareVar(EventBean.class, "syntheticEvent", constantNull())
                .ifCondition(ref("synthesize"))
                        .assignRef("syntheticEvent", syntheticProcessorForge.processCodegen(memberResultEventType, memberEventAdapterService, params, context))
                        .blockEnd()
                .declareVar(Object[].class, "parameters", bindProcessorForge.processCodegen(params, context))
                .methodReturn(newInstance(NaturalEventBean.class, member(memberResultEventType.getMemberId()), ref("parameters"), ref("syntheticEvent")));
        return localMethodBuild(method).passAll(params).call();
    }
}
