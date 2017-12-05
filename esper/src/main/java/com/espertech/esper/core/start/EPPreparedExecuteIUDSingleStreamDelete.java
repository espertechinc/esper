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

import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPPreparedExecuteIUDSingleStreamDelete extends EPPreparedExecuteIUDSingleStream {
    public EPPreparedExecuteIUDSingleStreamDelete(StatementSpecCompiled statementSpec, EPServicesContext services, StatementContext statementContext) throws ExprValidationException {
        super(statementSpec, services, statementContext);
    }

    public EPPreparedExecuteIUDSingleStreamExec getExecutor(QueryGraph queryGraph, String aliasName) {
        return new EPPreparedExecuteIUDSingleStreamExecDelete(queryGraph, statementSpec.getFilterRootNode(), statementSpec.getAnnotations(), statementSpec.getTableNodes(), services);
    }
}
