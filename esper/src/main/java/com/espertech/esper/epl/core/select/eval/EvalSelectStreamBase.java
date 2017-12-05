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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;

import java.util.List;

public abstract class EvalSelectStreamBase implements SelectExprProcessor, SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final List<SelectClauseStreamCompiledSpec> namedStreams;
    protected final boolean isUsingWildcard;
    protected ExprEvaluator[] evaluators;

    public EvalSelectStreamBase(SelectExprForgeContext context, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.namedStreams = namedStreams;
        this.isUsingWildcard = usingWildcard;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprForgeContext getContext() {
        return context;
    }
}