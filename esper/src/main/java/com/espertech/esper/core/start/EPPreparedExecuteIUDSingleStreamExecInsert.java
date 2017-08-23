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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;

public class EPPreparedExecuteIUDSingleStreamExecInsert implements EPPreparedExecuteIUDSingleStreamExec {
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final SelectExprProcessor insertHelper;
    private final ExprTableAccessNode[] optionalTableNodes;
    private final EPServicesContext services;

    public EPPreparedExecuteIUDSingleStreamExecInsert(ExprEvaluatorContext exprEvaluatorContext, SelectExprProcessor insertHelper, ExprTableAccessNode[] optionalTableNodes, EPServicesContext services) {
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.insertHelper = insertHelper;
        this.optionalTableNodes = optionalTableNodes;
        this.services = services;
    }

    public EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance) {
        return fireAndForgetProcessorInstance.processInsert(this);
    }

    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }

    public SelectExprProcessor getInsertHelper() {
        return insertHelper;
    }

    public ExprTableAccessNode[] getOptionalTableNodes() {
        return optionalTableNodes;
    }

    public EPServicesContext getServices() {
        return services;
    }
}
