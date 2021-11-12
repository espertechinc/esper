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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorDB;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorDBExecUnprepared;

import java.util.Collection;

public class FAFQueryMethodSelectExecDBUnprepared extends FAFQueryMethodSelectExecDBBase {

    public FAFQueryMethodSelectExecDBUnprepared(StatementContextRuntimeServices services) {
        super(services);
    }

    protected Collection<EventBean> executeInternal(ExprEvaluatorContext exprEvaluatorContext, FAFQueryMethodSelect select) {
        FireAndForgetProcessorDB db = (FireAndForgetProcessorDB) select.getProcessors()[0];
        FireAndForgetProcessorDBExecUnprepared unprepared = db.unprepared(exprEvaluatorContext, services);
        return unprepared.performQuery(exprEvaluatorContext);
    }
}
