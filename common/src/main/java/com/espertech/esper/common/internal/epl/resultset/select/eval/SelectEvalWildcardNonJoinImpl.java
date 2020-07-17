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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;

public class SelectEvalWildcardNonJoinImpl implements SelectExprProcessor {
    public final static EPTypeClass EPTYPE = new EPTypeClass(SelectEvalWildcardNonJoinImpl.class);
    private final StatementResultService statementResultService;

    public SelectEvalWildcardNonJoinImpl(StatementResultService statementResultService) {
        this.statementResultService = statementResultService;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvalCtx) {
        if (isSynthesize || statementResultService.isMakeSynthetic()) {
            return eventsPerStream[0];
        }
        return null;
    }
}
