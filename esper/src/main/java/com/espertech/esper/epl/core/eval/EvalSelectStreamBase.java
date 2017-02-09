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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;

import java.util.List;

public abstract class EvalSelectStreamBase implements SelectExprProcessor {

    protected final SelectExprContext selectExprContext;
    protected final EventType resultEventType;
    protected final List<SelectClauseStreamCompiledSpec> namedStreams;
    protected final boolean isUsingWildcard;

    public EvalSelectStreamBase(SelectExprContext selectExprContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        this.selectExprContext = selectExprContext;
        this.resultEventType = resultEventType;
        this.namedStreams = namedStreams;
        this.isUsingWildcard = usingWildcard;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprContext getSelectExprContext() {
        return selectExprContext;
    }
}