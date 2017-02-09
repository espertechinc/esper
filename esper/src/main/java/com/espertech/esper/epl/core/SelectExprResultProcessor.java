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
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.NaturalEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

/**
 * A select expression processor that check what type of result (synthetic and natural) event is expected and
 * produces.
 */
public class SelectExprResultProcessor implements SelectExprProcessor {
    private final StatementResultService statementResultService;
    private final SelectExprProcessor syntheticProcessor;
    private final BindProcessor bindProcessor;

    /**
     * Ctor.
     *
     * @param statementResultService for awareness of listeners and subscribers handles output results
     * @param syntheticProcessor     is the processor generating synthetic events according to the select clause
     * @param bindProcessor          for generating natural object column results
     */
    public SelectExprResultProcessor(StatementResultService statementResultService,
                                     SelectExprProcessor syntheticProcessor,
                                     BindProcessor bindProcessor) {
        this.statementResultService = statementResultService;
        this.syntheticProcessor = syntheticProcessor;
        this.bindProcessor = bindProcessor;
    }

    public EventType getResultEventType() {
        return syntheticProcessor.getResultEventType();
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qSelectClause(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        }
        if (isSynthesize && (!statementResultService.isMakeNatural())) {
            if (InstrumentationHelper.ENABLED) {
                EventBean result = syntheticProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
                InstrumentationHelper.get().aSelectClause(isNewData, result, null);
                return result;
            }
            return syntheticProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
        }

        EventBean syntheticEvent = null;
        EventType syntheticEventType = null;
        if (statementResultService.isMakeSynthetic() || isSynthesize) {
            syntheticEvent = syntheticProcessor.process(eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);

            if (!statementResultService.isMakeNatural()) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aSelectClause(isNewData, syntheticEvent, null);
                }
                return syntheticEvent;
            }

            syntheticEventType = syntheticProcessor.getResultEventType();
        }

        if (!statementResultService.isMakeNatural()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aSelectClause(isNewData, null, null);
            }
            return null; // neither synthetic nor natural required, be cheap and generate no output event
        }

        Object[] parameters = bindProcessor.process(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aSelectClause(isNewData, null, parameters);
        }
        return new NaturalEventBean(syntheticEventType, parameters, syntheticEvent);
    }
}
