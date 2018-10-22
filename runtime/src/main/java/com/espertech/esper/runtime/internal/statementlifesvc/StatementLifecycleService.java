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
package com.espertech.esper.runtime.internal.statementlifesvc;

import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementContextResolver;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

public interface StatementLifecycleService extends StatementContextResolver {
    void addStatement(EPStatementSPI stmt);

    StatementContext getStatementContextById(int statementId);

    EPStatementSPI getStatementById(int statementId);

    void removeStatement(int statementId);
}
