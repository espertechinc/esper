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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;

import java.util.List;

public abstract class SelectEvalStreamBase implements SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final List<SelectClauseStreamCompiledSpec> namedStreams;
    protected final boolean isUsingWildcard;
    protected ExprEvaluator[] evaluators;

    public SelectEvalStreamBase(SelectExprForgeContext context, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.namedStreams = namedStreams;
        this.isUsingWildcard = usingWildcard;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprForgeContext getContext() {
        return context;
    }
}