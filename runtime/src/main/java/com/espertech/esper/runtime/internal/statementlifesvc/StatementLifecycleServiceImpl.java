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
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.HashMap;
import java.util.Map;

public class StatementLifecycleServiceImpl implements StatementLifecycleService {
    private final Map<Integer, EPStatementSPI> statementsById = new HashMap<>();

    public void addStatement(EPStatementSPI stmt) {
        int statementId = stmt.getStatementId();
        if (statementsById.containsKey(statementId)) {
            throw new IllegalArgumentException("Statement id " + stmt.getStatementId() + " already assigned");
        }
        statementsById.put(statementId, stmt);
    }

    public StatementContext getStatementContextById(int statementId) {
        EPStatementSPI statement = statementsById.get(statementId);
        return statement == null ? null : statement.getStatementContext();
    }

    public EPStatementSPI getStatementById(int statementId) {
        return statementsById.get(statementId);
    }

    public void removeStatement(int statementId) {
        statementsById.remove(statementId);
    }
}
