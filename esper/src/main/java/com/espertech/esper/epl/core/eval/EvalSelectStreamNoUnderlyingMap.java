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
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EvalSelectStreamNoUnderlyingMap extends EvalSelectStreamBaseMap implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalSelectStreamNoUnderlyingMap.class);

    public EvalSelectStreamNoUnderlyingMap(SelectExprContext selectExprContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard) {
        super(selectExprContext, resultEventType, namedStreams, usingWildcard);
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return super.getSelectExprContext().getEventAdapterService().adapterForTypedMap(props, super.getResultEventType());
    }
}
