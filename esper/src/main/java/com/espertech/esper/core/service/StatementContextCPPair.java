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
package com.espertech.esper.core.service;

public class StatementContextCPPair {
    private final int statementId;
    private final int agentInstanceId;
    private final StatementContext optionalStatementContext;

    public StatementContextCPPair(int statementId, int agentInstanceId, StatementContext optionalStatementContext) {
        this.statementId = statementId;
        this.agentInstanceId = agentInstanceId;
        this.optionalStatementContext = optionalStatementContext;
    }

    public int getStatementId() {
        return statementId;
    }

    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    public StatementContext getOptionalStatementContext() {
        return optionalStatementContext;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatementContextCPPair)) return false;

        StatementContextCPPair that = (StatementContextCPPair) o;

        if (agentInstanceId != that.agentInstanceId) return false;
        return statementId == that.statementId;

    }

    public int hashCode() {
        int result = statementId;
        result = 31 * result + agentInstanceId;
        return result;
    }
}
